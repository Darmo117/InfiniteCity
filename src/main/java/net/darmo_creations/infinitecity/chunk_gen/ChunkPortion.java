package net.darmo_creations.infinitecity.chunk_gen;

import net.minecraft.block.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.*;

import java.util.*;

class ChunkPortion {
  // Bounding box enclosing for optimization:
  // Only the array portions inside this BBox will be iterated over and placed into the world.
  private int minX = 16;
  private int maxX = -1;
  private int minY = 16;
  private int maxY = -1;
  private int minZ = 16;
  private int maxZ = -1;

  private final BlockState[][][] blockStates;

  public ChunkPortion(int height) {
    if (height <= 0) throw new IllegalArgumentException("height <= 0");
    this.blockStates = new BlockState[height][16][16];
  }

  private ChunkPortion(final BlockState[][][] blockStates) {
    this.blockStates = blockStates;
    for (int y = 0; y < blockStates.length; y++) {
      for (int z = 0; z < 16; z++) {
        for (int x = 0; x < 16; x++) {
          if (blockStates[y][z][x] != null) {
            this.minX = Math.min(this.minX, x);
            this.maxX = Math.max(this.maxX, x);
            this.minY = Math.min(this.minY, y);
            this.maxY = Math.max(this.maxY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxZ = Math.max(this.maxZ, z);
          }
        }
      }
    }
  }

  public void fill(int fromX, int toX, int fromZ, int toZ, int fromY, int toY, BlockState blockState) {
    Objects.requireNonNull(blockState);
    this.minX = Math.min(this.minX, fromX);
    this.maxX = Math.max(this.maxX, toX - 1);
    this.minY = Math.min(this.minY, fromY);
    this.maxY = Math.max(this.maxY, toY - 1);
    this.minZ = Math.min(this.minZ, fromZ);
    this.maxZ = Math.max(this.maxZ, toZ - 1);
    for (int y = fromY; y < toY; y++)
      for (int z = fromZ; z < toZ; z++)
        for (int x = fromX; x < toX; x++)
          this.blockStates[y][z][x] = blockState;
  }

  public void placeInWorld(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int bottomY) {
    for (int dy = this.minY; dy <= this.maxY; dy++) {
      final int y = bottomY + dy;
      for (int dz = this.minZ; dz <= this.maxZ; dz++) {
        final int z = ChunkGenerationUtils.getHPos(chunkZ, dz);
        for (int dx = this.minX; dx <= this.maxX; dx++) {
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
    return new ChunkPortion(rotateChunk(this.blockStates, rotation));
  }

  public ChunkPortion withMirror(BlockMirror mirror) {
    if (mirror == BlockMirror.NONE) return this;
    return new ChunkPortion(mirrorChunk(this.blockStates, mirror));
  }

  /*
   * The following methods have package-only visibility instead of private for unit tests
   */

  static BlockState[][][] mirrorChunk(final BlockState[][][] blockStates, BlockMirror mirror) {
    if (mirror == BlockMirror.NONE) return blockStates;

    final BlockState[][][] out = copy(blockStates, new BlockState[blockStates.length][16][16]);

    for (final var layer : out) {
      mirrorLayer(layer, mirror);
      for (int x = 0; x < 16; x++)
        for (int z = 0; z < 16; z++)
          if (layer[z][x] != null)
            layer[z][x] = layer[z][x].mirror(mirror);
    }

    return out;
  }

  static BlockState[][][] rotateChunk(final BlockState[][][] blockStates, BlockRotation rotation) {
    if (rotation == BlockRotation.NONE) return blockStates;

    final BlockState[][][] out = copy(blockStates, new BlockState[blockStates.length][16][16]);

    for (final var layer : out) {
      rotateLayer(layer, rotation);
      for (int x = 0; x < 16; x++)
        for (int z = 0; z < 16; z++)
          if (layer[z][x] != null)
            layer[z][x] = layer[z][x].rotate(rotation);
    }

    return out;
  }

  /*
   * Util methods (non-private for unit tests)
   */

  static <T> void mirrorLayer(T[][] layer, BlockMirror mirror) {
    switch (mirror) {
      case LEFT_RIGHT -> reverseColumns(layer);
      case FRONT_BACK -> reverseRows(layer);
    }
  }

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

  static <T> void reverseRows(T[][] layer) {
    final int length = 16;
    final int halfLength = length / 2;
    for (final var row : layer) {
      for (int x = 0; x < halfLength; x++) {
        final var temp = row[x];
        final int x2 = length - x - 1;
        row[x] = row[x2];
        row[x2] = temp;
      }
    }
  }

  static <T> void reverseColumns(T[][] layer) {
    final int length = 16;
    final int halfLength = length / 2;
    for (int x = 0; x < length; x++) {
      for (int z = 0; z < halfLength; z++) {
        final var temp = layer[z][x];
        final int z2 = length - z - 1;
        layer[z][x] = layer[z2][x];
        layer[z2][x] = temp;
      }
    }
  }

  static <T> void transpose(T[][] layer) {
    final int length = 16;
    for (int z = 0; z < length; z++) {
      for (int x = z + 1; x < length; x++) {
        final var temp = layer[z][x];
        layer[z][x] = layer[x][z];
        layer[x][z] = temp;
      }
    }
  }

  static <T> T[][][] copy(final T[][][] source, T[][][] dest) {
    for (int y = 0; y < source.length; y++) {
      final int length = source[y].length;
      for (int z = 0; z < length; z++)
        dest[y][z] = Arrays.copyOf(source[y][z], length);
    }
    return dest;
  }
}
