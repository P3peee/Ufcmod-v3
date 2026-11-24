package com.example.ufcmod.fight;

import net.minecraft.util.Mth;

/**
 * Body of a fighter: height (cm), weight (kg) and "ape index" (wingspan / height, in percent).
 * Everything else (reach, power, speed, chin...) is derived from these three numbers.
 */
public class FighterProfile {
    public static final int MIN_H = 150, MAX_H = 215;
    public static final int MIN_W = 52, MAX_W = 130;
    public static final int MIN_A = 94, MAX_A = 108;
    public static final FighterProfile DEFAULT = new FighterProfile(180, 77, 100);

    public final int heightCm, weightKg, apePct;

    public FighterProfile(int h, int w, int a) {
        this.heightCm = Mth.clamp(h, MIN_H, MAX_H);
        this.weightKg = Mth.clamp(w, MIN_W, MAX_W);
        this.apePct = Mth.clamp(a, MIN_A, MAX_A);
    }

    /** model scale: 180 cm = normal Minecraft player */
    public float scale() { return heightCm / 180f; }
    public float reachCm() { return heightCm * apePct / 100f; }
    public float reachFactor() { return reachCm() / 180f; }
    public float armLength() { return apePct / 100f; }
    public float mass() { return weightKg / 77f; }

    /** visual thickness from BMI */
    public float fat() {
        float h = heightCm / 100f;
        float bmi = weightKg / (h * h);
        return Mth.clamp((float) Math.sqrt(bmi / 23.8f), 0.82f, 1.25f);
    }

    /** > 1 = slower strikes (tall / heavy), < 1 = faster */
    public float timeMul() {
        return Mth.clamp((float) (Math.pow(scale(), 0.6) * Math.pow(mass(), 0.12)), 0.85f, 1.2f);
    }
    public float damageMul() { return (float) Math.pow(mass(), 0.55); }
    public float chin() { return (float) Math.pow(mass(), -0.5); }          // rock taken
    public float staminaCostMul() { return (float) Math.pow(mass(), 0.3); }
    public float regenMul() { return (float) Math.pow(mass(), -0.25); }
    public float speedMul() { return Mth.clamp(1f - (mass() - 1f) * 0.12f - (scale() - 1f) * 0.05f, 0.9f, 1.1f); }
    public float kbResist() { return Mth.clamp((mass() - 1f) * 0.5f, 0f, 0.35f); }

    public String weightClass() {
        if (weightKg <= 57) return "Flyweight";
        if (weightKg <= 61) return "Bantamweight";
        if (weightKg <= 66) return "Featherweight";
        if (weightKg <= 70) return "Lightweight";
        if (weightKg <= 77) return "Welterweight";
        if (weightKg <= 84) return "Middleweight";
        if (weightKg <= 93) return "Light Heavyweight";
        if (weightKg <= 120) return "Heavyweight";
        return "Super Heavyweight";
    }
}
