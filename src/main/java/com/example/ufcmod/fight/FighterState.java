package com.example.ufcmod.fight;

import net.minecraft.world.entity.LivingEntity;

public class FighterState {
    public final LivingEntity entity;

    public float stamina = 100f;
    public float rock = 0f;
    public boolean stance = false;
    public boolean blockHeld = false;
    public boolean subHeld = false;

    public int cooldown = 0;
    public int staminaDelay = 0;
    public int stunned = 0;
    public int grounded = 0;
    public int iframes = 0;
    public int comboCount = 0;
    public int comboTimer = 0;
    public int submitTicks = 0;

    public Move pending = null;
    public int pendingTicks = 0;
    public boolean alt = false;
    public boolean wasActive = false;
    public int lastFlags = 0;

    public FighterState(LivingEntity entity) {
        this.entity = entity;
    }

    public boolean isBlocking() {
        return blockHeld && stance && stunned == 0 && grounded == 0 && pending == null && cooldown == 0;
    }
}
