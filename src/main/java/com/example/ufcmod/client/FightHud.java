package com.example.ufcmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class FightHud {
    private static final int BAR_W = 100;

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        boolean idle = !ClientFightData.stance && ClientFightData.stamina >= 99.5f
                && ClientFightData.rock <= 0.5f && ClientFightData.stunned == 0
                && ClientFightData.grounded == 0;
        if (idle) return;

        int x = 10;
        int y = h - 52;

        // status line
        String status;
        int statusColor = 0xFFFFFFFF;
        if (ClientFightData.stunned > 0) { status = "KNOCKED DOWN"; statusColor = 0xFFFF5555; }
        else if (ClientFightData.grounded > 0) { status = "ON THE GROUND  [V] struggle"; statusColor = 0xFFFFAA00; }
        else if (ClientFightData.blocking) { status = "GUARD"; statusColor = 0xFF55AAFF; }
        else if (ClientFightData.stance) { status = "FIGHT STANCE"; statusColor = 0xFF55FF55; }
        else { status = "RECOVERING"; }
        g.drawString(mc.font, status, x, y - 22, statusColor, true);

        if (ClientFightData.combo > 1) {
            g.drawString(mc.font, "COMBO x" + ClientFightData.combo, x, y - 11, 0xFFFFDD55, true);
        }

        // stamina
        float st = Math.max(0f, Math.min(100f, ClientFightData.stamina));
        int stColor = st > 50 ? 0xFF55DD55 : st > 25 ? 0xFFEEDD33 : 0xFFEE3333;
        bar(g, x, y, st / 100f, stColor);
        g.drawString(mc.font, "STAMINA", x + BAR_W + 6, y - 1, 0xFFDDDDDD, true);

        // rocked meter
        float rk = Math.max(0f, Math.min(100f, ClientFightData.rock));
        bar(g, x, y + 10, rk / 100f, 0xFFFF8800);
        g.drawString(mc.font, "ROCKED", x + BAR_W + 6, y + 9, 0xFFDDDDDD, true);

        // submission progress
        if (ClientFightData.sub > 0) {
            bar(g, x, y + 20, ClientFightData.sub / 100f, 0xFFAA33FF);
            g.drawString(mc.font, "SUBMISSION", x + BAR_W + 6, y + 19, 0xFFDDDDDD, true);
        }
    }

    private static void bar(GuiGraphics g, int x, int y, float fill, int color) {
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + 7, 0xFF000000);
        g.fill(x, y, x + BAR_W, y + 6, 0xFF3A3A3A);
        g.fill(x, y, x + (int) (BAR_W * fill), y + 6, color);
    }
}
