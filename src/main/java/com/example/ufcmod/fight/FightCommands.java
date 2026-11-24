package com.example.ufcmod.fight;

import com.example.ufcmod.UfcMod;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /ufc info | height <cm> | weight <kg> | reach <ape%> */
@Mod.EventBusSubscriber(modid = UfcMod.MODID)
public class FightCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("ufc")
                .then(Commands.literal("info").executes(c -> {
                    info(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("height").then(Commands.argument("cm",
                        IntegerArgumentType.integer(FighterProfile.MIN_H, FighterProfile.MAX_H)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    FighterProfile o = Profiles.get(p);
                    Profiles.set(p, IntegerArgumentType.getInteger(c, "cm"), o.weightKg, o.apePct);
                    info(p);
                    return 1;
                })))
                .then(Commands.literal("weight").then(Commands.argument("kg",
                        IntegerArgumentType.integer(FighterProfile.MIN_W, FighterProfile.MAX_W)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    FighterProfile o = Profiles.get(p);
                    Profiles.set(p, o.heightCm, IntegerArgumentType.getInteger(c, "kg"), o.apePct);
                    info(p);
                    return 1;
                })))
                .then(Commands.literal("reach").then(Commands.argument("ape",
                        IntegerArgumentType.integer(FighterProfile.MIN_A, FighterProfile.MAX_A)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    FighterProfile o = Profiles.get(p);
                    Profiles.set(p, o.heightCm, o.weightKg, IntegerArgumentType.getInteger(c, "ape"));
                    info(p);
                    return 1;
                }))));
    }

    private static void info(ServerPlayer p) {
        FighterProfile f = Profiles.get(p);
        p.sendSystemMessage(Component.literal(String.format(
                "%s | %d cm | reach %.0f cm (ape %d%%) | %d kg | power x%.2f | chin x%.2f | speed x%.2f",
                f.weightClass(), f.heightCm, f.reachCm(), f.apePct, f.weightKg,
                f.damageMul(), 1f / f.chin(), f.speedMul())));
    }
}
