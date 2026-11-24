package com.example.ufcmod.net;

import com.example.ufcmod.UfcMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class Net {
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UfcMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ActionPacket.class,
                ActionPacket::encode, ActionPacket::decode, ActionPacket::handle);
        CHANNEL.registerMessage(id++, SyncPacket.class,
                SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
        CHANNEL.registerMessage(id++, AnimPacket.class,
                AnimPacket::encode, AnimPacket::decode, AnimPacket::handle);
        CHANNEL.registerMessage(id++, ProfilePacket.class,
                ProfilePacket::encode, ProfilePacket::decode, ProfilePacket::handle);
        CHANNEL.registerMessage(id++, SetProfilePacket.class,
                SetProfilePacket::encode, SetProfilePacket::decode, SetProfilePacket::handle);
    }
}
