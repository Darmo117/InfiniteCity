package net.darmo_creations.infinitecity.chunk_gen;

import net.minecraft.util.*;

import java.util.*;

final class ChunkPortions {
  private static final Map<BlockRotation, ChunkPortion> COLUMN_CORNER = new HashMap<>();
  private static final Map<BlockRotation, Map<BlockMirror, ChunkPortion>> COLUMN_SIDE = new HashMap<>();

  static ChunkPortion getColumnCorner(BlockRotation rotation) {
    if (!COLUMN_CORNER.containsKey(BlockRotation.NONE))
      initDefaultColumnCorner();
    if (!COLUMN_CORNER.containsKey(rotation))
      COLUMN_CORNER.put(rotation, COLUMN_CORNER.get(BlockRotation.NONE).withRotation(rotation));
    return COLUMN_CORNER.get(rotation);
  }

  static ChunkPortion getColumnSide(BlockRotation rotation, BlockMirror mirror) {
    if (!COLUMN_SIDE.containsKey(BlockRotation.NONE))
      initDefaultColumnSide();
    if (!COLUMN_SIDE.containsKey(rotation))
      COLUMN_SIDE.put(rotation, new HashMap<>());
    final var forRotation = COLUMN_SIDE.get(rotation);
    if (!forRotation.containsKey(mirror)) {
      ChunkPortion defaultCP = COLUMN_SIDE.get(BlockRotation.NONE).get(BlockMirror.NONE);
      forRotation.put(mirror, defaultCP.withRotation(rotation).withMirror(mirror));
    }
    return forRotation.get(mirror);
  }

  private static void initDefaultColumnCorner() {
    final int height = InfiniteCityChunkGenerator.COLUMN_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);
    chunkPortion.fill(0, 16, 0, 16, 0, 16, InfiniteCityChunkGenerator.TERRAIN);
    chunkPortion.fill(0, 16, 0, 16, height - 16, height, InfiniteCityChunkGenerator.TERRAIN);
    chunkPortion.fill(0, 4, 0, 4, 16, height - 16, InfiniteCityChunkGenerator.TERRAIN);
    COLUMN_CORNER.put(BlockRotation.NONE, chunkPortion);
  }

  private static void initDefaultColumnSide() {
    COLUMN_SIDE.put(BlockRotation.NONE, new HashMap<>());
    final int height = InfiniteCityChunkGenerator.COLUMN_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);
    chunkPortion.fill(0, 8, 0, 8, 0, height, InfiniteCityChunkGenerator.TERRAIN);
    COLUMN_SIDE.get(BlockRotation.NONE).put(BlockMirror.NONE, chunkPortion);
  }

  private ChunkPortions() {
  }
}
