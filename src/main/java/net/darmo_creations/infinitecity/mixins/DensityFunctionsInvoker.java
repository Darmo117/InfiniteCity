package net.darmo_creations.infinitecity.mixins;

import net.minecraft.registry.*;
import net.minecraft.util.math.noise.*;
import net.minecraft.world.gen.densityfunction.*;
import net.minecraft.world.gen.noise.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.*;

@Mixin(DensityFunctions.class)
public interface DensityFunctionsInvoker {
  @Invoker("createSurfaceNoiseRouter")
  static NoiseRouter invokeCreateSurfaceNoiseRouter(
      RegistryEntryLookup<DensityFunction> densityFunctionLookup,
      RegistryEntryLookup<DoublePerlinNoiseSampler.NoiseParameters> noiseParametersLookup,
      boolean largeBiomes,
      boolean amplified
  ) {
    throw new AssertionError();
  }
}
