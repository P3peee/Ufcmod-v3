package com.example.ufcmod.net;

import com.example.ufcmod.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** server -> clients: height / weight / ape index of a player */
public class ProfilePacket {
    public final UUID uuid;
    public final int h, w, a;

    public ProfilePacket(UUID uuid, int h, int w, int a) {
        this.uuid = uuid;
        this.h = h;
        this.w = w;
        this.a = a;
    }

    public static void encode(ProfilePacket m, FriendlyByteBuf b) {
        b.writeUUID(m.uuid);
        b.writeVarInt(m.h);
        b.writeVarInt(m.w);
        b.writeVarInt(m.a);
    }

    public static ProfilePacket decode(FriendlyByteBuf b) {
        return new ProfilePacket(b.readUUID(), b.readVarInt(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(ProfilePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onProfile(m)));
        ctx.get().setPacketHandled(true);
    }
}
