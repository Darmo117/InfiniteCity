package net.darmo_creations.infinitecity.blocks;

import net.minecraft.util.*;
import net.minecraft.util.math.*;

import java.util.*;

/**
 * Types of vertical slabs.
 */
public enum VerticalSlabType implements StringIdentifiable {
  NORTH("north", Direction.NORTH),
  SOUTH("south", Direction.SOUTH),
  EAST("east", Direction.EAST),
  WEST("west", Direction.WEST),
  DOUBLE("double", null);

  private final String name;
  private final Direction direction;

  VerticalSlabType(final String name, Direction direction) {
    this.name = name;
    this.direction = direction;
  }

  /**
   * Returns the direction associated to this type.
   */
  public Optional<Direction> getDirection() {
    return Optional.ofNullable(this.direction);
  }

  /**
   * Applies the given rotation to this slab type.
   *
   * @param rotation Rotation to apply.
   * @return Resulting slab type.
   */
  public VerticalSlabType rotate(BlockRotation rotation) {
    if (this.direction == null) {
      return this;
    }
    return forDirection(rotation.rotate(this.direction));
  }

  /**
   * Applies the given mirror transformation to this slab type.
   *
   * @param mirror Mirror transformation to apply.
   * @return Resulting slab type.
   */
  public VerticalSlabType mirror(BlockMirror mirror) {
    if (this.direction == null) {
      return this;
    }
    return forDirection(mirror.apply(this.direction));
  }

  @Override
  public String asString() {
    return this.name;
  }

  @Override
  public String toString() {
    return this.asString();
  }

  /**
   * Returns the slab type for the given horizontal direction.
   *
   * @param direction A horizontal direction.
   * @return The corresponding slab type or null if the direction is vertical.
   */
  public static VerticalSlabType forDirection(Direction direction) {
    return switch (direction) {
      case NORTH -> NORTH;
      case SOUTH -> SOUTH;
      case WEST -> WEST;
      case EAST -> EAST;
      default -> null;
    };
  }
}
