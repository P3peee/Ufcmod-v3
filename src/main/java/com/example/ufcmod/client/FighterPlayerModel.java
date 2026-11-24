package com.example.ufcmod.client;

import com.example.ufcmod.fight.AnimFlags;
import com.example.ufcmod.fight.FighterProfile;
import com.example.ufcmod.fight.Profiles;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * Player model that (1) shapes the body from the fighter profile (arm length = reach, thickness = weight)
 * and (2) poses the limbs for stance / guard / strikes / kicks / takedown / ground.
 *
 * Rotation notes (Minecraft model space):
 *  - arm.xRot  negative = arm raised forward ( -1.57 = straight forward )
 *  - right arm yRot negative = swings toward the centre, left arm yRot positive = toward the centre
 *  - leg.xRot  negative = leg forward
 *  - body.xRot positive = lean forward
 */
public class FighterPlayerModel extends PlayerModel<AbstractClientPlayer> {
    private static final float G = -1.45f;      // guard arm pitch
    private static final float STRAIGHT = -1.5708f;

    public FighterPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AbstractClientPlayer p, float ls, float lsa, float age, float yaw, float pitch) {
        super.setupAnim(p, ls, lsa, age, yaw, pitch);
        shape(Profiles.get(p));
        ClientAnims.Anim a = ClientAnims.get(p.getId());
        if (a != null) pose(a, lsa, age);
        syncOverlays();
    }

    // ------------------------------------------------------------ body shape
    private void shape(FighterProfile pr) {
        float f = pr.fat();
        float al = pr.armLength();

        body.xScale = f;      body.zScale = f;
        rightArm.xScale = f;  rightArm.zScale = f;  rightArm.yScale = al;
        leftArm.xScale = f;   leftArm.zScale = f;   leftArm.yScale = al;
        rightLeg.xScale = f;  rightLeg.zScale = f;
        leftLeg.xScale = f;   leftLeg.zScale = f;
        rightLeg.yScale = 1f; leftLeg.yScale = 1f; body.yScale = 1f;

        rightArm.x = -5f * f;
        leftArm.x = 5f * f;
        rightLeg.x = -1.9f * f;
        leftLeg.x = 1.9f * f;
    }

    private void syncOverlays() {
        copyAll(leftPants, leftLeg);
        copyAll(rightPants, rightLeg);
        copyAll(leftSleeve, leftArm);
        copyAll(rightSleeve, rightArm);
        copyAll(jacket, body);
        copyAll(hat, head);
    }

    private static void copyAll(ModelPart dst, ModelPart src) {
        dst.copyFrom(src);
        dst.xScale = src.xScale;
        dst.yScale = src.yScale;
        dst.zScale = src.zScale;
    }

    private static boolean has(ClientAnims.Anim a, int flag) {
        return (a.flags & flag) != 0;
    }

    // ------------------------------------------------------------ poses
    private void pose(ClientAnims.Anim a, float lsa, float age) {
        boolean grounded = has(a, AnimFlags.GROUNDED);
        boolean stunned = has(a, AnimFlags.STUNNED);
        boolean stance = has(a, AnimFlags.STANCE);
        boolean block = has(a, AnimFlags.BLOCK);
        boolean sub = has(a, AnimFlags.SUBMIT);

        if (grounded || a.lie > 0.05f) {
            lying();
            return;
        }
        if (stunned) {
            rocked(age);
            return;
        }
        if (!stance) return;

        // ---- base fight stance
        float walk = Mth.clamp(lsa * 2f, 0f, 1f);
        float bounce = Mth.sin(age * 0.35f) * 0.04f * (1f - walk);
        float rg = block ? -1.75f : G;
        float yg = block ? 0.75f : 0.45f;

        rightArm.xRot = rg + bounce;  rightArm.yRot = -yg; rightArm.zRot = 0f;
        leftArm.xRot  = rg - bounce;  leftArm.yRot  = yg;  leftArm.zRot  = 0f;
        body.xRot = block ? 0.25f : 0.12f;
        body.yRot = 0f;
        body.zRot = 0f;
        leftLeg.xRot  = Mth.lerp(walk, -0.30f + bounce, leftLeg.xRot);
        rightLeg.xRot = Mth.lerp(walk,  0.30f - bounce, rightLeg.xRot);

        if (sub) {
            body.xRot = 0.9f;
            rightArm.xRot = -1.15f; rightArm.yRot = -0.3f;
            leftArm.xRot  = -1.15f; leftArm.yRot  = 0.3f;
            leftLeg.xRot = -0.9f;   rightLeg.xRot = 0.7f;
            return;
        }

        // ---- one-shot move on top
        if (a.type == null || !a.active(age)) return;
        float t = a.t(age);
        float hit = a.param;
        float dur = a.duration;

        switch (a.type) {
            case JAB -> {
                float e = AnimMath.ext(t, hit, dur), e0 = Math.max(e, 0f);
                leftArm.xRot = Mth.lerp(e0, G, STRAIGHT) + Math.min(e, 0f) * 0.6f;
                leftArm.yRot = Mth.lerp(e0, 0.45f, 0.03f);
                body.yRot = 0.25f * e0;
                leftLeg.xRot = Mth.lerp(e0, leftLeg.xRot, -0.55f);
                rightLeg.xRot = Mth.lerp(e0, rightLeg.xRot, 0.40f);
            }
            case CROSS -> {
                float e = AnimMath.ext(t, hit, dur), e0 = Math.max(e, 0f);
                rightArm.xRot = Mth.lerp(e0, G, STRAIGHT) + Math.min(e, 0f) * 0.6f;
                rightArm.yRot = Mth.lerp(e0, -0.45f, -0.03f);
                body.yRot = -0.55f * e0;
                leftLeg.xRot = Mth.lerp(e0, leftLeg.xRot, -0.55f);
                rightLeg.xRot = Mth.lerp(e0, rightLeg.xRot, 0.55f);
            }
            case LEG_KICK -> kick(t, hit, dur, 0.95f, -0.10f);
            case HEAD_KICK -> kick(t, hit, dur, 1.95f, -0.40f);
            case TAKEDOWN -> {
                float e = AnimMath.ext(t, hit, dur), e0 = Math.max(e, 0f);
                body.xRot += 1.0f * e0;
                rightArm.xRot = Mth.lerp(e0, rightArm.xRot, -1.95f);
                leftArm.xRot = Mth.lerp(e0, leftArm.xRot, -1.95f);
                rightArm.yRot = Mth.lerp(e0, rightArm.yRot, -0.10f);
                leftArm.yRot = Mth.lerp(e0, leftArm.yRot, 0.10f);
                leftLeg.xRot = Mth.lerp(e0, leftLeg.xRot, -0.9f);
                rightLeg.xRot = Mth.lerp(e0, rightLeg.xRot, 0.8f);
            }
            case DODGE -> {
                float bell = Mth.sin(Mth.PI * Mth.clamp(t / dur, 0f, 1f));
                int dir = a.param;
                if (dir == 0) body.xRot -= 0.35f * bell;
                else if (dir == 3) body.xRot += 0.30f * bell;
                leftLeg.xRot += 0.5f * bell;
                rightLeg.xRot -= 0.5f * bell;
            }
            case HIT -> {
                float bell = Mth.sin(Mth.PI * Mth.clamp(t / dur, 0f, 1f));
                head.xRot -= 0.5f * bell;
                body.xRot -= 0.18f * bell;
            }
        }
    }

    private void kick(float t, float hit, float dur, float peak, float lean) {
        float e = AnimMath.ext(t, hit, dur), e0 = Math.max(e, 0f);
        rightLeg.xRot = Mth.lerp(e0, rightLeg.xRot, -peak) + Math.min(e, 0f) * -0.5f;
        leftLeg.xRot = Mth.lerp(e0, leftLeg.xRot, 0.1f);
        body.xRot += lean * e0;
        // arms spread for balance
        rightArm.yRot += 0.9f * e0;
        leftArm.yRot -= 0.9f * e0;
    }

    private void lying() {
        rightArm.xRot = 0f; leftArm.xRot = 0f;
        rightArm.yRot = 0f; leftArm.yRot = 0f;
        rightArm.zRot = 0.4f; leftArm.zRot = -0.4f;
        rightLeg.xRot = 0f; leftLeg.xRot = 0f;
        rightLeg.zRot = 0.1f; leftLeg.zRot = -0.1f;
        body.xRot = 0f; body.yRot = 0f; body.zRot = 0f;
        head.xRot = 0f;
    }

    private void rocked(float age) {
        float w = Mth.sin(age * 0.6f);
        body.xRot = 0.35f;
        head.xRot = 0.5f;
        rightArm.xRot = -0.3f + w * 0.3f; leftArm.xRot = -0.3f - w * 0.3f;
        rightArm.yRot = 0f; leftArm.yRot = 0f;
        rightArm.zRot = 0.2f; leftArm.zRot = -0.2f;
        rightLeg.xRot = 0.35f * w; leftLeg.xRot = -0.35f * w;
    }
}
