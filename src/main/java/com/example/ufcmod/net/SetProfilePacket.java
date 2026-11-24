package com.example.ufcmod.net;

import com.example.ufcmod.fight.FightManager;
import com.example.ufcmod.fight.FighterState;
import com.example.ufcmod.fight.Profiles;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** client -> server: "apply my new body" (values are clamped on the server) */
public class SetProfilePacket {
    public final int h, w, a;

    public SetProfilePacket(int h, int w, int a) {
        this.h = h;
        this.w = w;
        this.a = a;
    }

    public static void encode(SetProfilePacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.h);
        b.writeVarInt(m.w);
        b.writeVarInt(m.a);
    }

    public static SetProfilePacket decode(FriendlyByteBuf b) {
        return new SetProfilePacket(b.readVarInt(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(SetProfilePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer p = ctx.get().getSender();
            if (p == null) return;
            FighterState s = FightManager.peek(p);
            if (s != null && (s.stunned > 0 || s.grounded > 0)) return; // not while knocked down
            Profiles.set(p, m.h, m.w, m.a);
        });
        ctx.get().setPacketHandled(true);
    }
}
