package net.darmo_creations.infinitecity.chunk_gen;

import net.minecraft.block.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.*;

import java.util.*;

/**
 * A chunk portion contains block states that may be placed in a world chunk.
 * It has a width of 16×16 blocks.
 * <p>
 * The x, y and z coordinates in the {@code setBlock*}/{@code fill*} methods
 * are relative to the chunk’s width and height.
 */
class ChunkPortion {
  /**
   * Optimization: only the array portions inside of each BBox will be iterated over
   * in {@link #placeInWorld(Chunk, BlockPos.Mutable, int, int, int)}.
   */
  private final ChunkLayerBBox[] bBoxes;
  private final BlockState[][][] blockStates;

  /**
   * Create an empty chunk portion of given height.
   *
   * @param height The portion’s height.
   * @throws IllegalArgumentException If the height is ≤ 0.
   */
  public ChunkPortion(int height) {
    if (height <= 0) throw new IllegalArgumentException("height <= 0");
    this.blockStates = new BlockState[height][16][16];
    this.bBoxes = new ChunkLayerBBox[height];
    for (int i = 0; i < height; i++)
      this.bBoxes[i] = new ChunkLayerBBox();
  }

  /**
   * Create a chunk portion for the given blockstates.
   *
   * @param blockStates The blockstates.
   */
  private ChunkPortion(final BlockState[][][] blockStates) {
    this.blockStates = blockStates;
    this.bBoxes = new ChunkLayerBBox[blockStates.length];
    for (int y = 0; y < blockStates.length; y++) {
      this.bBoxes[y] = new ChunkLayerBBox();
      for (int z = 0; z < 16; z++)
        for (int x = 0; x < 16; x++)
          if (blockStates[y][z][x] != null)
            this.bBoxes[y].update(x, z);
    }
  }

  /**
   * Fill the given area of this chunk portion.
   *
   * @param fromX      Min X.
   * @param toX        Max X (exclusive).
   * @param fromZ      Min Z.
   * @param toZ        Max Z (exclusive).
   * @param fromY      Min Y.
   * @param toY        Max Y (exclusive).
   * @param blockState The block state to use as filler.
   * @throws NullPointerException If {@code blocksState} is null.
   */
  public void fill(int fromX, int toX, int fromZ, int toZ, int fromY, int toY, BlockState blockState) {
    Objects.requireNonNull(blockState);
    for (int y = fromY; y < toY; y++) {
      this.bBoxes[y].update(fromX, toX - 1, fromZ, toZ - 1);
      for (int z = fromZ; z < toZ; z++)
        for (int x = fromX; x < toX; x++)
          this.blockStates[y][z][x] = blockState;
    }
  }

  /**
   * Fill the given area of this chunk portion and its mirror version starting from the top.
   *
   * @param fromX      Min X.
   * @param toX        Max X (exclusive).
   * @param fromZ      Min Z.
   * @param toZ        Max Z (exclusive).
   * @param fromY      Min bottom Y.
   * @param toY        Max bottom Y (exclusive).
   * @param blockState The block state to use as filler.
   * @throws NullPointerException If {@code blocksState} is null.
   */
  public void fillMirrorTop(int fromX, int toX, int fromZ, int toZ, int fromY, int toY, BlockState blockState) {
    this.fillMirrorTop(fromX, toX, fromZ, toZ, fromY, toY, blockState, blockState);
  }

  /**
   * Fill the given area of this chunk portion and its mirror version starting from the top.
   *
   * @param fromX            Min X.
   * @param toX              Max X (exclusive).
   * @param fromZ            Min Z.
   * @param toZ              Max Z (exclusive).
   * @param fromY            Min bottom Y.
   * @param toY              Max bottom Y (exclusive).
   * @param bottomBlockState The block state to use as filler for the bottom fill.
   * @param topBlockState    The block state to use as filler for the top fill.
   * @throws NullPointerException If either block state is null.
   */
  public void fillMirrorTop(int fromX, int toX, int fromZ, int toZ, int fromY, int toY, BlockState bottomBlockState, BlockState topBlockState) {
    final int height = this.blockStates.length;
    this.fill(fromX, toX, fromZ, toZ, fromY, toY, bottomBlockState);
    this.fill(fromX, toX, fromZ, toZ, height - toY, height - fromY, topBlockState);
  }

