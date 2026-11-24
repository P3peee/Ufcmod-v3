package com.example.ufcmod.fight;

/**
 * Frame data, like in a fighting game. Times are in ticks (20 = 1 second).
 * startup  = delay before the hit lands (can be dodged / countered in this window)
 * recovery = time after the hit during which you can't act
 */
public enum Move {
    //          startup recovery cost  damage rock  range knockback high   low
    JAB(        2,      7,       4f,   3.0f,  7f,   2.7,  0.10f,    true,  false),
    CROSS(      4,      11,      8f,   5.5f,  13f,  2.7,  0.30f,    true,  false),
    LEG_KICK(   5,      15,      10f,  4.0f,  0f,   3.0,  0.00f,    false, true),
    HEAD_KICK(  8,      20,      16f,  8.0f,  28f,  3.2,  0.60f,    true,  false),
    TAKEDOWN(   6,      26,      20f,  0.0f,  0f,   2.4,  0.00f,    false, false);

    public final int startup, recovery;
    public final float cost, damage, rock, knockback;
    public final double range;
    public final boolean high, low;

    Move(int startup, int recovery, float cost, float damage, float rock,
         double range, float knockback, boolean high, boolean low) {
        this.startup = startup;
        this.recovery = recovery;
        this.cost = cost;
        this.damage = damage;
        this.rock = rock;
        this.range = range;
        this.knockback = knockback;
        this.high = high;
        this.low = low;
    }
}
