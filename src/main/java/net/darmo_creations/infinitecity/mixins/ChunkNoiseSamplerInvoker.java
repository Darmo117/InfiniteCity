package net.darmo_creations.infinitecity.mixins;

import net.minecraft.block.*;
import net.minecraft.world.gen.chunk.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.*;

@Mixin(ChunkNoiseSampler.class)
public interface ChunkNoiseSamplerInvoker {
  @Invoker("getHorizontalCellBlockCount")
  int invokeGetHorizontalCellBlockCount();

  @Invoker("getVerticalCellBlockCount")
  int invokeGetVerticalCellBlockCount();

  @Invoker("sampleBlockState")
  BlockState invokeSampleBlockState();
}
