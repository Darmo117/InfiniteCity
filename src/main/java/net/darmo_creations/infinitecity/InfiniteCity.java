package net.darmo_creations.infinitecity;

import net.darmo_creations.infinitecity.blocks.*;
import net.darmo_creations.infinitecity.chunk_gen.*;
import net.fabricmc.api.*;
import net.kyrptonaught.customportalapi.api.*;
import net.kyrptonaught.customportalapi.portal.*;
import net.minecraft.block.*;
import net.minecraft.registry.*;
import net.minecraft.util.*;

public class InfiniteCity implements ModInitializer {
  public static final String MOD_ID = "infinitecity";

  @Override
  public void onInitialize() {
    Registry.register(Registries.CHUNK_GENERATOR, new Identifier(MOD_ID, "city"), InfiniteCityChunkGenerator.CODEC);
    ModBlocks.init();
    CustomPortalBuilder.beginPortal()
        .frameBlock(Blocks.LIGHT_GRAY_CONCRETE)
        .customIgnitionSource(PortalIgnitionSource.FIRE)
        .onlyLightInOverworld()
        .destDimID(new Identifier(MOD_ID, "city"))
        .tintColor(100, 100, 100)
        .registerPortal();
  }
}
