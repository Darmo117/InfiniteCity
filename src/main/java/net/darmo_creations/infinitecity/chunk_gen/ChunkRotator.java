package net.darmo_creations.infinitecity.chunk_gen;

import net.minecraft.block.*;
import net.minecraft.util.*;

import java.util.*;

public class ChunkRotator {
  public static BlockState[][][] rotateChunk(final BlockState[][][] blockStates, BlockRotation rotation) {
    final BlockState[][][] out = copy(blockStates, new BlockState[blockStates.length][16][16]);

    if (rotation == BlockRotation.NONE) return out;

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
   * Util methods (package access for unit tests)
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
