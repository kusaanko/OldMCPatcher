package net.minecraft.client.OldMCPatcher;

import cpw.mods.fml.common.ITickHandler;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;

public class OldMCPatcherTickEvent implements ITickHandler {
    private Field loadTexture;
    private Field registerTexture;
    private Field renderEngine;
    private byte count;

    public OldMCPatcherTickEvent(Class mainClass) {
        try {
            loadTexture = ReflectionHelper.getField(mainClass, "loadTexture");
            registerTexture = ReflectionHelper.getField(mainClass, "registerTexture");
            renderEngine = ReflectionHelper.getField(mainClass, "renderEngine");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tickStart(EnumSet enumSet, Object... object) {
        count++;
        if(count == 2) {
            count = 0;
            try {
                String name = (String) loadTexture.get(null);
                if (name != null) {
                    try {
                        ((Method) registerTexture.get(null)).invoke(renderEngine.get(null), name);
                        System.out.println("Register texture " + name);
                        loadTexture.set(null, null);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void tickEnd(EnumSet enumSet, Object... object) {

    }

    @Override
    public EnumSet ticks() {
        try {
            for(Object enu : getClass().getClassLoader().loadClass("cpw.mods.fml.common.TickType").getEnumConstants()) {
                if(enu.toString().equals("CLIENT")) {
                    return EnumSet.of((Enum) enu);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getLabel() {
        return "OldMCPatcher";
    }
}
