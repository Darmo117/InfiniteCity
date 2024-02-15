package net.darmo_creations.infinitecity.chunk_gen;

import net.minecraft.block.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.*;

final class ChunkGenerationUtils {
  static void fillChunk(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int bottomY, int topY, BlockState blockState) {
    fill(chunk, mutable, chunkX, chunkZ, 0, 16, 0, 16, bottomY, topY, blockState);
  }

  static void fill(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int fromX, int toX, int fromZ, int toZ, int bottomY, int topY, BlockState blockState) {
    for (int y = bottomY; y < topY; y++) {
      for (int dx = fromX; dx < toX; dx++) {
        final int x = getHPos(chunkX, dx);
        for (int dz = fromZ; dz < toZ; dz++) {
          final int z = getHPos(chunkZ, dz);
          chunk.setBlockState(mutable.set(x, y, z), blockState, false);
        }
      }
    }
  }

  static void setBlock(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int x, int z, int y, BlockState blockState) {
    chunk.setBlockState(mutable.set(getHPos(chunkX, x), y, getHPos(chunkZ, z)), blockState, false);
  }

  static void placeBlocks(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int bottomY, final BlockState[][][] blockStates, BlockRotation rotation, BlockMirror mirror) {
    final var transformed = ChunkManipulator.mirrorChunk(ChunkManipulator.rotateChunk(blockStates, rotation), mirror);
    for (int dy = 0; dy < transformed.length; dy++) {
      final int y = bottomY + dy;
      for (int dz = 0; dz < 16; dz++) {
        final int z = getHPos(chunkZ, dz);
        for (int dx = 0; dx < 16; dx++) {
          final int x = getHPos(chunkX, dx);
          final BlockState state = transformed[dy][dz][dx];
          if (state != null)
            chunk.setBlockState(mutable.set(x, y, z), state, false);
        }
      }
    }
  }

  static int getHPos(int chunkCoord, int d) {
    return ChunkSectionPos.getOffsetPos(chunkCoord, d);
  }

  private ChunkGenerationUtils() {
  }
}
