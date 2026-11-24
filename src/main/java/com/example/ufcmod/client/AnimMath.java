package com.example.ufcmod.client;

import net.minecraft.util.Mth;

public class AnimMath {
    public static float smooth(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    /**
     * Strike extension curve: small pull-back (-0.25), snap to full extension (1.0) exactly at
     * the 'hit' tick, hold briefly, then return to guard (0).
     */
    public static float ext(float t, float hit, float dur) {
        if (hit < 1f) hit = 1f;
        if (t < hit) {
            float u = t / hit;
            if (u < 0.4f) return -0.25f * smooth(u / 0.4f);
            return Mth.lerp(smooth((u - 0.4f) / 0.6f), -0.25f, 1f);
        }
        float u2 = (t - hit) / Math.max(1f, dur - hit);
        if (u2 < 0.15f) return 1f;
        return 1f - smooth((u2 - 0.15f) / 0.85f);
    }
}
