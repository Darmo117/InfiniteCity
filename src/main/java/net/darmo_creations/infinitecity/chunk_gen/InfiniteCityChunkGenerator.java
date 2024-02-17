package net.darmo_creations.infinitecity.chunk_gen;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.*;
import net.darmo_creations.infinitecity.mixins.*;
import net.minecraft.block.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.noise.*;
import net.minecraft.util.math.random.*;
import net.minecraft.world.*;
import net.minecraft.world.biome.source.*;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static net.darmo_creations.infinitecity.chunk_gen.ChunkGenerationUtils.*;
import static net.darmo_creations.infinitecity.chunk_gen.ChunkGeneratorBlocks.*;
import static net.darmo_creations.infinitecity.chunk_gen.ChunkPortions.*;

/**
 * Notations:
 * <ul>
 *  <li>[min layer-max layer]
 *  <li>(block chunk-wise width / block chunk-wise spacing)
 * </ul>
 * Layers: TODO update
 * <ol>
 *  <li>[0] Single layer of bedrock
 *  <li>[1-3] Thin layer of terrain blocks with small random elevation changes
 *  <li>[3-200] Empty space with various structures, buildings, bridges, etc.
 *      that stretch from the previous layer up to the next
 *  <li>[200-400] On-grid floating blocks (16 / 4)
 *  <li>[400-550] Empty space with sand deserts on top of the previous layer
 *  <li>[550-750] Random floating blocks (base unit: 4 / 4) whose sides ares facades with windows
 *  <li>[750-814] Empty space with various structures along with columns that support the blocks above
 *  <li>[814-1014] Plain layer with on-grid vertical holes (6 / 14), centered above the blocks of layer 4
 *  <li>[1014-1114] Empty space
 *  <li>[1114-1328] Random floating blocks (base unit: 8 / random) with hanging catwalks, antennas, grate, doors, etc.
 *      on the bottom and sides
 * </ol>
 */
public class InfiniteCityChunkGenerator extends ChunkGenerator {
  public static final Codec<InfiniteCityChunkGenerator> CODEC = RecordCodecBuilder.create(
      instance -> instance
          .group(InfiniteCityChunkGeneratorConfig.CODEC.fieldOf("settings")
              .forGetter(InfiniteCityChunkGenerator::getConfig))
          .apply(instance, instance.stable(InfiniteCityChunkGenerator::new))
  );

  public static final int FACADE_HEIGHT = 401;
  public static final int COLUMN_HEIGHT = 128;
  public static final int INNER_RING_HEIGHT = 8;
  public static final int DESERT_BLOCK_HEIGHT = 200;
  public static final int DESERT_BLOCK_EDGE_HEIGHT = 8;

  public static final int LAYER_1 = -2032; // Bedrock
  public static final int LAYER_2 = LAYER_1 + 1; // Thin terrain layer
  public static final int LAYER_3 = LAYER_2 + 1; // Empty space with columns, bridges, arches, etc.
  public static final int LAYER_4 = LAYER_3 + 200; // Plain layer with concentric hollow rings with suspended bridges spanning the gap
  public static final int LAYER_5 = LAYER_4 + 400; // ?
  public static final int LAYER_6 = LAYER_5 + 200; // On-grid blocks with windowed facades
  public static final int LAYER_7 = LAYER_6 + FACADE_HEIGHT; // Empty space with structures, hanging walkways and columns around holes of next layer
  public static final int LAYER_8 = LAYER_7 + COLUMN_HEIGHT; // Plain layer with on-grid square holes
  public static final int LAYER_9 = LAYER_8 + 412; // Empty space with ?
  public static final int LAYER_10 = LAYER_9 + COLUMN_HEIGHT; // On-grid blocks
  public static final int LAYER_11 = LAYER_10 + DESERT_BLOCK_HEIGHT; // Empty space with desert landscapes atop blocks of previous layer
  public static final int TOP = 2032; // Max allowed value
  public static final int WORLD_HEIGHT = TOP - LAYER_1;

  private static final ChunkCirclesManager LAYER_4_CIRCLE_MANAGER = new ChunkCirclesManager(20, 50, 0, 0);
  private static final ChunkGridManager LAYER_6_GRID_MANAGER = new ChunkGridManager(14, 4, 0, 0, false);
  private static final List<ChunkGridManager> LAYER_8_GRID_MANAGERS = List.of(
      new ChunkGridManager(8, 28, 12, 12, true),
      new ChunkGridManager(8, 28, -6, -6, true)
  );
  private static final List<ChunkGridManager> COLUMNS_GRID_MANAGERS = List.of(
      new ChunkGridManager(2, 10, 9, 9, false),
      new ChunkGridManager(2, 10, -9, -9, false)
  );
  private static final ChunkGridManager LAYER_10_GRID_MANAGER = new ChunkGridManager(32, 4, 0, 0, false);

