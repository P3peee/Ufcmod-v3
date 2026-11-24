package com.example.ufcmod.client;

import com.example.ufcmod.fight.AnimFlags;
import com.example.ufcmod.fight.AnimType;
import com.example.ufcmod.net.AnimPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Client-side animation state of every fighter we can see (key = entity id). */
public class ClientAnims {
    public static class Anim {
        public int flags;
        public AnimType type;
        public int param, duration;
        public float startAge;
        public float lie, lieO; // 0 = standing, 1 = fully lying down

        public boolean active(float age) {
            return type != null && age >= startAge && age - startAge < duration;
        }

        public float t(float age) {
            return age - startAge;
        }
    }

    public static final Map<Integer, Anim> MAP = new HashMap<>();

    public static Anim get(int id) {
        return MAP.get(id);
    }

    public static void apply(AnimPacket m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(m.entityId);
        Anim a = MAP.computeIfAbsent(m.entityId, id -> new Anim());
        a.flags = m.flags;
        if (m.oneShot >= 0 && m.oneShot < AnimType.values().length) {
            a.type = AnimType.values()[m.oneShot];
            a.param = m.param;
            a.duration = Math.max(1, m.duration);
            a.startAge = e != null ? e.tickCount : 0f;
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            MAP.clear();
            return;
        }
        Iterator<Map.Entry<Integer, Anim>> it = MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Anim> en = it.next();
            Entity e = mc.level.getEntity(en.getKey());
            if (e == null) {
                it.remove();
                continue;
            }
            Anim a = en.getValue();
            a.lieO = a.lie;
            boolean grounded = (a.flags & AnimFlags.GROUNDED) != 0;
            a.lie = grounded ? Math.min(1f, a.lie + 0.2f) : Math.max(0f, a.lie - 0.15f);
            if (a.flags == 0 && a.lie <= 0f && !a.active(e.tickCount)) it.remove();
        }
    }
}
