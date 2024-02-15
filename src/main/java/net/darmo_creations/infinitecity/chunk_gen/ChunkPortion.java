package net.darmo_creations.infinitecity.chunk_gen;

import net.minecraft.block.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.*;

class ChunkPortion {
  private final int height;
  private final BlockState[][][] blockStates;

  public ChunkPortion(int height) {
    if (height <= 0) throw new IllegalArgumentException("height <= 0");
    this.height = height;
    this.blockStates = new BlockState[height][16][16];
  }

  private ChunkPortion(final BlockState[][][] blockStates) {
    this.height = blockStates.length;
    this.blockStates = blockStates;
  }

  public void fill(int fromX, int toX, int fromZ, int toZ, int fromY, int toY, BlockState blockState) {
    for (int y = fromY; y < toY; y++)
      for (int z = fromZ; z < toZ; z++)
        for (int x = fromX; x < toX; x++)
          this.blockStates[y][z][x] = blockState;
  }

  public void placeInWorld(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int bottomY) {
    for (int dy = 0; dy < this.height; dy++) {
      final int y = bottomY + dy;
      for (int dz = 0; dz < 16; dz++) {
        final int z = ChunkGenerationUtils.getHPos(chunkZ, dz);
        for (int dx = 0; dx < 16; dx++) {
          final int x = ChunkGenerationUtils.getHPos(chunkX, dx);
          final BlockState state = this.blockStates[dy][dz][dx];
          if (state != null)
            chunk.setBlockState(mutable.set(x, y, z), state, false);
        }
      }
    }
  }

  public ChunkPortion withRotation(BlockRotation rotation) {
    if (rotation == BlockRotation.NONE) return this;
    return new ChunkPortion(ChunkManipulator.rotateChunk(this.blockStates, rotation));
  }

  public ChunkPortion withMirror(BlockMirror mirror) {
    if (mirror == BlockMirror.NONE) return this;
    return new ChunkPortion(ChunkManipulator.mirrorChunk(this.blockStates, mirror));
  }
}
