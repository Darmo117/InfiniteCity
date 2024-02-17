package net.darmo_creations.infinitecity.chunk_gen;

import net.minecraft.block.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.*;

import java.util.*;

class ChunkPortion {
  /**
   * Optimization: only the array portions inside a each BBox will be iterated over
   * in {@link #placeInWorld(Chunk, BlockPos.Mutable, int, int, int)}.
   */
  private final ChunkLayerBBox[] bBoxes;
  private final BlockState[][][] blockStates;

  public ChunkPortion(int height) {
    if (height <= 0) throw new IllegalArgumentException("height <= 0");
    this.blockStates = new BlockState[height][16][16];
    this.bBoxes = new ChunkLayerBBox[height];
    for (int i = 0; i < height; i++)
      this.bBoxes[i] = new ChunkLayerBBox();
  }

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

  public void fill(int fromX, int toX, int fromZ, int toZ, int fromY, int toY, BlockState blockState) {
    Objects.requireNonNull(blockState);
    for (int y = fromY; y < toY; y++) {
      this.bBoxes[y].update(fromX, toX - 1, fromZ, toZ - 1);
      for (int z = fromZ; z < toZ; z++)
        for (int x = fromX; x < toX; x++)
          this.blockStates[y][z][x] = blockState;
    }
  }

  public void fillMirrorTop(int fromX, int toX, int fromZ, int toZ, int fromY, int toY, BlockState blockState) {
    this.fillMirrorTop(fromX, toX, fromZ, toZ, fromY, toY, blockState, blockState);
  }

  public void fillMirrorTop(int fromX, int toX, int fromZ, int toZ, int fromY, int toY, BlockState bottomBlockState, BlockState topBlockState) {
    final int height = this.blockStates.length;
    this.fill(fromX, toX, fromZ, toZ, fromY, toY, bottomBlockState);
    this.fill(fromX, toX, fromZ, toZ, height - toY, height - fromY, topBlockState);
  }

  public void setBlock(int x, int z, int y, BlockState blockState) {
    Objects.requireNonNull(blockState);
    this.bBoxes[y].update(x, z);
    this.blockStates[y][z][x] = blockState;
  }

  public void setBlockMirror(int x, int z, int y, BlockState bottomBlockState, BlockState topBlockState) {
    this.setBlock(x, z, y, bottomBlockState);
    this.setBlock(x, z, this.blockStates.length - y - 1, topBlockState);
  }

  public void placeInWorld(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int atY) {
    this.placeInWorld(chunk, mutable, chunkX, chunkZ, 0, 0, atY);
  }

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
