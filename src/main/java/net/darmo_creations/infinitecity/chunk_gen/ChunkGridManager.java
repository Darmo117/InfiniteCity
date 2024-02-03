package net.darmo_creations.infinitecity.chunk_gen;

import net.minecraft.util.*;
import net.minecraft.util.math.*;

import java.util.*;

public class ChunkGridManager {
  private final int blockSize;
  private final int blockSpacing;
  private final int offsetX;
  private final int offsetZ;
  private final boolean inverted;

  /**
   * Create a chunk grid manager.
   *
   * @param blockSize    The size of plain blocks in chunks.
   * @param blockSpacing The size of the space between blocks in chunks.
   * @param offsetX      The offset of the first block’s north-west corner from the 0 coordinate along the X axis.
   * @param offsetZ      The offset of the first block’s north-west corner from the 0 coordinate along the Z axis.
   * @param inverted     Whether to invert the filled/empty regions.
   */
  public ChunkGridManager(int blockSize, int blockSpacing, int offsetX, int offsetZ, boolean inverted) {
    this.blockSize = blockSize;
    this.blockSpacing = blockSpacing;
    this.offsetX = offsetX;
    this.offsetZ = offsetZ;
    this.inverted = inverted;
  }

  /**
   * Indicate whether the chunk at the given coordinates should be filled, based on this grid’s settings.
   *
   * @param chunkX Chunk’s X coord.
   * @param chunkZ Chunk’s Z coord.
   * @return True if the chunk should be filled, false if it should be empty.
   */
  public boolean shouldBeFilled(int chunkX, int chunkZ) {
    return this.inverted != (this.isInBlock(chunkX, this.offsetX) && this.isInBlock(chunkZ, this.offsetZ));
  }

  public Pair<Integer, Integer> getGridIJ(int chunkX, int chunkZ) {
    return new Pair<>(this.getGridI(chunkX, this.offsetX), this.getGridI(chunkZ, this.offsetZ));
  }

  public Optional<HoleDirection> isAtEdge(int chunkX, int chunkZ) {
    if (this.inverted) {
      final Optional<Direction.AxisDirection> pastX = this.isPastBlockEdge(chunkX, this.offsetX)
          .map(Direction.AxisDirection::getOpposite);
      final Optional<Direction.AxisDirection> pastZ = this.isPastBlockEdge(chunkZ, this.offsetZ)
          .map(Direction.AxisDirection::getOpposite);

      if (pastX.isPresent() && pastZ.isPresent())
        return Optional.of(HoleDirection.forDirections(pastX.get(), pastZ.get()));
      else if (pastX.isPresent() && !this.shouldBeFilled(chunkZ, this.offsetZ))
        return pastX.map(d -> HoleDirection.forAxisAndDirection(Direction.Axis.X, d));
      else if (pastZ.isPresent() && !this.shouldBeFilled(chunkX, this.offsetX))
        return pastZ.map(d -> HoleDirection.forAxisAndDirection(Direction.Axis.Z, d));

    } else {
      final Optional<Direction.AxisDirection> edgeX = this.isAtBlockEdge(chunkX, this.offsetX);
      final Optional<Direction.AxisDirection> edgeZ = this.isAtBlockEdge(chunkZ, this.offsetZ);

      if (edgeX.isPresent() && edgeZ.isPresent())
        return Optional.of(HoleDirection.forDirections(edgeX.get(), edgeZ.get()));
      else if (edgeX.isPresent() && this.shouldBeFilled(chunkZ, this.offsetZ))
        return edgeX.map(d -> HoleDirection.forAxisAndDirection(Direction.Axis.X, d));
      else if (edgeZ.isPresent() && this.shouldBeFilled(chunkX, this.offsetX))
        return edgeZ.map(d -> HoleDirection.forAxisAndDirection(Direction.Axis.Z, d));
    }

    return Optional.empty();
  }

