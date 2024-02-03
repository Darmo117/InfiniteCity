package net.darmo_creations.infinitecity.client;

import net.darmo_creations.infinitecity.*;
import net.fabricmc.api.*;
import net.fabricmc.fabric.mixin.client.rendering.*;
import net.minecraft.util.*;

public class InfiniteCityClient implements ClientModInitializer {
  public static final Identifier INFINITE_CITY_DIMENSION_EFFECTS_KEY =
      new Identifier(InfiniteCity.MOD_ID, "city");

  @Override
  public void onInitializeClient() {
    // Inject custom dimension effects. Custom dimension and dimension type are added through datapack.
    //noinspection UnstableApiUsage
    DimensionEffectsAccessor.getIdentifierMap().put(
        INFINITE_CITY_DIMENSION_EFFECTS_KEY,
        new InfiniteCityDimensionEffects()
    );
  }
}
