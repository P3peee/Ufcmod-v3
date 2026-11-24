package com.example.ufcmod.client;

import com.example.ufcmod.UfcMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = UfcMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent e) {
        e.register(Keys.STANCE);
        e.register(Keys.LEG_KICK);
        e.register(Keys.HEAD_KICK);
        e.register(Keys.BLOCK);
        e.register(Keys.DODGE);
        e.register(Keys.TAKEDOWN);
        e.register(Keys.SUBMIT);
        e.register(Keys.PROFILE);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("fight_hud", FightHud::render);
    }

    /** Swap the vanilla player model for our animated one (both "default" and "slim" skins). */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers e) {
        for (String skin : e.getSkins()) {
            PlayerRenderer r = e.getSkin(skin);
            if (r == null) continue;
            boolean slim = skin.equals("slim");
            FighterPlayerModel model = new FighterPlayerModel(
                    e.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
            if (!swapModel(r, model)) {
                UfcMod.LOGGER.error("UFC mod: could not install animated player model for skin '{}'", skin);
            }
        }
    }

    /** LivingEntityRenderer.model is protected; find it by type so it works with any mappings. */
    private static boolean swapModel(LivingEntityRenderer<?, ?> renderer, EntityModel<?> model) {
        try {
            for (Field f : LivingEntityRenderer.class.getDeclaredFields()) {
                if (f.getType() == EntityModel.class) {
                    f.setAccessible(true);
                    f.set(renderer, model);
                    return true;
                }
            }
        } catch (Exception ex) {
            UfcMod.LOGGER.error("UFC mod: reflection failed", ex);
        }
        return false;
    }
}
