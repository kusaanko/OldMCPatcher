package net.minecraft.client.OldMCPatcher;

import java.util.EnumSet;

public interface ITickHandler {
   void tickStart(EnumSet var1, Object... var2);

   void tickEnd(EnumSet var1, Object... var2);

   EnumSet ticks();

   String getLabel();
}
