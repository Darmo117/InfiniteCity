package net.darmo_creations.infinitecity.mixins;

import net.minecraft.world.*;
import net.minecraft.world.gen.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.*;

/**
 * This mixin makes the private {@code StructureAccessor.world} attribute publicly accessible.
 */
@Mixin(StructureAccessor.class)
public interface StructureAccessorAccessor {
  @Accessor
  WorldAccess getWorld();
}
