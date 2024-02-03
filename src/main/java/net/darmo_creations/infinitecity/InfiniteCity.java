package net.darmo_creations.infinitecity;

import net.darmo_creations.infinitecity.chunk_gen.*;
import net.fabricmc.api.*;
import net.minecraft.registry.*;
import net.minecraft.util.*;

public class InfiniteCity implements ModInitializer {
  public static final String MOD_ID = "infinitecity";

  @Override
  public void onInitialize() {
    Registry.register(Registries.CHUNK_GENERATOR, new Identifier(MOD_ID, "city"), InfiniteCityChunkGenerator.CODEC);
  }
}