  private final InfiniteCityChunkGeneratorConfig config;

  public InfiniteCityChunkGenerator(InfiniteCityChunkGeneratorConfig config) {
    super(new FixedBiomeSource(config.biome()));
    this.config = config;
  }

  public InfiniteCityChunkGeneratorConfig getConfig() {
    return this.config;
  }

  /**
   * Generate the base shape of the chunk out of the basic
   * block states as decided by this chunk generator’s config.
   */
  @Override
  public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
    return CompletableFuture.supplyAsync(Util.debugSupplier(
        "wgen_fill_noise",
        () -> populateNoise(chunk, structureAccessor)
    ), Util.getMainWorkerExecutor());
  }

  private static Chunk populateNoise(Chunk chunk, StructureAccessor structureAccessor) {
    final var mutable = new BlockPos.Mutable();
    final ChunkPos chunkPos = chunk.getPos();
    final int chunkX = chunkPos.x;
    final int chunkZ = chunkPos.z;
    generateBedrockLayer(chunk, mutable, chunkX, chunkZ);
    generateBottomLayer(chunk, mutable, chunkX, chunkZ);
    generateCirclesLayer(chunk, mutable, chunkX, chunkZ);
    generateBuildingsLayer(chunk, mutable, chunkX, chunkZ, structureAccessor);
    generateColumnsAroundHoles(chunk, mutable, chunkX, chunkZ, LAYER_7, LAYER_8);
    generateLayerWithHoles(chunk, mutable, chunkX, chunkZ);
    generateColumnsAroundHoles(chunk, mutable, chunkX, chunkZ, LAYER_9, LAYER_10);
    generateBigBlocksAndDesertLayer(chunk, mutable, chunkX, chunkZ, structureAccessor);
    return chunk;
  }

  private static void generateBedrockLayer(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ) {
    fillChunk(chunk, mutable, chunkX, chunkZ, LAYER_1, LAYER_2, BEDROCK);
  }

  private static void generateBottomLayer(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ) {
    fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_2, LAYER_3);
  }

  private static void generateBigBlocksAndDesertLayer(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, StructureAccessor structureAccessor) {
    if (LAYER_10_GRID_MANAGER.shouldBeFilled(chunkX, chunkZ)) {
      fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_10, LAYER_11);
      final var atEdge = LAYER_10_GRID_MANAGER.isAtEdge(chunkX, chunkZ);
      if (atEdge.isPresent()) {
        fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_11, LAYER_11 + DESERT_BLOCK_EDGE_HEIGHT); // Desert edge
        generateDesertEgdePillars(chunk, mutable, chunkX, chunkZ);
        generateBigBlocksInnerEdges(chunk, mutable, chunkX, chunkZ, atEdge.get());
      } else {
        generateDunes(chunk, mutable, chunkX, chunkZ, structureAccessor);
        erodeDunesNearEdge(chunk, mutable, chunkX, chunkZ);
      }
    } else
      LAYER_10_GRID_MANAGER.isPastEdge(chunkX, chunkZ)
          .ifPresent(d -> generateBigBlocksOuterEdges(chunk, mutable, chunkX, chunkZ, d));
  }

  private static void generateDesertEgdePillars(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ) {
    int y = LAYER_11 + DESERT_BLOCK_EDGE_HEIGHT;
    fill(chunk, mutable, chunkX, chunkZ, 6, 10, 6, 10, y, y += 4, TERRAIN);
    fill(chunk, mutable, chunkX, chunkZ, 5, 11, 5, 11, y, y += 6, TERRAIN);
    fill(chunk, mutable, chunkX, chunkZ, 4, 12, 4, 12, y, y + 8, TERRAIN);
  }

  private static void generateBigBlocksInnerEdges(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, ChunkGridManager.HoleDirection holeDirection) {
    final int y = LAYER_10 - 1;
    switch (holeDirection) {
      case NORTH -> getDesertInnerEdgeSide(BlockRotation.COUNTERCLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case SOUTH -> getDesertInnerEdgeSide(BlockRotation.CLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case WEST -> getDesertInnerEdgeSide(BlockRotation.CLOCKWISE_180)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case EAST -> getDesertInnerEdgeSide(BlockRotation.NONE)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case NORTH_WEST -> getDesertInnerEdgeCorner(BlockRotation.CLOCKWISE_180)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case NORTH_EAST -> getDesertInnerEdgeCorner(BlockRotation.COUNTERCLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case SOUTH_WEST -> getDesertInnerEdgeCorner(BlockRotation.CLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case SOUTH_EAST -> getDesertInnerEdgeCorner(BlockRotation.NONE)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);
    }
  }

  private static void generateBigBlocksOuterEdges(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, ChunkGridManager.HoleDirection holeDirection) {
    final int y = LAYER_10 - 1;
    switch (holeDirection) {
      case NORTH -> getDesertOuterEdgeSide(BlockRotation.COUNTERCLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case SOUTH -> getDesertOuterEdgeSide(BlockRotation.CLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case WEST -> getDesertOuterEdgeSide(BlockRotation.CLOCKWISE_180)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case EAST -> getDesertOuterEdgeSide(BlockRotation.NONE)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case NORTH_WEST -> getDesertOuterEdgeCorner(BlockRotation.CLOCKWISE_180)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case NORTH_EAST -> getDesertOuterEdgeCorner(BlockRotation.COUNTERCLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case SOUTH_WEST -> getDesertOuterEdgeCorner(BlockRotation.CLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

      case SOUTH_EAST -> getDesertOuterEdgeCorner(BlockRotation.NONE)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, y);
    }
  }

  private static void generateDunes(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, StructureAccessor structureAccessor) {
    final var sampler = DoublePerlinNoiseSampler.create(getRandom(structureAccessor), -6, 1.0, 0.5);
    for (int dx = 0; dx < 16; dx++) {
      final int x = getHPos(chunkX, dx);
      for (int dz = 0; dz < 16; dz++) {
        final int z = getHPos(chunkZ, dz);
        final double sample = (sampler.sample(x, LAYER_11, z) + 1) * 10;
        for (int dy = 0; dy < sample; dy++) {
          chunk.setBlockState(mutable.set(x, LAYER_11 + dy, z), SAND, false);
        }
      }
    }
  }

  private static void erodeDunesNearEdge(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ) {
    final int edgeTop = LAYER_11 + DESERT_BLOCK_EDGE_HEIGHT;
    final int erosionHeight = 10;
    if (LAYER_10_GRID_MANAGER.isAtEdge(chunkX - 1, chunkZ).isPresent()) {
      for (int i = 0; i < erosionHeight; i++)
        fill(chunk, mutable, chunkX, chunkZ, i, i + 1, 0, 16, edgeTop + i, edgeTop + 16, AIR);
    } else if (LAYER_10_GRID_MANAGER.isAtEdge(chunkX + 1, chunkZ).isPresent()) {
      for (int i = 0; i < erosionHeight; i++)
        fill(chunk, mutable, chunkX, chunkZ, 15 - i, 16 - i, 0, 16, edgeTop + i, edgeTop + 16, AIR);
    }

    if (LAYER_10_GRID_MANAGER.isAtEdge(chunkX, chunkZ - 1).isPresent()) {
      for (int i = 0; i < erosionHeight; i++)
        fill(chunk, mutable, chunkX, chunkZ, 0, 16, i, i + 1, edgeTop + i, edgeTop + 16, AIR);
    } else if (LAYER_10_GRID_MANAGER.isAtEdge(chunkX, chunkZ + 1).isPresent()) {
      for (int i = 0; i < erosionHeight; i++)
        fill(chunk, mutable, chunkX, chunkZ, 0, 16, 15 - i, 16 - i, edgeTop + i, edgeTop + 16, AIR);
    }
  }

  private static void generateCirclesLayer(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ) {
    if (LAYER_4_CIRCLE_MANAGER.shouldBeFilled(chunkX, chunkZ))
      fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_4, LAYER_5);
    else
      fill(chunk, mutable, chunkX, chunkZ, 0, 16, 0, 16, LAYER_5 - 32, LAYER_5, TERRAIN);
  }

  private static void generateBuildingsLayer(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, StructureAccessor structureAccessor) {
    if (LAYER_6_GRID_MANAGER.shouldBeFilled(chunkX, chunkZ)) {
      fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_6, LAYER_7);
      LAYER_6_GRID_MANAGER.isAtEdge(chunkX, chunkZ).ifPresent(
          d -> generateBuildingFacade(chunk, mutable, chunkX, chunkZ, d, structureAccessor));
    } else {
      LAYER_6_GRID_MANAGER.isPastEdge(chunkX, chunkZ).ifPresent(
          d -> generateBuildingFacadeEdge(chunk, mutable, chunkX, chunkZ, d));
    }
  }

  private static void generateBuildingFacade(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, ChunkGridManager.HoleDirection holeDirection, StructureAccessor structureAccessor) {
    final var sampler = DoublePerlinNoiseSampler.create(getRandom(structureAccessor), 0, 1.0);
    final int edgeHeight = 8;
    final int topY = LAYER_7 - edgeHeight;
    final int bottomY = LAYER_6 + edgeHeight;
    final Function<Double, BlockState> getBlockState = sample -> sample > 0.25 ? LIGHT_BLOCK : BLACK;
    for (int y = topY; y - 3 > bottomY; y -= 3) {
      for (int d = 1; d < 15; d += 2) {
        if (holeDirection.faces(Direction.NORTH) || holeDirection.faces(Direction.SOUTH)) {
          final int dz = holeDirection.faces(Direction.NORTH) ? 0 : 15;
          setBlock(chunk, mutable, chunkX, chunkZ, d, dz, y - 2, GLASS_PANE_X);
          setBlock(chunk, mutable, chunkX, chunkZ, d, dz, y - 3, GLASS_PANE_X);
          final int dz1 = dz + (holeDirection.faces(Direction.NORTH) ? 1 : -1);
          final double sample = sampler.sample(getHPos(chunkX, d), y, getHPos(chunkZ, dz1));
          final BlockState blockState = getBlockState.apply(sample);
          setBlock(chunk, mutable, chunkX, chunkZ, d, dz1, y - 2, blockState);
          setBlock(chunk, mutable, chunkX, chunkZ, d, dz1, y - 3, blockState);
        }
        if (holeDirection.faces(Direction.WEST) || holeDirection.faces(Direction.EAST)) {
          final int dx = holeDirection.faces(Direction.WEST) ? 0 : 15;
          setBlock(chunk, mutable, chunkX, chunkZ, dx, d, y - 2, GLASS_PANE_Z);
          setBlock(chunk, mutable, chunkX, chunkZ, dx, d, y - 3, GLASS_PANE_Z);
          final int dx1 = dx + (holeDirection.faces(Direction.WEST) ? 1 : -1);
          final double sample = sampler.sample(getHPos(chunkX, dx1), y, getHPos(chunkZ, d));
          final BlockState blockState = getBlockState.apply(sample);
          setBlock(chunk, mutable, chunkX, chunkZ, dx1, d, y - 2, blockState);
          setBlock(chunk, mutable, chunkX, chunkZ, dx1, d, y - 3, blockState);
        }
        if (d == 5) d += 3; // Leave 4-block empty space at middle
      }
    }
  }

  private static void generateBuildingFacadeEdge(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, ChunkGridManager.HoleDirection holeDirection) {
    switch (holeDirection) {
      case NORTH -> getFacadeEdgesSide(BlockRotation.COUNTERCLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, LAYER_6);

      case SOUTH -> getFacadeEdgesSide(BlockRotation.CLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, LAYER_6);

      case WEST -> getFacadeEdgesSide(BlockRotation.CLOCKWISE_180)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, LAYER_6);

      case EAST -> getFacadeEdgesSide(BlockRotation.NONE)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, LAYER_6);

      case NORTH_WEST -> getFacadeEdgesCorner(BlockRotation.CLOCKWISE_180)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, LAYER_6);

      case NORTH_EAST -> getFacadeEdgesCorner(BlockRotation.COUNTERCLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, LAYER_6);

      case SOUTH_WEST -> getFacadeEdgesCorner(BlockRotation.CLOCKWISE_90)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, LAYER_6);

      case SOUTH_EAST -> getFacadeEdgesCorner(BlockRotation.NONE)
          .placeInWorld(chunk, mutable, chunkX, chunkZ, LAYER_6);
    }
  }

  private static void generateColumnsAroundHoles(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int bottomY, int topY) {
    // Avoid floating columns
    if (!LAYER_6_GRID_MANAGER.shouldBeFilled(chunkX, chunkZ)) return;

    for (final var gm : COLUMNS_GRID_MANAGERS) {
      if (gm.shouldBeFilled(chunkX, chunkZ))
        fillChunkTerrain(chunk, mutable, chunkX, chunkZ, bottomY, topY);
      else {
        final var xz = gm.getGridXZ(chunkX, chunkZ);
        final int gx = xz.getLeft();
        final int gz = xz.getRight();
        gm.isPastEdge(chunkX, chunkZ).ifPresent(d -> {
          switch (d) {
            case NORTH ->
                getColumnSide(BlockRotation.COUNTERCLOCKWISE_90, gx == 0 ? BlockMirror.NONE : BlockMirror.FRONT_BACK)
                    .placeInWorld(chunk, mutable, chunkX, chunkZ, bottomY);

            case SOUTH -> getColumnSide(BlockRotation.CLOCKWISE_90, gx == 1 ? BlockMirror.NONE : BlockMirror.FRONT_BACK)
                .placeInWorld(chunk, mutable, chunkX, chunkZ, bottomY);

            case WEST -> getColumnSide(BlockRotation.CLOCKWISE_180, gz == 1 ? BlockMirror.NONE : BlockMirror.LEFT_RIGHT)
                .placeInWorld(chunk, mutable, chunkX, chunkZ, bottomY);

            case EAST -> getColumnSide(BlockRotation.NONE, gz == 0 ? BlockMirror.NONE : BlockMirror.LEFT_RIGHT)
                .placeInWorld(chunk, mutable, chunkX, chunkZ, bottomY);

            case NORTH_WEST -> getColumnCorner(BlockRotation.CLOCKWISE_180)
                .placeInWorld(chunk, mutable, chunkX, chunkZ, bottomY);

            case NORTH_EAST -> getColumnCorner(BlockRotation.COUNTERCLOCKWISE_90)
                .placeInWorld(chunk, mutable, chunkX, chunkZ, bottomY);

            case SOUTH_WEST -> getColumnCorner(BlockRotation.CLOCKWISE_90)
                .placeInWorld(chunk, mutable, chunkX, chunkZ, bottomY);

            case SOUTH_EAST -> getColumnCorner(BlockRotation.NONE)
                .placeInWorld(chunk, mutable, chunkX, chunkZ, bottomY);
          }
        });
      }
    }
  }

  private static void generateLayerWithHoles(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ) {
    if (LAYER_8_GRID_MANAGERS.stream().allMatch(gm -> gm.shouldBeFilled(chunkX, chunkZ)))
      fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_8, LAYER_9);
    for (final var chunkGridManager : LAYER_8_GRID_MANAGERS) {
      chunkGridManager.isPastEdge(chunkX, chunkZ)
          .ifPresent(d -> generateHoleInnerRings(chunk, mutable, chunkX, chunkZ, d));
    }
  }

  private static void generateHoleInnerRings(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, ChunkGridManager.HoleDirection holeDirection) {
    for (int y = LAYER_8 - 1; y < LAYER_9; y += 50 + INNER_RING_HEIGHT) {
      switch (holeDirection) {
        case NORTH -> getInnerRingSide(BlockRotation.COUNTERCLOCKWISE_90)
            .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

        case SOUTH -> getInnerRingSide(BlockRotation.CLOCKWISE_90)
            .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

        case WEST -> getInnerRingSide(BlockRotation.CLOCKWISE_180)
            .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

        case EAST -> getInnerRingSide(BlockRotation.NONE)
            .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

        case NORTH_WEST -> getInnerRingCorner(BlockRotation.CLOCKWISE_180)
            .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

        case NORTH_EAST -> getInnerRingCorner(BlockRotation.COUNTERCLOCKWISE_90)
            .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

        case SOUTH_WEST -> getInnerRingCorner(BlockRotation.CLOCKWISE_90)
            .placeInWorld(chunk, mutable, chunkX, chunkZ, y);

        case SOUTH_EAST -> getInnerRingCorner(BlockRotation.NONE)
            .placeInWorld(chunk, mutable, chunkX, chunkZ, y);
      }
    }
  }

  private static void fillChunkTerrain(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int bottomY, int topY) {
    fillChunk(chunk, mutable, chunkX, chunkZ, bottomY, topY, TERRAIN);
  }

  /**
   * Place the surface blocks of the biomes after the noise has been generated.
   */
  @Override
  public void buildSurface(ChunkRegion region, StructureAccessor structureAccessor, NoiseConfig noiseConfig, Chunk chunk) {
    final var mutable = new BlockPos.Mutable();
    final ChunkPos chunkPos = chunk.getPos();
    final int chunkX = chunkPos.x;
    final int chunkZ = chunkPos.z;
    // TODO generate structures in layers 3, 7, 9 and 11
    // TODO generate features in gaps between windows on facades of layer 6
    this.generateFacadeStructures(chunk, mutable, chunkX, chunkZ, structureAccessor);
    generateBaseLayerElevation(chunk, mutable, chunkX, chunkZ, structureAccessor);
  }

  private void generateFacadeStructures(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, StructureAccessor structureAccessor) {
    final int yOffset = 10;
    final var sampler = DoublePerlinNoiseSampler.create(getRandom(structureAccessor), 0, 1.0);
    final double threshold = 0.75;
    LAYER_6_GRID_MANAGER.isPastEdge(chunkX, chunkZ).ifPresent(dir -> {
      for (int y = LAYER_6 + yOffset; y < LAYER_7 - yOffset; y++) {
        for (int d = 7; d < 9; d++) {
          final int dOffset = d - 8; // 8 = offset of antenna’s center
          switch (dir) {
            case NORTH, SOUTH -> {
              if (sampler.sample(getHPos(chunkX, d), y, getHPos(chunkZ, 8)) > threshold)
                getSmallHorizontalAntenna(dir.faces(Direction.NORTH) ? BlockRotation.COUNTERCLOCKWISE_90 : BlockRotation.CLOCKWISE_90)
                    .placeInWorld(chunk, mutable, chunkX, chunkZ, dOffset + (dir.faces(Direction.SOUTH) ? 1 : 0), 0, y);
            }
            case EAST, WEST -> {
              if (sampler.sample(getHPos(chunkX, 8), y, getHPos(chunkZ, d)) > threshold)
                getSmallHorizontalAntenna(dir.faces(Direction.WEST) ? BlockRotation.CLOCKWISE_180 : BlockRotation.NONE)
                    .placeInWorld(chunk, mutable, chunkX, chunkZ, 0, dOffset + (dir.faces(Direction.WEST) ? 1 : 0), y);
            }
          }
        }
      }
    });
  }

  private static void generateBaseLayerElevation(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, StructureAccessor structureAccessor) {
    final var sampler = DoublePerlinNoiseSampler.create(getRandom(structureAccessor), 0, 1.0);
    final int precision = 8;
    for (int dx = 0; dx < 16; dx += precision) {
      final int x = getHPos(chunkX, dx);
      for (int dz = 0; dz < 16; dz += precision) {
        final int z = getHPos(chunkZ, dz);
        final double sample = Math.abs(sampler.sample(x, LAYER_2, z)) * 3;
        int floor = MathHelper.floor(sample);
        int topY = LAYER_2 + floor;
        if (floor > 0)
          fill(chunk, mutable, chunkX, chunkZ, dx, dx + precision, dz, dz + precision, LAYER_2, topY, TERRAIN);
        if (sample - floor >= 0.5)
          fill(chunk, mutable, chunkX, chunkZ, dx, dx + precision, dz, dz + precision, topY, topY + 1, SLAB);
      }
    }
  }

  @SuppressWarnings({"deprecation", "resource"})
  private static ChunkRandom getRandom(StructureAccessor structureAccessor) {
    final long seed = ((ChunkRegion) ((StructureAccessorAccessor) structureAccessor).getWorld()).toServerWorld().getSeed();
    return new ChunkRandom(new CheckedRandom(seed));
  }

  /**
   * Generates caves for the given chunk.
   */
  @Override
  public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
  }

  /**
   * Spawn entities in the world.
   */
  @Override
  public void populateEntities(ChunkRegion region) {
  }

  /**
   * Return a sample of all the block states in a column for use in structure generation.
   */
  @Override
  public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
    return new VerticalBlockSample(0, new BlockState[0]);
  }

  /**
   * Return the raw noise height of a column for use in structure generation.
   */
  @Override
  public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
    return 0;
  }

  /**
   * The lowest value that blocks can be placed in the world.
   */
  @Override
  public int getMinimumY() {
    return LAYER_1;
  }

  /**
   * The distance between the highest and lowest points in the world.
   */
  @Override
  public int getWorldHeight() {
    return WORLD_HEIGHT;
  }

  @Override
  public int getSeaLevel() {
    return 0;
  }

  /**
   * Add text to the F3 menu.
   */
  @Override
  public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
  }

  @Override
  protected Codec<? extends ChunkGenerator> getCodec() {
    return CODEC;
  }
}