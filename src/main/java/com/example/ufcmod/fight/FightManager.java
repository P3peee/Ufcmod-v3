package com.example.ufcmod.fight;

import com.example.ufcmod.ModItems;
import com.example.ufcmod.net.AnimPacket;
import com.example.ufcmod.net.Net;
import com.example.ufcmod.net.SyncPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class FightManager {
    private static final Map<UUID, FighterState> STATES = new HashMap<>();
    private static final UUID STANCE_MOD = UUID.fromString("5b1c0f6e-3a5d-4c1e-9b0a-1f2d3c4b5a61");
    private static final UUID BLOCK_MOD = UUID.fromString("5b1c0f6e-3a5d-4c1e-9b0a-1f2d3c4b5a62");

    public static boolean APPLYING = false;
    public static final int SUB_TIME = 70;
    public static final int TAKEDOWN_TIME = 120;

    // ------------------------------------------------------------ state access
    public static FighterState get(LivingEntity e) {
        return STATES.computeIfAbsent(e.getUUID(), id -> new FighterState(e));
    }

    public static FighterState peek(LivingEntity e) {
        return STATES.get(e.getUUID());
    }

    public static void remove(LivingEntity e) {
        FighterState s = STATES.remove(e.getUUID());
        if (s != null) {
            speedMod(e, STANCE_MOD, 0, false);
            speedMod(e, BLOCK_MOD, 0, false);
        }
    }

    // ------------------------------------------------------------ animation sync
    public static int flagsOf(FighterState s) {
        int f = 0;
        if (s.stance) f |= AnimFlags.STANCE;
        if (s.isBlocking()) f |= AnimFlags.BLOCK;
        if (s.grounded > 0) f |= AnimFlags.GROUNDED;
        if (s.stunned > 0) f |= AnimFlags.STUNNED;
        if (s.submitTicks > 0) f |= AnimFlags.SUBMIT;
        return f;
    }

    private static void sendFlags(LivingEntity e, int flags) {
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e),
                new AnimPacket(e.getId(), flags, -1, 0, 0));
    }

    public static void sendFlagsTo(ServerPlayer viewer, LivingEntity e, FighterState s) {
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                new AnimPacket(e.getId(), flagsOf(s), -1, 0, 0));
    }

    private static void sendAnim(LivingEntity e, AnimType type, int param, int duration) {
        FighterState s = peek(e);
        int flags = s == null ? 0 : flagsOf(s);
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e),
                new AnimPacket(e.getId(), flags, type.ordinal(), param, duration));
    }

    // ------------------------------------------------------------ input from client
    public static void handleAction(ServerPlayer p, Action a, int extra) {
        if (!p.isAlive() || p.isSpectator()) return;
        FighterState s = get(p);
        switch (a) {
            case TOGGLE_STANCE -> {
                if (s.stunned > 0 || s.grounded > 0) return;
                s.stance = !s.stance;
                if (!s.stance) {
                    s.blockHeld = false;
                    s.subHeld = false;
                    s.pending = null;
                }
                p.displayClientMessage(Component.translatable(
                        s.stance ? "msg.ufcmod.stance_on" : "msg.ufcmod.stance_off"), true);
            }
            case JAB -> startMove(p, s, Move.JAB);
            case CROSS -> startMove(p, s, Move.CROSS);
            case LEG_KICK -> startMove(p, s, Move.LEG_KICK);
            case HEAD_KICK -> startMove(p, s, Move.HEAD_KICK);
            case TAKEDOWN -> startMove(p, s, Move.TAKEDOWN);
            case DODGE -> dodge(p, s, extra);
            case BLOCK_START -> s.blockHeld = true;
            case BLOCK_STOP -> s.blockHeld = false;
            case SUB_START -> s.subHeld = true;
            case SUB_STOP -> s.subHeld = false;
        }
    }

    private static boolean canAct(FighterState s) {
        return s.stance && s.stunned == 0 && s.grounded == 0 && s.cooldown == 0 && s.pending == null;
    }

    // ------------------------------------------------------------ moves
    private static void startMove(ServerPlayer p, FighterState s, Move m) {
        if (!canAct(s)) return;
        FighterProfile pr = Profiles.get(p);
        float cost = m.cost * pr.staminaCostMul();
        if (s.stamina < cost * 0.5f) {
            p.displayClientMessage(Component.translatable("msg.ufcmod.gassed"), true);
            return;
        }
        s.stamina = Math.max(0f, s.stamina - cost);
        s.staminaDelay = 30;

        int startup = Math.max(1, Math.round(m.startup * pr.timeMul()));
        int recovery = Math.max(2, Math.round(m.recovery * pr.timeMul()));
        s.pending = m;
        s.pendingTicks = startup;
        s.cooldown = startup + recovery;

        if (m == Move.JAB) p.swing(InteractionHand.OFF_HAND, true);        // lead hand
        else if (m == Move.CROSS) p.swing(InteractionHand.MAIN_HAND, true); // rear hand
        else p.swing(InteractionHand.MAIN_HAND, true);

        sendAnim(p, AnimType.valueOf(m.name()), startup, startup + recovery);
    }

    private static void resolveMove(ServerPlayer p, FighterState st) {
        Move m = st.pending;
        st.pending = null;
        if (m == null) return;

        ServerLevel level = p.serverLevel();
        FighterProfile ap = Profiles.get(p);
        boolean kick = m == Move.LEG_KICK || m == Move.HEAD_KICK;
        double range = m.range * (kick ? ap.scale() : ap.reachFactor());

        LivingEntity target = findTarget(p, range, e -> true);

        if (target == null) { // whiff
            st.stamina = Math.max(0f, st.stamina - 3f);
            st.cooldown += 4;
            st.comboCount = 0;
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
                    SoundSource.PLAYERS, 0.7f, 1.0f);
            return;
        }

        if (m == Move.TAKEDOWN) {
            resolveTakedown(p, st, target, ap);
            return;
        }

        FighterState ts = get(target);
        FighterProfile tp = Profiles.get(target);

        if (ts.iframes > 0) {
            p.displayClientMessage(Component.literal("Dodged!"), true);
            st.comboCount = 0;
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
                    SoundSource.PLAYERS, 0.8f, 1.3f);
            return;
        }

        boolean counter = ts.pending != null;
        boolean down = ts.grounded > 0;

        float dmg = m.damage * ap.damageMul() * (st.stamina < 20f ? 0.75f : 1f);
        if (holdingGloves(p)) dmg *= 1.2f;
        if (down) dmg *= 1.5f;
        if (counter) dmg *= 1.35f;
        dmg *= 1f + 0.06f * Math.min(st.comboCount, 5);

        float rock = m.rock * tp.chin();
        if (counter) rock *= 1.5f;
        if (ts.stamina < 25f) rock *= 1.4f;
        if (down) rock *= 0.5f;

        boolean blocked = false;
        if (ts.isBlocking() && isFacing(target, p)) {
            blocked = true;
            float reduce = m.low ? 0.25f : 0.8f;
            dmg *= 1f - reduce;
            rock *= 0.1f;
            ts.stamina -= m.damage * (m.low ? 1.5f : 3f);
            level.playSound(null, target.blockPosition(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 0.8f, 1.2f);
            if (ts.stamina <= 0f) {
                ts.stamina = 0f;
                ts.stunned = 30;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
                broadcast(p, "GUARD BROKEN!");
            }
        }

        if (target instanceof Player) {
            dmg = Math.min(dmg, Math.max(0f, target.getHealth() - 1f));
        }

        DamageSource src = p.damageSources().playerAttack(p);
        APPLYING = true;
        try {
            target.invulnerableTime = 0;
            target.hurt(src, dmg);
        } finally {
            APPLYING = false;
        }

        if (!blocked && m.knockback > 0f) {
            float ratio = Mth.clamp(ap.mass() / tp.mass(), 0.5f, 2f);
            float yaw = p.getYRot() * Mth.DEG_TO_RAD;
            target.knockback(m.knockback * ratio, Mth.sin(yaw), -Mth.cos(yaw));
            target.hurtMarked = true;
        }
        if (m.low && !blocked) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
        }
        if (!blocked) sendAnim(target, AnimType.HIT, 0, 6);

        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.7), target.getZ(),
                blocked ? 3 : 10, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, target.blockPosition(),
                m == Move.HEAD_KICK ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.0f, blocked ? 0.7f : 1.0f);

        if (!blocked) {
            st.comboCount++;
            st.comboTimer = 40;
        } else {
            st.comboCount = 0;
        }

        ts.rock += rock;
        if (ts.rock >= 100f && ts.stunned == 0) {
            ts.rock = 40f;
            ts.stunned = 50;
            ts.pending = null;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            broadcast(p, "ROCKED!");
        }

        if (target instanceof Player && target.getHealth() <= 2f && ts.stunned == 0) {
            knockOut(target, 200);
            broadcast(p, p.getName().getString() + " wins by KO!");
        }
    }

    private static void resolveTakedown(ServerPlayer p, FighterState st, LivingEntity target, FighterProfile ap) {
        FighterState ts = get(target);
        FighterProfile tp = Profiles.get(target);
        ServerLevel level = p.serverLevel();
        if (ts.grounded > 0) return;

        double chance = target instanceof Player ? 0.60 : 0.85;
        if (ts.iframes > 0) chance = 0.0;
        else {
            if (ts.isBlocking()) chance -= 0.30;
            if (ts.stamina < 25f) chance += 0.20;
            if (st.stamina < 15f) chance -= 0.20;
            chance += Mth.clamp((ap.mass() - tp.mass()) * 0.35f, -0.25f, 0.25f); // weight advantage
        }

        if (p.getRandom().nextDouble() < chance) {
            ground(target, TAKEDOWN_TIME);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                    SoundSource.PLAYERS, 1.0f, 0.7f);
            broadcast(p, "TAKEDOWN!");
        } else {
            st.cooldown += 12;
            p.displayClientMessage(Component.literal("Takedown stuffed!"), true);
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
                    SoundSource.PLAYERS, 0.8f, 0.8f);
        }
    }

    // ------------------------------------------------------------ dodge / ground
    private static void dodge(ServerPlayer p, FighterState s, int dir) {
        if (s.grounded > 0 && s.stunned == 0) {
            if (s.stamina >= 8f) {
                s.stamina -= 8f;
                s.staminaDelay = 30;
                s.grounded -= 25;
                p.displayClientMessage(Component.literal("Struggling!"), true);
                if (s.grounded <= 0) endGround(p, s);
            }
            return;
        }
        if (!canAct(s) || s.stamina < 12f) return;

        s.stamina -= 12f;
        s.staminaDelay = 30;
        s.iframes = 8;
        s.cooldown = 14;

        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
        double vx, vz;
        if (dir == 1)      { vx = -rx; vz = -rz; }
        else if (dir == 2) { vx = rx;  vz = rz;  }
        else if (dir == 3) { vx = fx;  vz = fz;  }
        else               { vx = -fx; vz = -fz; }

        p.setDeltaMovement(vx * 0.85, 0.12, vz * 0.85);
        p.hurtMarked = true;
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(),
                6, 0.2, 0.05, 0.2, 0.02);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.6f, 1.4f);
        sendAnim(p, AnimType.DODGE, dir, 10);
    }

    public static void ground(LivingEntity target, int ticks) {
        FighterState ts = get(target);
        ts.grounded = ticks;
        ts.pending = null;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, false, false));
    }

    private static void endGround(LivingEntity e, FighterState s) {
        s.grounded = 0;
        e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    public static void knockOut(LivingEntity t, int ticks) {
        FighterState ts = get(t);
        ts.stunned = ticks;
        ts.grounded = Math.max(ts.grounded, ticks);
        ts.rock = 0f;
        ts.pending = null;
        ts.subHeld = false;
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, false, false));
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(ticks, 100), 0, false, false, false));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, false, false));
        if (t.level() instanceof ServerLevel sl) {
            sl.playSound(null, t.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 0.8f);
            sl.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY(1.0), t.getZ(), 25, 0.4, 0.4, 0.4, 0.2);
        }
    }

    // ------------------------------------------------------------ submissions
    private static void tickSubmission(ServerPlayer sp, FighterState s) {
        if (!(s.subHeld && s.stance && s.stunned == 0 && s.grounded == 0)) {
            s.submitTicks = 0;
            return;
        }
        double range = 2.8 * Profiles.get(sp).scale();
        LivingEntity t = findTarget(sp, range, e -> {
            FighterState es = peek(e);
            return es != null && es.grounded > 0;
        });
        if (t == null) {
            s.submitTicks = 0;
            return;
        }
        s.submitTicks++;
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 5, false, false, false));
        if (s.submitTicks % 10 == 0) {
            sp.serverLevel().playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                    SoundSource.PLAYERS, 0.5f, 0.6f);
        }
        if (s.submitTicks >= SUB_TIME) {
            s.submitTicks = 0;
            s.subHeld = false;
            broadcast(sp, sp.getName().getString() + " wins by SUBMISSION!");
            if (t instanceof Player) {
                knockOut(t, 160);
            } else {
                t.hurt(sp.damageSources().playerAttack(sp), 1000f);
            }
        }
    }

    // ------------------------------------------------------------ server tick
    public static void tickAll() {
        for (FighterState s : new ArrayList<>(STATES.values())) {
            LivingEntity e = s.entity;
            if (e.isRemoved() || !e.isAlive()) {
                STATES.remove(e.getUUID());
                continue;
            }
            tick(s);
        }
    }

    private static void tick(FighterState s) {
        LivingEntity e = s.entity;
        ServerPlayer sp = e instanceof ServerPlayer x ? x : null;
        FighterProfile pr = Profiles.get(e);

        if (s.pending != null && --s.pendingTicks <= 0) {
            if (sp != null) resolveMove(sp, s);
            else s.pending = null;
        }
        if (s.cooldown > 0) s.cooldown--;
        if (s.iframes > 0) s.iframes--;
        if (s.comboTimer > 0 && --s.comboTimer == 0) s.comboCount = 0;

        if (s.stunned > 0) {
            s.stunned--;
            s.pending = null;
        }
        if (s.grounded > 0) {
            s.grounded--;
            s.pending = null;
            if (s.grounded == 0) endGround(e, s);
        }

        if (s.staminaDelay > 0) s.staminaDelay--;
        else s.stamina = Math.min(100f, s.stamina + (s.isBlocking() ? 0.15f : 0.45f) * pr.regenMul());
        if (s.rock > 0f) s.rock = Math.max(0f, s.rock - 0.25f);

        if (sp != null) {
            tickSubmission(sp, s);
            speedMod(e, STANCE_MOD, -0.12, s.stance);
            speedMod(e, BLOCK_MOD, -0.40, s.isBlocking());
        }

        // animation flags for everybody who can see this entity
        int f = flagsOf(s);
        if (f != s.lastFlags) {
            s.lastFlags = f;
            sendFlags(e, f);
        }

        if (sp != null) {
            boolean active = s.stance || s.stamina < 99.5f || s.rock > 0.5f
                    || s.stunned > 0 || s.grounded > 0 || s.comboCount > 0;
            if ((active && e.tickCount % 2 == 0) || (!active && s.wasActive)) {
                Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncPacket(
                        s.stamina, s.rock, s.stance, s.isBlocking(),
                        s.stunned, s.grounded, s.comboCount,
                        s.submitTicks * 100 / SUB_TIME));
            }
            s.wasActive = active;
        } else if (!s.stance && s.rock <= 0f && s.stunned == 0 && s.grounded == 0
                && s.iframes == 0 && s.pending == null) {
            if (s.lastFlags != 0) sendFlags(e, 0);
            STATES.remove(e.getUUID());
        }
    }

    // ------------------------------------------------------------ helpers
    public static LivingEntity findTarget(LivingEntity a, double range, Predicate<LivingEntity> filter) {
        Vec3 look = a.getLookAngle();
        double len = Math.sqrt(look.x * look.x + look.z * look.z);
        if (len < 1.0E-4) return null;
        double fx = look.x / len, fz = look.z / len;

        AABB box = a.getBoundingBox().inflate(range + 1.0, 1.5, range + 1.0);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : a.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == a || !e.isAlive() || e.isSpectator() || !filter.test(e)) continue;
            double dx = e.getX() - a.getX(), dz = e.getZ() - a.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d - e.getBbWidth() / 2.0 > range) continue;
            if (Math.abs(e.getY() - a.getY()) > 2.0) continue;
            double dot = d < 1.0E-4 ? 1.0 : (dx * fx + dz * fz) / d;
            if (dot < 0.55) continue;
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    public static boolean isFacing(LivingEntity victim, Entity other) {
        Vec3 look = victim.getLookAngle();
        double tx = other.getX() - victim.getX(), tz = other.getZ() - victim.getZ();
        return look.x * tx + look.z * tz > 0;
    }

    private static boolean holdingGloves(Player p) {
        return p.getMainHandItem().is(ModItems.MMA_GLOVES.get())
                || p.getOffhandItem().is(ModItems.MMA_GLOVES.get());
    }

    private static void broadcast(LivingEntity src, String text) {
        if (!(src.level() instanceof ServerLevel sl)) return;
        Component c = Component.literal(text);
        for (ServerPlayer pl : sl.players()) {
            if (pl.distanceToSqr(src) < 1600) pl.sendSystemMessage(c);
        }
    }

    private static void speedMod(LivingEntity e, UUID id, double amount, boolean on) {
        AttributeInstance inst = e.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) return;
        AttributeModifier m = inst.getModifier(id);
        if (on && m == null) {
            inst.addTransientModifier(new AttributeModifier(id, "ufcmod_speed",
                    amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!on && m != null) {
            inst.removeModifier(id);
        }
    }
}
