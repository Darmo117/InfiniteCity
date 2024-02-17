package net.darmo_creations.infinitecity.chunk_gen;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.*;
import net.minecraft.registry.entry.*;
import net.minecraft.world.biome.*;

/**
 * The configuration class for {@link InfiniteCityChunkGenerator}.
 *
 * @param biome The single biome to use.
 */
public record InfiniteCityChunkGeneratorConfig(RegistryEntry<Biome> biome) {
  public static final Codec<InfiniteCityChunkGeneratorConfig> CODEC = RecordCodecBuilder.<InfiniteCityChunkGeneratorConfig>create(
      instance -> instance
          .group(Biome.REGISTRY_CODEC.fieldOf("biome").forGetter(config -> config.biome))
          .apply(instance, InfiniteCityChunkGeneratorConfig::new)
  ).stable();
}
