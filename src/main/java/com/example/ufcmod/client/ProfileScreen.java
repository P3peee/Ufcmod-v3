package com.example.ufcmod.client;

import com.example.ufcmod.fight.FighterProfile;
import com.example.ufcmod.fight.Profiles;
import com.example.ufcmod.net.Net;
import com.example.ufcmod.net.SetProfilePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/** Press P: choose height, weight and reach (ape index); stats update live. */
public class ProfileScreen extends Screen {
    private int h, w, a;

    public ProfileScreen() {
        super(Component.translatable("screen.ufcmod.profile"));
        FighterProfile cur = Profiles.get(net.minecraft.client.Minecraft.getInstance().player);
        h = cur.heightCm;
        w = cur.weightKg;
        a = cur.apePct;
    }

    @Override
    protected void init() {
        int x = 20, sw = 190, y = 50;
        addRenderableWidget(new ValueSlider(x, y, sw, FighterProfile.MIN_H, FighterProfile.MAX_H, h,
                "Height", " cm", v -> h = v));
        addRenderableWidget(new ValueSlider(x, y + 26, sw, FighterProfile.MIN_W, FighterProfile.MAX_W, w,
                "Weight", " kg", v -> w = v));
        addRenderableWidget(new ValueSlider(x, y + 52, sw, FighterProfile.MIN_A, FighterProfile.MAX_A, a,
                "Reach (ape index)", " %", v -> a = v));
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            Net.CHANNEL.sendToServer(new SetProfilePacket(h, w, a));
            onClose();
        }).bounds(x, y + 90, 92, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(x + 98, y + 90, 92, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);

        FighterProfile f = new FighterProfile(h, w, a);
        int x = 20, y = 170;
        g.drawString(font, f.weightClass(), x, y, 0xFFFFD25A, true);
        g.drawString(font, String.format("Wingspan: %.0f cm", f.reachCm()), x, y + 12, 0xFFDDDDDD, true);
        g.drawString(font, String.format("Power: x%.2f", f.damageMul()), x, y + 24, 0xFFDDDDDD, true);
        g.drawString(font, String.format("Chin (resist KO): x%.2f", 1f / f.chin()), x, y + 36, 0xFFDDDDDD, true);
        g.drawString(font, String.format("Strike speed: x%.2f", 1f / f.timeMul()), x, y + 48, 0xFFDDDDDD, true);
        g.drawString(font, String.format("Move speed: x%.2f", f.speedMul()), x, y + 60, 0xFFDDDDDD, true);

        if (minecraft != null && minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, width * 3 / 4, height / 2 + 70, 60,
                    (float) (width * 3 / 4 - mx), (float) (height / 2 + 10 - my), minecraft.player);
            g.drawCenteredString(font, "Current (applied) body", width * 3 / 4, height / 2 + 80, 0xFF999999);
        }
        super.render(g, mx, my, pt);
    }

    private static class ValueSlider extends AbstractSliderButton {
        private final int min, max;
        private final String label, unit;
        private final IntConsumer onChange;

        ValueSlider(int x, int y, int w, int min, int max, int val, String label, String unit, IntConsumer onChange) {
            super(x, y, w, 20, Component.empty(), (val - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.unit = unit;
            this.onChange = onChange;
            updateMessage();
        }

        private int intValue() {
            return min + (int) Math.round(this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + intValue() + unit));
        }

        @Override
        protected void applyValue() {
            onChange.accept(intValue());
        }
    }
}
