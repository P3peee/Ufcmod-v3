package com.example.ufcmod.net;

import com.example.ufcmod.client.ClientFightData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** server -> client: HUD data */
public class SyncPacket {
    public final float stamina, rock;
    public final boolean stance, blocking;
    public final int stunned, grounded, combo, sub;

    public SyncPacket(float stamina, float rock, boolean stance, boolean blocking,
                      int stunned, int grounded, int combo, int sub) {
        this.stamina = stamina;
        this.rock = rock;
        this.stance = stance;
        this.blocking = blocking;
        this.stunned = stunned;
        this.grounded = grounded;
        this.combo = combo;
        this.sub = sub;
    }

    public static void encode(SyncPacket m, FriendlyByteBuf b) {
        b.writeFloat(m.stamina);
        b.writeFloat(m.rock);
        b.writeBoolean(m.stance);
        b.writeBoolean(m.blocking);
        b.writeVarInt(m.stunned);
        b.writeVarInt(m.grounded);
        b.writeVarInt(m.combo);
        b.writeVarInt(m.sub);
    }

    public static SyncPacket decode(FriendlyByteBuf b) {
        return new SyncPacket(b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean(),
                b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(SyncPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFightData.apply(m)));
        ctx.get().setPacketHandled(true);
    }
}
