package com.example.ufcmod.client;

import com.example.ufcmod.UfcMod;
import com.example.ufcmod.fight.Action;
import com.example.ufcmod.fight.Profiles;
import com.example.ufcmod.net.ActionPacket;
import com.example.ufcmod.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UfcMod.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static boolean blockHeld = false;
    private static boolean subHeld = false;

    private static void send(Action a) {
        send(a, 0);
    }

    private static void send(Action a, int extra) {
        Net.CHANNEL.sendToServer(new ActionPacket(a, extra));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        ClientAnims.tick();
        fixHitboxes(mc);

        if (mc.player == null || mc.screen != null) {
            if (blockHeld) { blockHeld = false; if (mc.player != null) send(Action.BLOCK_STOP); }
            if (subHeld)   { subHeld = false;   if (mc.player != null) send(Action.SUB_STOP); }
            return;
        }

        while (Keys.PROFILE.consumeClick())   mc.setScreen(new ProfileScreen());
        while (Keys.STANCE.consumeClick())    send(Action.TOGGLE_STANCE);
        while (Keys.LEG_KICK.consumeClick())  send(Action.LEG_KICK);
        while (Keys.HEAD_KICK.consumeClick()) send(Action.HEAD_KICK);
        while (Keys.TAKEDOWN.consumeClick())  send(Action.TAKEDOWN);
        while (Keys.DODGE.consumeClick())     send(Action.DODGE, dodgeDirection(mc.options));

        boolean b = Keys.BLOCK.isDown();
        if (b != blockHeld) {
            blockHeld = b;
            send(b ? Action.BLOCK_START : Action.BLOCK_STOP);
        }
        boolean s = Keys.SUBMIT.isDown();
        if (s != subHeld) {
            subHeld = s;
            send(s ? Action.SUB_START : Action.SUB_STOP);
        }
    }

    /** safety net: if a profile arrived before the player entity existed, fix its hitbox later */
    private static void fixHitboxes(Minecraft mc) {
        if (mc.level == null || mc.level.getGameTime() % 20 != 0) return;
        for (Player p : mc.level.players()) {
            if (p.getPose() != Pose.STANDING) continue;
            float want = 1.8f * Profiles.get(p).scale();
            if (Math.abs(p.getBbHeight() - want) > 0.02f) p.refreshDimensions();
        }
    }

    /** 0 = back, 1 = left, 2 = right, 3 = forward */
    private static int dodgeDirection(Options o) {
        if (o.keyUp.isDown()) return 3;
        if (o.keyLeft.isDown()) return 1;
        if (o.keyRight.isDown()) return 2;
        return 0;
    }

    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered e) {
        if (!ClientFightData.stance) return;
        if (e.isAttack()) {
            e.setCanceled(true);
            e.setSwingHand(false);
            send(Action.JAB);
        } else if (e.isUseItem()) {
            e.setCanceled(true);
            e.setSwingHand(false);
            send(Action.CROSS);
        }
    }
}
