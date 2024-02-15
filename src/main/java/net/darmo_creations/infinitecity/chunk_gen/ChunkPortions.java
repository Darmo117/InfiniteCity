package net.darmo_creations.infinitecity.chunk_gen;

import net.minecraft.block.*;
import net.minecraft.block.enums.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;

import java.util.*;

import static net.darmo_creations.infinitecity.chunk_gen.ChunkGeneratorBlocks.*;

final class ChunkPortions {
  private static final Map<BlockRotation, ChunkPortion> COLUMN_CORNER = new HashMap<>();
  private static final Map<BlockRotation, Map<BlockMirror, ChunkPortion>> COLUMN_SIDE = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> FACADE_EDGES_CORNER = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> FACADE_EDGES_SIDE = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> INNER_RING_CORNER = new HashMap<>();
  private static final Map<BlockRotation, ChunkPortion> INNER_RING_SIDE = new HashMap<>();

  static ChunkPortion getColumnCorner(BlockRotation rotation) {
    return forRotation(COLUMN_CORNER, rotation, ChunkPortions::initDefaultColumnCorner);
  }

  static ChunkPortion getColumnSide(BlockRotation rotation, BlockMirror mirror) {
    if (!COLUMN_SIDE.containsKey(BlockRotation.NONE))
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

  static ChunkPortion getFacadeEdgesCorner(BlockRotation rotation) {
    return forRotation(FACADE_EDGES_CORNER, rotation, ChunkPortions::initDefaultFacadeEdgesCorner);
  }

  static ChunkPortion getFacadeEdgesSide(BlockRotation rotation) {
    return forRotation(FACADE_EDGES_SIDE, rotation, ChunkPortions::initDefaultFacadeEdgesSide);
  }

  static ChunkPortion getInnerRingCorner(BlockRotation rotation) {
    return forRotation(INNER_RING_CORNER, rotation, ChunkPortions::initDefaultInnerRingCorner);
  }

  static ChunkPortion getInnerRingSide(BlockRotation rotation) {
    return forRotation(INNER_RING_SIDE, rotation, ChunkPortions::initDefaultInnerRingSide);
  }

  private static ChunkPortion forRotation(Map<BlockRotation, ChunkPortion> map, BlockRotation rotation, Callback initDefault) {
    if (!map.containsKey(BlockRotation.NONE))
      initDefault.invoke();
    if (!map.containsKey(rotation))
      map.put(rotation, map.get(BlockRotation.NONE).withRotation(rotation));
    return map.get(rotation);
  }

  private static void initDefaultColumnCorner() {
    final int height = InfiniteCityChunkGenerator.COLUMN_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    chunkPortion.fillMirrorTop(0, 16, 0, 16, 0, 16, TERRAIN);
    chunkPortion.fillMirrorTop(0, 8, 0, 8, 16, 24, TERRAIN);

    chunkPortion.fill(0, 4, 0, 4, 24, height - 24, TERRAIN);

    COLUMN_CORNER.put(BlockRotation.NONE, chunkPortion);
  }

  private static void initDefaultColumnSide() {
    COLUMN_SIDE.put(BlockRotation.NONE, new HashMap<>());
    final int height = InfiniteCityChunkGenerator.COLUMN_HEIGHT;
    final ChunkPortion chunkPortion = new ChunkPortion(height);

    chunkPortion.fill(0, 8, 0, 8, 28, height - 28, TERRAIN);

    chunkPortion.fillMirrorTop(0, 12, 0, 16, 0, 20, TERRAIN);
    chunkPortion.fillMirrorTop(0, 10, 0, 16, 20, 28, TERRAIN);
    final BlockState stairsWest = STAIRS.with(StairsBlock.FACING, Direction.WEST);
    final BlockState stairsWestTop = stairsWest.with(StairsBlock.HALF, BlockHalf.TOP);
    chunkPortion.fillMirrorTop(11, 12, 0, 16, 19, 20, stairsWest, stairsWestTop);
    chunkPortion.fillMirrorTop(10, 11, 0, 16, 20, 21, stairsWest, stairsWestTop);

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

  @FunctionalInterface
  private interface Callback {
    void invoke();
  }

  private ChunkPortions() {
  }
}
