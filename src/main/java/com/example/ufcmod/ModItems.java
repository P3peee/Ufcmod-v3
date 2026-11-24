package com.example.ufcmod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, UfcMod.MODID);

    public static final RegistryObject<Item> MMA_GLOVES = ITEMS.register("mma_gloves",
            () -> new GloveItem(new Item.Properties().durability(500)));

    public static final RegistryObject<Item> CHAMPION_BELT = ITEMS.register("champion_belt",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> OCTAGON_MAT = ITEMS.register("octagon_mat",
            () -> new BlockItem(ModBlocks.OCTAGON_MAT.get(), new Item.Properties()));
}
