package net.darmo_creations.infinitecity.chunk_gen;

import net.darmo_creations.infinitecity.blocks.*;
import net.minecraft.block.*;
import net.minecraft.block.enums.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;

import java.util.*;

import static net.darmo_creations.infinitecity.chunk_gen.ChunkGeneratorBlocks.*;

/**
 * The {@link ChunkPortion}s available for terrain generation.
 * <p>
 * All {@link ChunkPortion}s are directed towards the east (south-east for corners) by default,
 * i.e. when no rotation nor mirroring is applied.
 * <p>
 * {@link ChunkPortion}s for all rotations and mirrorings are lazily created and cached.
 *
 * @see InfiniteCityChunkGenerator
 */
final class ChunkPortions {
  // Chunk portion caches
  private static final Map<BlockRotation, ChunkPortion> COLUMN_CORNER = new HashMap<>();
  private static final Map<BlockRotation, Map<BlockMirror, ChunkPortion>> COLUMN_SIDE = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> FACADE_EDGES_CORNER = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> FACADE_EDGES_SIDE = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> INNER_RING_CORNER = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> INNER_RING_SIDE = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> DESERT_OUTER_EDGE_CORNER = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> DESERT_OUTER_EDGE_SIDE = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> DESERT_INNER_EDGE_CORNER = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> DESERT_INNER_EDGE_SIDE = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> SMALL_ANTENNA_HORIZ = new HashMap<>();

  /**
   * Return the column corner for the given rotation.
   */
  public static ChunkPortion getColumnCorner(BlockRotation rotation) {
    return forRotation(COLUMN_CORNER, rotation, ChunkPortions::initDefaultColumnCorner);
  }

