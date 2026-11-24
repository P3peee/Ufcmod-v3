package com.example.ufcmod.client;

import com.example.ufcmod.fight.FighterProfile;
import com.example.ufcmod.fight.Profiles;
import com.example.ufcmod.net.AnimPacket;
import com.example.ufcmod.net.ProfilePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Entry points called from packets on the client thread. */
public class ClientHooks {
    public static void onAnim(AnimPacket m) {
        ClientAnims.apply(m);
    }

    public static void onProfile(ProfilePacket m) {
        Profiles.CLIENT.put(m.uuid, new FighterProfile(m.h, m.w, m.a));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Player p = mc.level.getPlayerByUUID(m.uuid);
            if (p != null) p.refreshDimensions();
        }
    }
}
