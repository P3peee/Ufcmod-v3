package com.example.ufcmod.client;

import com.example.ufcmod.UfcMod;
import com.example.ufcmod.fight.AnimFlags;
import com.example.ufcmod.fight.Profiles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Whole-body effects done with the pose stack, for ANY living entity:
 * height scale, falling down / lying on the ground, dodge lean, takedown dive, rocked wobble.
 */
@Mod.EventBusSubscriber(modid = UfcMod.MODID, value = Dist.CLIENT)
public class RenderEvents {

    @SubscribeEvent
    public static void onPre(RenderLivingEvent.Pre<?, ?> e) {
        LivingEntity le = e.getEntity();
        PoseStack ps = e.getPoseStack();
        float pt = e.getPartialTick();

        if (le instanceof Player p) {
            float s = Profiles.get(p).scale();
            if (Math.abs(s - 1f) > 0.001f) ps.scale(s, s, s);
        }

        ClientAnims.Anim a = ClientAnims.get(le.getId());
        if (a == null) return;

        float age = le.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, le.yBodyRotO, le.yBodyRot);
        float lie = AnimMath.smooth(Mth.lerp(pt, a.lieO, a.lie));

        float xDeg = 0f, zDeg = 0f, up = 0f;
        if (lie > 0f) {
            xDeg = 90f * lie;      // fall onto the back
            up = 0.22f * lie;
        } else {
            if ((a.flags & AnimFlags.STUNNED) != 0) {
                zDeg += Mth.sin(age * 0.5f) * 7f;   // rocked wobble
            }
            if (a.type != null && a.active(age)) {
                float t = a.t(age);
                float bell = Mth.sin(Mth.PI * Mth.clamp(t / a.duration, 0f, 1f));
                switch (a.type) {
                    case DODGE -> {
                        // +x = lean back, -x = lean forward, +z = lean left, -z = lean right
                        switch (a.param) {
                            case 0 -> xDeg += 16f * bell;
                            case 3 -> xDeg -= 12f * bell;
                            case 1 -> zDeg += 16f * bell;
                            case 2 -> zDeg -= 16f * bell;
                            default -> { }
                        }
                    }
                    case TAKEDOWN -> {
                        float ex = Math.max(AnimMath.ext(t, a.param, a.duration), 0f);
                        xDeg -= 22f * ex;
                    }
                    case HIT -> xDeg += 6f * bell;
                    default -> { }
                }
            }
        }

        if (up != 0f) ps.translate(0.0, up, 0.0);
        if (xDeg != 0f || zDeg != 0f) {
            // rotate around the feet, in the entity's own frame (vanilla yaw is applied afterwards)
            float r = 180f - bodyYaw;
            ps.mulPose(Axis.YP.rotationDegrees(r));
            ps.mulPose(Axis.XP.rotationDegrees(xDeg));
            ps.mulPose(Axis.ZP.rotationDegrees(zDeg));
            ps.mulPose(Axis.YP.rotationDegrees(-r));
        }
    }

    /** camera shakes when YOU are rocked / knocked out */
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ClientAnims.Anim a = ClientAnims.get(mc.player.getId());
        if (a == null) return;
        if ((a.flags & AnimFlags.STUNNED) != 0) {
            float age = mc.player.tickCount + (float) e.getPartialTick();
            e.setRoll(Mth.sin(age * 0.4f) * 6f);
        }
    }
}