  /**
   * Return the column side for the given rotation and mirroring.
   */
  public static ChunkPortion getColumnSide(BlockRotation rotation, BlockMirror mirror) {
    if (!COLUMN_SIDE.containsKey(BlockRotation.NONE)
        || !COLUMN_SIDE.get(BlockRotation.NONE).containsKey(BlockMirror.NONE))
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

  /**
   * Return the building facade edge corner (top + bottom) for the given rotation.
   */
  public static ChunkPortion getFacadeEdgesCorner(BlockRotation rotation) {
    return forRotation(FACADE_EDGES_CORNER, rotation, ChunkPortions::initDefaultFacadeEdgesCorner);
  }

  /**
   * Return the building facade edge side (top + bottom) for the given rotation.
   */
  public static ChunkPortion getFacadeEdgesSide(BlockRotation rotation) {
    return forRotation(FACADE_EDGES_SIDE, rotation, ChunkPortions::initDefaultFacadeEdgesSide);
  }

  /**
   * Return the hole inner ring corner for the given rotation.
   */
  public static ChunkPortion getInnerRingCorner(BlockRotation rotation) {
    return forRotation(INNER_RING_CORNER, rotation, ChunkPortions::initDefaultInnerRingCorner);
  }

  /**
   * Return the hole inner ring side for the given rotation.
   */
  public static ChunkPortion getInnerRingSide(BlockRotation rotation) {
    return forRotation(INNER_RING_SIDE, rotation, ChunkPortions::initDefaultInnerRingSide);
  }

  /**
   * Return the desert block outer edge corner for the given rotation.
   */
  public static ChunkPortion getDesertOuterEdgeCorner(BlockRotation rotation) {
    return forRotation(DESERT_OUTER_EDGE_CORNER, rotation, ChunkPortions::initDefaultDesertOuterEdgeCorner);
  }

  /**
   * Return the desert block outer edge side for the given rotation.
   */
  public static ChunkPortion getDesertOuterEdgeSide(BlockRotation rotation) {
    return forRotation(DESERT_OUTER_EDGE_SIDE, rotation, ChunkPortions::initDefaultDesertOuterEdgeSide);
  }

  /**
   * Return the desert block inner edge corner for the given rotation.
   */
  public static ChunkPortion getDesertInnerEdgeCorner(BlockRotation rotation) {
    return forRotation(DESERT_INNER_EDGE_CORNER, rotation, ChunkPortions::initDefaultDesertInnerEdgeCorner);
  }

  /**
   * Return the desert block inner edge side for the given rotation.
   */
  public static ChunkPortion getDesertInnerEdgeSide(BlockRotation rotation) {
    return forRotation(DESERT_INNER_EDGE_SIDE, rotation, ChunkPortions::initDefaultDesertInnerEdgeSide);
  }

  /**
   * Return the small horizontal antenna (3×3×16) for the given rotation.
   * The antenna is centered at (x = 8, z = 8) when no rotation is applied.
   */
  public static ChunkPortion getSmallHorizontalAntenna(BlockRotation rotation) {
    return forRotation(SMALL_ANTENNA_HORIZ, rotation, ChunkPortions::initSmallHorizontalAntenna);
  }

  /**
   * Return the {@link ChunkPortion} from the given map for a specific rotation,
   * calling {@code initDefault} first if it does not exist yet.
   * <p>
   * This method handles lazy creation and caching.
   *
   * @param map         The map to query the portion from.
   * @param rotation    The rotation to apply to the default portion.
   * @param initDefault A callback to invoke if the default portion has not been created yet.
   * @return The portion for the given rotation.
   */
  private static ChunkPortion forRotation(Map<BlockRotation, ChunkPortion> map, BlockRotation rotation, Callback initDefault) {
    if (!map.containsKey(BlockRotation.NONE))
      initDefault.invoke();
    if (!map.containsKey(rotation))
      map.put(rotation, map.get(BlockRotation.NONE).withRotation(rotation));
    return map.get(rotation);
  }

  /*
   * Methods that create the default chunk portions.
   */

  private static void initDefaultColumnCorner() {
    final int height = InfiniteCityChunkGenerator.COLUMN_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    chunkPortion.fillMirrorTop(0, 16, 0, 16, 0, 16, TERRAIN);
    chunkPortion.fillMirrorTop(0, 8, 0, 8, 16, 24, TERRAIN);

    chunkPortion.fill(0, 4, 0, 4, 24, height - 24, TERRAIN);

    COLUMN_CORNER.put(BlockRotation.NONE, chunkPortion);
  }

  private static void initDefaultColumnSide() {
    if (!COLUMN_SIDE.containsKey(BlockRotation.NONE))
      COLUMN_SIDE.put(BlockRotation.NONE, new HashMap<>());
    final int height = InfiniteCityChunkGenerator.COLUMN_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    chunkPortion.fillMirrorTop(0, 8, 0, 8, 28, height / 2, TERRAIN);
    chunkPortion.fillMirrorTop(0, 4, 9, 13, 36, height / 2, TERRAIN);
    chunkPortion.fillMirrorTop(0, 4, 14, 16, 36, height / 2, TERRAIN);

    chunkPortion.fillMirrorTop(0, 12, 0, 16, 0, 20, TERRAIN);
    chunkPortion.fillMirrorTop(0, 10, 0, 16, 20, 28, TERRAIN);
    chunkPortion.fillMirrorTop(0, 7, 8, 16, 28, 36, TERRAIN);

    final BlockState stairsWest = STAIRS.with(StairsBlock.FACING, Direction.WEST);
    final BlockState stairsWestTop = stairsWest.with(StairsBlock.HALF, BlockHalf.TOP);
    for (int i = 1; i < 15; i += 4) {
      chunkPortion.fillMirrorTop(12, 14, i, i + 2, 0, 16, TERRAIN);
      chunkPortion.fillMirrorTop(13, 14, i, i + 2, 15, 16, stairsWest, stairsWestTop);
      chunkPortion.fillMirrorTop(12, 13, i, i + 2, 16, 17, stairsWest, stairsWestTop);
    }

    chunkPortion.fillMirrorTop(11, 12, 0, 16, 19, 20, stairsWest, stairsWestTop);
    chunkPortion.fillMirrorTop(10, 11, 0, 16, 20, 21, stairsWest, stairsWestTop);

    chunkPortion.fillMirrorTop(9, 10, 0, 8, 28, 29, stairsWest, stairsWestTop);
    chunkPortion.fillMirrorTop(8, 9, 0, 8, 29, 30, stairsWest, stairsWestTop);
    chunkPortion.fillMirrorTop(8, 9, 0, 8, 28, 29, TERRAIN);

    COLUMN_SIDE.get(BlockRotation.NONE).put(BlockMirror.NONE, chunkPortion);
  }

  private static void initDefaultFacadeEdgesCorner() {
    final int height = InfiniteCityChunkGenerator.FACADE_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    final int edgeHeight = 8;
    final int width = 4;
    final int x0 = width - 1;
    final int z0 = width - 1;
    final BlockState stairsNorth = STAIRS.with(StairsBlock.FACING, Direction.NORTH);
    final BlockState stairsNorthTop = stairsNorth.with(StairsBlock.HALF, BlockHalf.TOP);
    final BlockState stairsWest = STAIRS.with(StairsBlock.FACING, Direction.WEST);
    final BlockState stairsWestTop = stairsWest.with(StairsBlock.HALF, BlockHalf.TOP);
    final BlockState stairsCorner = stairsWest.with(StairsBlock.SHAPE, StairShape.OUTER_RIGHT);
    final BlockState stairsCornerTop = stairsWestTop.with(StairsBlock.SHAPE, StairShape.OUTER_RIGHT);
    chunkPortion.fillMirrorTop(0, width, 0, width, 0, 8, TERRAIN);
    // South
    chunkPortion.fillMirrorTop(0, width, z0, z0 + 1, edgeHeight - 1, edgeHeight, stairsNorth, stairsNorthTop);
    chunkPortion.fillMirrorTop(0, width, z0, z0 + 1, 0, 1, stairsNorthTop, stairsNorth);
    // East
    chunkPortion.fillMirrorTop(x0, x0 + 1, 0, width, edgeHeight - 1, edgeHeight, stairsWest, stairsWestTop);
    chunkPortion.fillMirrorTop(x0, x0 + 1, 0, width, 0, 1, stairsWestTop, stairsWest);
    // Corner
    chunkPortion.setBlockMirror(x0, z0, edgeHeight - 1, stairsCorner, stairsCornerTop);
    chunkPortion.setBlockMirror(x0, z0, 0, stairsCornerTop, stairsCorner);

    FACADE_EDGES_CORNER.put(BlockRotation.NONE, chunkPortion);
  }

  private static void initDefaultFacadeEdgesSide() {
    final int height = InfiniteCityChunkGenerator.FACADE_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    final int edgeHeight = 8;
    final int width = 4;
    final int x0 = width - 1;
    final BlockState stairsWest = STAIRS.with(StairsBlock.FACING, Direction.WEST);
    final BlockState stairsWestTop = stairsWest.with(StairsBlock.HALF, BlockHalf.TOP);
    chunkPortion.fillMirrorTop(0, width, 0, 16, 0, edgeHeight, TERRAIN);
    chunkPortion.fillMirrorTop(x0, x0 + 1, 0, 16, edgeHeight - 1, edgeHeight, stairsWest, stairsWestTop);
    chunkPortion.fillMirrorTop(x0, x0 + 1, 0, 16, 0, 1, stairsWestTop, stairsWest);

    FACADE_EDGES_SIDE.put(BlockRotation.NONE, chunkPortion);
  }

  private static void initDefaultInnerRingCorner() {
    final int height = InfiniteCityChunkGenerator.INNER_RING_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    final int width = 5;
    final int cornerWidth = 8;
    final int x0 = width - 1;
    final int z0 = width - 1;
    final BlockState stairsNorth = STAIRS.with(StairsBlock.FACING, Direction.NORTH);
    final BlockState stairsNorthTop = stairsNorth.with(StairsBlock.HALF, BlockHalf.TOP);
    final BlockState stairsWest = STAIRS.with(StairsBlock.FACING, Direction.WEST);
    final BlockState stairsWestTop = stairsWest.with(StairsBlock.HALF, BlockHalf.TOP);
    chunkPortion.fill(0, cornerWidth, 0, cornerWidth, 0, height, TERRAIN);
    // South
    chunkPortion.fill(cornerWidth, 16, 0, width, 0, height, TERRAIN);
    chunkPortion.fill(cornerWidth, 16, z0, z0 + 1, height - 1, height, stairsNorth);
    chunkPortion.fill(cornerWidth, 16, z0, z0 + 1, 0, 1, stairsNorthTop);
    // East
    chunkPortion.fill(0, width, cornerWidth, 16, 0, height, TERRAIN);
    chunkPortion.fill(x0, x0 + 1, cornerWidth, 16, height - 1, height, stairsWest);
    chunkPortion.fill(x0, x0 + 1, cornerWidth, 16, 0, 1, stairsWestTop);

    INNER_RING_CORNER.put(BlockRotation.NONE, chunkPortion);
  }

  private static void initDefaultInnerRingSide() {
    final int height = InfiniteCityChunkGenerator.INNER_RING_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    final int width = 5;
    final int x0 = width - 1;
    final BlockState stairsWest = STAIRS.with(StairsBlock.FACING, Direction.WEST);
    final BlockState stairsWestTop = stairsWest.with(StairsBlock.HALF, BlockHalf.TOP);
    chunkPortion.fill(0, width, 0, 16, 0, height, TERRAIN);
    chunkPortion.fill(x0, x0 + 1, 0, 16, height - 1, height, stairsWest);
    chunkPortion.fill(x0, x0 + 1, 0, 16, 0, 1, stairsWestTop);

    INNER_RING_SIDE.put(BlockRotation.NONE, chunkPortion);
  }

  private static final int DESERT_EDGE_BLOCK_HALF_SIZE = 2;
  private static final int DESERT_BLOCK_HEIGHT =
      InfiniteCityChunkGenerator.DESERT_BLOCK_HEIGHT
          + InfiniteCityChunkGenerator.DESERT_BLOCK_EDGE_HEIGHT
          + DESERT_EDGE_BLOCK_HALF_SIZE
          + 1;

  private static void initDefaultDesertOuterEdgeCorner() {
    final int h = DESERT_EDGE_BLOCK_HALF_SIZE;
    final int height = DESERT_BLOCK_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    final int smallBlockBottomY = height - 2 * h;
    chunkPortion.fill(0, 1, 0, 1, 0, smallBlockBottomY, TERRAIN);
    chunkPortion.fill(0, h, 0, h, smallBlockBottomY, height, TERRAIN);

    DESERT_OUTER_EDGE_CORNER.put(BlockRotation.NONE, chunkPortion);
  }

  private static void initDefaultDesertOuterEdgeSide() {
    final int h = DESERT_EDGE_BLOCK_HALF_SIZE;
    final int height = DESERT_BLOCK_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    final int smallBlockBottomY = height - 2 * h;
    chunkPortion.fill(0, 1, 0, 16, 0, 2, TERRAIN);
    chunkPortion.fill(0, 1, 0, 1, 2, smallBlockBottomY, TERRAIN);
    chunkPortion.fill(0, 1, 15, 16, 2, smallBlockBottomY, TERRAIN);
    chunkPortion.fill(0, h, 0, h, smallBlockBottomY, height, TERRAIN);
    chunkPortion.fill(0, h, 16 - h, 16, smallBlockBottomY, height, TERRAIN);

    DESERT_OUTER_EDGE_SIDE.put(BlockRotation.NONE, chunkPortion);
  }

  private static void initDefaultDesertInnerEdgeCorner() {
    final int h = DESERT_EDGE_BLOCK_HALF_SIZE;
    final int height = DESERT_BLOCK_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    final int smallBlockBottomY = height - 2 * h;
    chunkPortion.fill(0, 16, 15, 16, 0, 1, TERRAIN);
    chunkPortion.fill(15, 16, 0, 16, 0, 1, TERRAIN);
    chunkPortion.fill(16 - h, 16, 0, h, smallBlockBottomY, height, TERRAIN);
    chunkPortion.fill(16 - h, 16, 16 - h, 16, smallBlockBottomY, height, TERRAIN);
    chunkPortion.fill(0, h, 16 - h, 16, smallBlockBottomY, height, TERRAIN);

    DESERT_INNER_EDGE_CORNER.put(BlockRotation.NONE, chunkPortion);
  }

  private static void initDefaultDesertInnerEdgeSide() {
    final int h = DESERT_EDGE_BLOCK_HALF_SIZE;
    final int height = DESERT_BLOCK_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    final int smallBlockBottomY = height - 2 * h;
    chunkPortion.fill(15, 16, 0, 16, 0, 1, TERRAIN);
    chunkPortion.fill(16 - h, 16, 0, h, smallBlockBottomY, height, TERRAIN);
    chunkPortion.fill(16 - h, 16, 16 - h, 16, smallBlockBottomY, height, TERRAIN);

    DESERT_INNER_EDGE_SIDE.put(BlockRotation.NONE, chunkPortion);
  }

  private static void initSmallHorizontalAntenna() {
    final ChunkPortion chunkPortion = new ChunkPortion(3);

    final BlockState stairsWest = STAIRS.with(StairsBlock.FACING, Direction.WEST);
    final BlockState composite = COMPOSITE.with(CompositeBlock.NORTH_WEST_BOTTOM, true)
        .with(CompositeBlock.NORTH_WEST_TOP, true)
        .with(CompositeBlock.NORTH_EAST_BOTTOM, true)
        .with(CompositeBlock.NORTH_EAST_TOP, true)
        .with(CompositeBlock.SOUTH_WEST_BOTTOM, true)
        .with(CompositeBlock.SOUTH_WEST_TOP, true);
    final BlockState thickPostH = THICK_POST.with(ThickPostBlock.AXIS, Direction.Axis.X);
    final BlockState postH = POST.with(PostBlock.AXIS, Direction.Axis.X);
    chunkPortion.setBlock(0, 8, 0, stairsWest.with(StairsBlock.HALF, BlockHalf.TOP));
    chunkPortion.setBlock(0, 8, 2, stairsWest);
    chunkPortion.setBlock(0, 7, 1, composite.mirror(BlockMirror.LEFT_RIGHT));
    chunkPortion.setBlock(0, 9, 1, composite);
    chunkPortion.fill(0, 2, 8, 9, 1, 2, TERRAIN);
    chunkPortion.fill(2, 9, 8, 9, 1, 2, thickPostH);
    chunkPortion.fill(9, 16, 8, 9, 1, 2, postH);

    SMALL_ANTENNA_HORIZ.put(BlockRotation.NONE, chunkPortion);
  }

  /**
   * A callback takes no argument and returns nothing.
   */
  @FunctionalInterface
  private interface Callback {
    /**
     * Invoke this callback.
     */
    void invoke();
  }

  private ChunkPortions() {
  }
}
