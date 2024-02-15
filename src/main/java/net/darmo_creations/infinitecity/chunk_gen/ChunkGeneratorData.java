package net.darmo_creations.infinitecity.chunk_gen;

import net.minecraft.block.*;

final class ChunkGeneratorData {
  private static BlockState[][][] COLUMN_CORNER;
  private static BlockState[][][] COLUMN_SIDE;

  static BlockState[][][] getColumnCorner() {
    if (COLUMN_CORNER == null) {
      final int height = InfiniteCityChunkGenerator.COLUMN_HEIGHT;
      COLUMN_CORNER = new BlockState[height][16][16];
      fill(COLUMN_CORNER, 0, 16, 0, 16, 0, 16, InfiniteCityChunkGenerator.TERRAIN);
      fill(COLUMN_CORNER, 0, 16, 0, 16, height - 16, height, InfiniteCityChunkGenerator.TERRAIN);
      fill(COLUMN_CORNER, 0, 4, 0, 4, 16, height - 16, InfiniteCityChunkGenerator.TERRAIN);
    }
    return COLUMN_CORNER;
  }

  static BlockState[][][] getColumnSide() {
    if (COLUMN_SIDE == null) {
      final int height = InfiniteCityChunkGenerator.COLUMN_HEIGHT;
      COLUMN_SIDE = new BlockState[height][16][16];
      fill(COLUMN_SIDE, 0, 8, 0, 8, 0, height, InfiniteCityChunkGenerator.TERRAIN);
    }
    return COLUMN_SIDE;
  }

  private static void fill(BlockState[][][] states, int fromX, int toX, int fromZ, int toZ, int bottomY, int topY, BlockState blockState) {
    for (int y = bottomY; y < topY; y++)
      for (int z = fromZ; z < toZ; z++)
        for (int x = fromX; x < toX; x++)
          states[y][z][x] = blockState;
  }

  private ChunkGeneratorData() {
  }
}
