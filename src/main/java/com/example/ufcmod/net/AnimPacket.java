package com.example.ufcmod.net;

import com.example.ufcmod.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** server -> clients: pose flags + optional one-shot animation of an entity */
public class AnimPacket {
    public final int entityId, flags, oneShot, param, duration;

    public AnimPacket(int entityId, int flags, int oneShot, int param, int duration) {
        this.entityId = entityId;
        this.flags = flags;
        this.oneShot = oneShot;
        this.param = param;
        this.duration = duration;
    }

    public static void encode(AnimPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.entityId);
        b.writeVarInt(m.flags);
        b.writeVarInt(m.oneShot + 1);
        b.writeVarInt(m.param);
        b.writeVarInt(m.duration);
    }

    public static AnimPacket decode(FriendlyByteBuf b) {
        return new AnimPacket(b.readVarInt(), b.readVarInt(), b.readVarInt() - 1, b.readVarInt(), b.readVarInt());
    }

    public static void handle(AnimPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onAnim(m)));
        ctx.get().setPacketHandled(true);
    }
}
