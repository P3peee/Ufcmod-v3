package com.example.ufcmod.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class Keys {
    private static final String CAT = "key.categories.ufcmod";
    public static final KeyMapping STANCE    = new KeyMapping("key.ufcmod.stance",    GLFW.GLFW_KEY_R, CAT);
    public static final KeyMapping LEG_KICK  = new KeyMapping("key.ufcmod.leg_kick",  GLFW.GLFW_KEY_Z, CAT);
    public static final KeyMapping HEAD_KICK = new KeyMapping("key.ufcmod.head_kick", GLFW.GLFW_KEY_X, CAT);
    public static final KeyMapping BLOCK     = new KeyMapping("key.ufcmod.block",     GLFW.GLFW_KEY_C, CAT);
    public static final KeyMapping DODGE     = new KeyMapping("key.ufcmod.dodge",     GLFW.GLFW_KEY_V, CAT);
    public static final KeyMapping TAKEDOWN  = new KeyMapping("key.ufcmod.takedown",  GLFW.GLFW_KEY_G, CAT);
    public static final KeyMapping SUBMIT    = new KeyMapping("key.ufcmod.submit",    GLFW.GLFW_KEY_H, CAT);
    public static final KeyMapping PROFILE   = new KeyMapping("key.ufcmod.profile",   GLFW.GLFW_KEY_P, CAT);
}