  /**
   * Set the block at the given position.
   *
   * @param x          Block’s X position.
   * @param z          Block’s Z position.
   * @param y          Block’s Y position.
   * @param blockState The block state to use as filler.
   * @throws NullPointerException If {@code blocksState} is null.
   */
  public void setBlock(int x, int z, int y, BlockState blockState) {
    Objects.requireNonNull(blockState);
    this.bBoxes[y].update(x, z);
    this.blockStates[y][z][x] = blockState;
  }

  /**
   * Set the block at the given position and its mirror version starting from the top.
   *
   * @param x                Block’s X position.
   * @param z                Block’s Z position.
   * @param y                Block’s Y position.
   * @param bottomBlockState The block state to use as filler for the bottom fill.
   * @param topBlockState    The block state to use as filler for the top fill.
   * @throws NullPointerException If either block state is null.
   */
  public void setBlockMirror(int x, int z, int y, BlockState bottomBlockState, BlockState topBlockState) {
    this.setBlock(x, z, y, bottomBlockState);
    this.setBlock(x, z, this.blockStates.length - y - 1, topBlockState);
  }

  /**
   * Place this chunk portion’s blocks in the given chunk, starting at the given Y position.
   *
   * @param chunk   The chunk to place blocks into.
   * @param mutable A mutable {@link BlockPos} object to use internally.
   * @param chunkX  The chunk’s X position.
   * @param chunkZ  The chunk’s Z position.
   * @param atY     The world Y position to start from.
   */
  public void placeInWorld(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int atY) {
    this.placeInWorld(chunk, mutable, chunkX, chunkZ, 0, 0, atY);
  }

  /**
   * Place this chunk portion’s blocks into the given chunk, starting at the given (X, Z, Y) position.
   * Any block that would end up outside of the chunk will be ignored.
   *
   * @param chunk   The chunk to place blocks into.
   * @param mutable A mutable {@link BlockPos} object to use internally.
   * @param chunkX  The chunk’s X position.
   * @param chunkZ  The chunk’s Z position.
   * @param atX     The chunk-relative X position corresponding to start from.
   * @param atZ     The chunk-relative Z position corresponding to start from.
   * @param atY     The world Y position to start from.
   */
  public void placeInWorld(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int atX, int atZ, int atY) {
    for (int dy = 0; dy < this.blockStates.length; dy++) {
      final ChunkLayerBBox bBox = this.bBoxes[dy];
      final int y = atY + dy;
      for (int dz = bBox.minZ(); dz <= bBox.maxZ(); dz++) {
        final int trueZ = atZ + dz;
        if (trueZ < 0 || trueZ >= 16) continue;
        final int z = ChunkGenerationUtils.getHPos(chunkZ, trueZ);
        for (int dx = bBox.minX(); dx <= bBox.maxX(); dx++) {
          final int trueX = atX + dx;
          if (trueX < 0 || trueX >= 16) continue;
          final int x = ChunkGenerationUtils.getHPos(chunkX, trueX);
          final BlockState state = this.blockStates[dy][dz][dx];
          if (state != null)
            chunk.setBlockState(mutable.set(x, y, z), state, false);
        }
      }
    }
  }

  /**
   * Return a rotated version of this portion.
   *
   * @param rotation The rotation amount.
   * @return A rotated copy of this portion, or this object if no rotation is applied.
   */
  public ChunkPortion withRotation(BlockRotation rotation) {
    if (rotation == BlockRotation.NONE) return this;
    return new ChunkPortion(rotateChunk(this.blockStates, rotation));
  }

  /**
   * Return a mirrored version of this portion.
   *
   * @param mirror The mirroring to apply.
   * @return A mirrored copy of this portion, or this object if no mirroring is applied.
   */
  public ChunkPortion withMirror(BlockMirror mirror) {
    if (mirror == BlockMirror.NONE) return this;
    return new ChunkPortion(mirrorChunk(this.blockStates, mirror));
  }

  /*
   * The following methods have package-only visibility instead of private to render them accessible to unit tests
   */

