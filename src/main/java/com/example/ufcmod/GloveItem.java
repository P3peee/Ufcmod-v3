package com.example.ufcmod;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class GloveItem extends SwordItem {
    private static final float KO_CHANCE = 0.10f;

    public GloveItem(Properties props) {
        // damage 3 (+1 base), attack speed 3.0 (fast jabs)
        super(Tiers.WOOD, 3, -1.0f, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            float yaw = attacker.getYRot() * Mth.DEG_TO_RAD;
            target.knockback(0.6D, Mth.sin(yaw), -Mth.cos(yaw));

            if (attacker.getRandom().nextFloat() < KO_CHANCE) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                attacker.level().playSound(null, target.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 0.8f);
                if (attacker instanceof Player p) {
                    p.displayClientMessage(Component.literal("KO!"), true);
                }
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }
}
