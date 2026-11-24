package com.example.ufcmod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Octagon mat: soft landing, reduces fall damage by 80%. */
public class MatBlock extends Block {
    public MatBlock(Properties props) {
        super(props);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        entity.causeFallDamage(distance, 0.2F, entity.damageSources().fall());
    }
}
