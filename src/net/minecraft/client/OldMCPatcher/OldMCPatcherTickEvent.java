package net.minecraft.client.OldMCPatcher;

import cpw.mods.fml.common.ITickHandler;

import java.util.EnumSet;

public class OldMCPatcherTickEvent implements ITickHandler {
    @Override
    public void tickStart(EnumSet enumSet, Object... object) {
        
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
