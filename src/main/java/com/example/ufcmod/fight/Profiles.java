package com.example.ufcmod.fight;

import com.example.ufcmod.net.Net;
import com.example.ufcmod.net.ProfilePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server: profiles live in the player's persistent NBT. Client: cache filled by ProfilePacket. */
public class Profiles {
    public static final Map<UUID, FighterProfile> CLIENT = new HashMap<>();

    private static final UUID REACH_MOD = UUID.fromString("7c1d0a52-1111-4e0a-8a10-0a0b0c0d0e01");
    private static final UUID SPEED_MOD = UUID.fromString("7c1d0a52-1111-4e0a-8a10-0a0b0c0d0e02");
    private static final UUID KB_MOD    = UUID.fromString("7c1d0a52-1111-4e0a-8a10-0a0b0c0d0e03");

    public static FighterProfile get(LivingEntity e) {
        if (!(e instanceof Player p)) return FighterProfile.DEFAULT;
        if (p.level().isClientSide) {
            return CLIENT.getOrDefault(p.getUUID(), FighterProfile.DEFAULT);
        }
        CompoundTag tag = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!tag.contains("ufc_h")) return FighterProfile.DEFAULT;
        return new FighterProfile(tag.getInt("ufc_h"), tag.getInt("ufc_w"), tag.getInt("ufc_a"));
    }

    public static void set(ServerPlayer p, int h, int w, int a) {
        FighterProfile pr = new FighterProfile(h, w, a); // clamps
        CompoundTag root = p.getPersistentData();
        CompoundTag tag = root.getCompound(Player.PERSISTED_NBT_TAG);
        tag.putInt("ufc_h", pr.heightCm);
        tag.putInt("ufc_w", pr.weightKg);
        tag.putInt("ufc_a", pr.apePct);
        root.put(Player.PERSISTED_NBT_TAG, tag);
        apply(p);
    }

    /** attributes + hitbox + sync to everybody who sees this player */
    public static void apply(ServerPlayer p) {
        FighterProfile pr = get(p);
        attrMod(p, ForgeMod.ENTITY_REACH.get(), REACH_MOD, (pr.reachFactor() - 1f) * 3.0,
                AttributeModifier.Operation.ADDITION);
        attrMod(p, Attributes.MOVEMENT_SPEED, SPEED_MOD, pr.speedMul() - 1f,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        attrMod(p, Attributes.KNOCKBACK_RESISTANCE, KB_MOD, pr.kbResist(),
                AttributeModifier.Operation.ADDITION);
        p.refreshDimensions();
        Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p),
                new ProfilePacket(p.getUUID(), pr.heightCm, pr.weightKg, pr.apePct));
    }

    public static void sendTo(ServerPlayer viewer, ServerPlayer target) {
        FighterProfile pr = get(target);
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                new ProfilePacket(target.getUUID(), pr.heightCm, pr.weightKg, pr.apePct));
    }

    public static void copyPersisted(Player from, Player to) {
        CompoundTag old = from.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag cur = to.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        for (String k : new String[]{"ufc_h", "ufc_w", "ufc_a"}) {
            if (old.contains(k)) cur.putInt(k, old.getInt(k));
        }
        to.getPersistentData().put(Player.PERSISTED_NBT_TAG, cur);
    }

    private static void attrMod(LivingEntity e, Attribute attr, UUID id, double amount,
                                AttributeModifier.Operation op) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier old = inst.getModifier(id);
        if (old != null) {
            if (Math.abs(old.getAmount() - amount) < 1.0E-6) return;
            inst.removeModifier(id);
        }
        inst.addTransientModifier(new AttributeModifier(id, "ufcmod_profile", amount, op));
    }
}
