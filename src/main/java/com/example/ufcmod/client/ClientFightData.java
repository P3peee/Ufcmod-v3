package com.example.ufcmod.client;

import com.example.ufcmod.net.SyncPacket;

/** Client-side copy of the fighter state (no client-only imports, safe to load anywhere). */
public class ClientFightData {
    public static float stamina = 100f, rock = 0f;
    public static boolean stance = false, blocking = false;
    public static int stunned = 0, grounded = 0, combo = 0, sub = 0;

    public static void apply(SyncPacket m) {
        stamina = m.stamina;
        rock = m.rock;
        stance = m.stance;
        blocking = m.blocking;
        stunned = m.stunned;
        grounded = m.grounded;
        combo = m.combo;
        sub = m.sub;
    }
}
