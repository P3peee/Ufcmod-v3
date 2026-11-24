package com.example.ufcmod.fight;

import com.example.ufcmod.UfcMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UfcMod.MODID)
public class FightEvents {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) FightManager.tickAll();
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e) {
        FighterState s = FightManager.peek(e.getEntity());
        if (s != null && s.iframes > 0 && !FightManager.APPLYING) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (FightManager.APPLYING) return;
        LivingEntity victim = e.getEntity();
        FighterState s = FightManager.peek(victim);
        if (s == null || !s.isBlocking()) return;
        if (e.getSource().getEntity() != null && FightManager.isFacing(victim, e.getSource().getEntity())) {
            e.setAmount(e.getAmount() * 0.5f);
            s.stamina = Math.max(0f, s.stamina - e.getAmount() * 2f);
        }
    }

    @SubscribeEvent
    public static void onVanillaAttack(AttackEntityEvent e) {
        FighterState s = FightManager.peek(e.getEntity());
        if (s != null && (s.stance || s.stunned > 0 || s.grounded > 0)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent e) {
        FighterState s = FightManager.peek(e.getEntity());
        if (s != null && (s.grounded > 0 || s.stunned > 0)) {
            Vec3 m = e.getEntity().getDeltaMovement();
            e.getEntity().setDeltaMovement(m.x, 0, m.z);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        FightManager.remove(e.getEntity());
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getEntity() instanceof LivingEntity le && !e.getLevel().isClientSide) FightManager.remove(le);
    }

    // ---------------------------------------------------------- body profile
    /** height -> hitbox + eye height (runs on both client and server) */
    @SubscribeEvent
    public static void onSize(EntityEvent.Size e) {
        if (e.getEntity() instanceof Player p) {
            float s = Profiles.get(p).scale();
            if (Math.abs(s - 1f) > 0.001f) {
                e.setNewSize(e.getNewSize().scale(s));
                e.setNewEyeHeight(e.getNewEyeHeight() * s);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) Profiles.apply(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) Profiles.apply(sp);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) Profiles.apply(sp);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        Profiles.copyPersisted(e.getOriginal(), e.getEntity());
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getEntity() instanceof ServerPlayer viewer)) return;
        Entity t = e.getTarget();
        if (t instanceof ServerPlayer tp) Profiles.sendTo(viewer, tp);
        if (t instanceof LivingEntity le) {
            FighterState s = FightManager.peek(le);
            if (s != null) FightManager.sendFlagsTo(viewer, le, s);
        }
    }
}