  /**
   * Return a rotated version of the given {@link BlockState} 3D-array.
   *
   * @param blockStates The array to mirror.
   * @param rotation    The rotation to apply.
   * @return A rotated copy of the array, or the array itself if no rotation is applied.
   */
  static BlockState[][][] rotateChunk(final BlockState[][][] blockStates, BlockRotation rotation) {
    if (rotation == BlockRotation.NONE) return blockStates;

    final BlockState[][][] out = copy(blockStates, new BlockState[blockStates.length][16][]);

    for (final var layer : out) {
      rotateLayer(layer, rotation);
      for (int x = 0; x < 16; x++)
        for (int z = 0; z < 16; z++)
          if (layer[z][x] != null)
            layer[z][x] = layer[z][x].rotate(rotation);
    }

    return out;
  }

  /**
   * Return a mirrored version of the given {@link BlockState} 3D-array.
   *
   * @param blockStates The array to mirror.
   * @param mirror      The mirroring to apply.
   * @return A mirrored copy of the array, or the array itself if no mirroring is applied.
   */
  static BlockState[][][] mirrorChunk(final BlockState[][][] blockStates, BlockMirror mirror) {
    if (mirror == BlockMirror.NONE) return blockStates;

    final BlockState[][][] out = copy(blockStates, new BlockState[blockStates.length][16][]);

    for (final var layer : out) {
      mirrorLayer(layer, mirror);
      for (int x = 0; x < 16; x++)
        for (int z = 0; z < 16; z++)
          if (layer[z][x] != null)
            layer[z][x] = layer[z][x].mirror(mirror);
    }

    return out;
  }

  /**
   * Rotate the given 2D-array.
   *
   * @param layer    The array to rotate.
   * @param rotation The rotation to apply.
   */
  static <T> void rotateLayer(T[][] layer, BlockRotation rotation) {
    switch (rotation) {
      case CLOCKWISE_90 -> {
        transpose(layer);
        reverseRows(layer);
      }
      case CLOCKWISE_180 -> {
        reverseRows(layer);
        reverseColumns(layer);
      }
      case COUNTERCLOCKWISE_90 -> {
        transpose(layer);
        reverseColumns(layer);
      }
    }
  }

  /**
   * Mirror the given 2D-array.
   *
   * @param layer  The array to mirror.
   * @param mirror The mirroring to apply.
   */
  static <T> void mirrorLayer(T[][] layer, BlockMirror mirror) {
    switch (mirror) {
      case LEFT_RIGHT -> reverseColumns(layer);
      case FRONT_BACK -> reverseRows(layer);
    }
  }

  /**
   * Reverse the rows of the given 2D-array.
   *
   * @param array The array to update.
   */
  static <T> void reverseRows(T[][] array) {
    final int length = 16;
    final int halfLength = length / 2;
    for (final var row : array) {
      for (int x = 0; x < halfLength; x++) {
        final var temp = row[x];
        final int x2 = length - x - 1;
        row[x] = row[x2];
        row[x2] = temp;
      }
    }
  }

  /**
   * Reverse the columns of the given 2D-array.
   *
   * @param array The array to update.
   */
  static <T> void reverseColumns(T[][] array) {
    final int length = 16;
    final int halfLength = length / 2;
    for (int x = 0; x < length; x++) {
      for (int z = 0; z < halfLength; z++) {
        final var temp = array[z][x];
        final int z2 = length - z - 1;
        array[z][x] = array[z2][x];
        array[z2][x] = temp;
      }
    }
  }

  /**
   * Transpose the given 2D-array.
   *
   * @param array The array to update.
   */
  static <T> void transpose(T[][] array) {
    final int length = 16;
    for (int z = 0; z < length; z++) {
      for (int x = z + 1; x < length; x++) {
        final var temp = array[z][x];
        array[z][x] = array[x][z];
        array[x][z] = temp;
      }
    }
  }

  /**
   * Copy the an array into the given one.
   *
   * @param source The array to copy values from.
   * @param dest   The array to copy values to.
   * @return The destination array.
   */
  static <T> T[][][] copy(final T[][][] source, T[][][] dest) {
    for (int y = 0; y < source.length; y++) {
      final int length = source[y].length;
      for (int z = 0; z < length; z++)
        dest[y][z] = Arrays.copyOf(source[y][z], length);
    }
    return dest;
  }
}
