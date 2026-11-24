package com.example.ufcmod.net;

import com.example.ufcmod.fight.Action;
import com.example.ufcmod.fight.FightManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** client -> server */
public class ActionPacket {
    public final Action action;
    public final int extra;

    public ActionPacket(Action action, int extra) {
        this.action = action;
        this.extra = extra;
    }

    public static void encode(ActionPacket m, FriendlyByteBuf buf) {
        buf.writeEnum(m.action);
        buf.writeVarInt(m.extra);
    }

    public static ActionPacket decode(FriendlyByteBuf buf) {
        return new ActionPacket(buf.readEnum(Action.class), buf.readVarInt());
    }

    public static void handle(ActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) FightManager.handleAction(p, m.action, m.extra);
        });
        ctx.get().setPacketHandled(true);
    }
}
