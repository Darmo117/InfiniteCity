package net.darmo_creations.infinitecity.client;

import net.minecraft.client.render.*;
import net.minecraft.util.math.*;

/**
 * Custom effects for the custom "infinite_city" dimension defined in this mod’s datapack.
 * <p>
 * Features:
 * <li>No clouds</li>
 * <li>No skybox</li>
 * <li>Fog</li>
 * <li>No thick fog</li>
 */
public class InfiniteCityDimensionEffects extends DimensionEffects {
  public InfiniteCityDimensionEffects() {
    super(Float.NaN, false, SkyType.NONE, false, false);
  }

  @Override
  public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
    return color;
  }

  @Override
  public boolean useThickFog(int camX, int camY) {
    return false;
  }
}
