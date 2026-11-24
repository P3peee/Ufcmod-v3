package com.example.ufcmod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UfcMod.MODID);

    public static final RegistryObject<CreativeModeTab> UFC_TAB = TABS.register("ufc_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ufcmod"))
                    .icon(() -> new ItemStack(ModItems.MMA_GLOVES.get()))
                    .displayItems((params, out) -> {
                        out.accept(ModItems.MMA_GLOVES.get());
                        out.accept(ModItems.CHAMPION_BELT.get());
                        out.accept(ModItems.OCTAGON_MAT.get());
                    })
                    .build());
}
