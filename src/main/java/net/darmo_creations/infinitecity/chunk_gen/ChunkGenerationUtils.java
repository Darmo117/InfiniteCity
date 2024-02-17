package net.darmo_creations.infinitecity.chunk_gen;

import net.minecraft.block.*;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.*;

/**
 * This class defines various methods to fill chunks.
 */
final class ChunkGenerationUtils {
  /**
   * Fill a chunk between two Y positions with the given block state.
   *
   * @param chunk      The chunk to fill.
   * @param mutable    A mutable {@link BlockPos} to use internally.
   * @param chunkX     The chunk’s X position.
   * @param chunkZ     The chunk’s Z position.
   * @param fromY      The lowest Y position to fill.
   * @param toY        The highest Y position to fill (exclusive).
   * @param blockState The block state to use as filler.
   */
  public static void fillChunk(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int fromY, int toY, BlockState blockState) {
    fill(chunk, mutable, chunkX, chunkZ, 0, 16, 0, 16, fromY, toY, blockState);
  }

  /**
   * Partially fill a chunk between two Y positions with the given block state.
   *
   * @param chunk      The chunk to fill.
   * @param mutable    A mutable {@link BlockPos} to use internally.
   * @param chunkX     The chunk’s X position.
   * @param chunkZ     The chunk’s Z position.
   * @param fromX      The smallest X chunk-relative position to fill.
   * @param toX        The biggest X chunk-relative position to fill (exclusive).
   * @param fromZ      The smallest Z chunk-relative position to fill.
   * @param toZ        The biggest Z chunk-relative position to fill (exclusive).
   * @param fromY      The lowest Y position to fill.
   * @param toY        The highest Y position to fill (exclusive).
   * @param blockState The block state to use as filler.
   */
  public static void fill(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int fromX, int toX, int fromZ, int toZ, int fromY, int toY, BlockState blockState) {
    for (int y = fromY; y < toY; y++) {
      for (int dx = fromX; dx < toX; dx++) {
        final int x = getHPos(chunkX, dx);
        for (int dz = fromZ; dz < toZ; dz++) {
          final int z = getHPos(chunkZ, dz);
          chunk.setBlockState(mutable.set(x, y, z), blockState, false);
        }
      }
    }
  }

  /**
   * Set the block at the given position.
   *
   * @param chunk      The chunk to fill.
   * @param mutable    A mutable {@link BlockPos} to use internally.
   * @param chunkX     The chunk’s X position.
   * @param chunkZ     The chunk’s Z position.
   * @param x          The block’s chunk-relative X position.
   * @param z          The block’s chunk-relative Z position.
   * @param y          The block’s Y position.
   * @param blockState The block state to use as filler.
   */
  public static void setBlock(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int x, int z, int y, BlockState blockState) {
    chunk.setBlockState(mutable.set(getHPos(chunkX, x), y, getHPos(chunkZ, z)), blockState, false);
  }

  /**
   * Get the horizontal absolute position of the given chunk-relative horizontal position.
   *
   * @param chunkPos         The chunk’s position.
   * @param chunkRelativePos The chunk-relative position.
   * @return The corresponding absolute position.
   */
  public static int getHPos(int chunkPos, int chunkRelativePos) {
    return ChunkSectionPos.getOffsetPos(chunkPos, chunkRelativePos);
  }

  private ChunkGenerationUtils() {
  }
}