  public Optional<HoleDirection> isPastEdge(int chunkX, int chunkZ) {
    if (this.inverted) {
      final Optional<Direction.AxisDirection> edgeX = this.isAtBlockEdge(chunkX, this.offsetX)
          .map(Direction.AxisDirection::getOpposite);
      final Optional<Direction.AxisDirection> edgeZ = this.isAtBlockEdge(chunkZ, this.offsetZ)
          .map(Direction.AxisDirection::getOpposite);

      if (edgeX.isPresent() && edgeZ.isPresent())
        return Optional.of(HoleDirection.forDirections(edgeX.get(), edgeZ.get()));
      else if (edgeX.isPresent() && !this.shouldBeFilled(chunkZ, this.offsetZ))
        return edgeX.map(d -> HoleDirection.forAxisAndDirection(Direction.Axis.X, d));
      else if (edgeZ.isPresent() && !this.shouldBeFilled(chunkX, this.offsetX))
        return edgeZ.map(d -> HoleDirection.forAxisAndDirection(Direction.Axis.Z, d));

    } else {
      final Optional<Direction.AxisDirection> pastX = this.isPastBlockEdge(chunkX, this.offsetX);
      final Optional<Direction.AxisDirection> pastZ = this.isPastBlockEdge(chunkZ, this.offsetZ);

      if (pastX.isPresent() && pastZ.isPresent())
        return Optional.of(HoleDirection.forDirections(pastX.get(), pastZ.get()));
      else if (pastX.isPresent() && this.shouldBeFilled(chunkZ, this.offsetZ))
        return pastX.map(d -> HoleDirection.forAxisAndDirection(Direction.Axis.X, d));
      else if (pastZ.isPresent() && this.shouldBeFilled(chunkX, this.offsetX))
        return pastZ.map(d -> HoleDirection.forAxisAndDirection(Direction.Axis.Z, d));
    }

    return Optional.empty();
  }

  private boolean isInBlock(int x, int offset) {
    final int i = this.getGridI(x, offset);
    return i < this.blockSize;
  }

  private Optional<Direction.AxisDirection> isAtBlockEdge(int x, int offset) {
    final int i = this.getGridI(x, offset);
    if (i == 0)
      return Optional.of(Direction.AxisDirection.NEGATIVE);
    else if (i == this.blockSize - 1)
      return Optional.of(Direction.AxisDirection.POSITIVE);
    else
      return Optional.empty();
  }

  private Optional<Direction.AxisDirection> isPastBlockEdge(int x, int offset) {
    final int i = this.getGridI(x, offset);
    if (i == this.blockSize)
      return Optional.of(Direction.AxisDirection.POSITIVE);
    else if (i == this.blockSize + this.blockSpacing - 1)
      return Optional.of(Direction.AxisDirection.NEGATIVE);
    else
      return Optional.empty();
  }

  private int getGridI(int x, int offset) {
    x -= offset;
    final int i;
    final int mod = this.blockSize + this.blockSpacing;
    if (x >= 0) {
      i = x % mod;
    } else {
      i = (mod - (-x) % mod) % mod;
    }
    return i;
  }

  public enum HoleDirection {
    NORTH,
    SOUTH,
    WEST,
    EAST,
    NORTH_WEST,
    NORTH_EAST,
    SOUTH_WEST,
    SOUTH_EAST;

    public static HoleDirection forAxisAndDirection(Direction.Axis axis, Direction.AxisDirection direction) {
      return switch (axis) {
        case X -> direction == Direction.AxisDirection.POSITIVE ? EAST : WEST;
        case Z -> direction == Direction.AxisDirection.POSITIVE ? SOUTH : NORTH;
        case Y -> throw new IllegalArgumentException("Invalid axis Y");
      };
    }

    public static HoleDirection forDirections(Direction.AxisDirection directionX, Direction.AxisDirection directionZ) {
      return switch (directionX) {
        case POSITIVE -> switch (directionZ) {
          case POSITIVE -> SOUTH_EAST;
          case NEGATIVE -> NORTH_EAST;
        };
        case NEGATIVE -> switch (directionZ) {
          case POSITIVE -> SOUTH_WEST;
          case NEGATIVE -> NORTH_WEST;
        };
      };
    }
  }
}
