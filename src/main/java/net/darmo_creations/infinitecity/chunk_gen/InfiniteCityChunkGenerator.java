package net.darmo_creations.infinitecity.chunk_gen;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.*;
import net.darmo_creations.infinitecity.blocks.*;
import net.darmo_creations.infinitecity.mixins.*;
import net.minecraft.block.*;
import net.minecraft.block.enums.*;
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

  private static final int LAYER_1 = 0; // Bedrock
  private static final int LAYER_2 = LAYER_1 + 1; // Thin terrain layer
  private static final int LAYER_3 = LAYER_2 + 1; // Empty space with columns, bridges, arches, etc.
  private static final int LAYER_4 = 200; // ?
  private static final int LAYER_5 = LAYER_4 + 200; // ?
  private static final int LAYER_6 = LAYER_5 + 200; // On-grid blocks with windowed facades
  private static final int LAYER_7 = LAYER_6 + 200; // Empty space with structures, hanging walkways and columns around holes of next layer
  private static final int LAYER_8 = LAYER_7 + 128; // Plain layer with on-grid square holes
  private static final int LAYER_9 = LAYER_8 + 400; // Empty space with structures hanging below next layer
  private static final int LAYER_10 = LAYER_9 + 100; // On-grid blocks
  private static final int LAYER_11 = LAYER_10 + 200; // Empty space with desert landscapes atop blocks of previous layer
  private static final int TOP = 2032; // Max allowed value
  private static final int WORLD_HEIGHT = TOP - LAYER_1;

  private static final BlockState AIR = Blocks.AIR.getDefaultState();
  private static final BlockState BEDROCK = Blocks.BEDROCK.getDefaultState();
  private static final BlockState TERRAIN = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
  private static final BlockState BLACK = Blocks.BLACK_CONCRETE.getDefaultState();
  private static final BlockState SLAB = ModBlocks.LIGHT_GRAY_CONCRETE_SLAB.getDefaultState();
  private static final BlockState LIGHT_BLOCK = ModBlocks.LIGHT_BLOCKS[14].getDefaultState();
  private static final BlockState GLASS_PANE = Blocks.LIGHT_GRAY_STAINED_GLASS_PANE.getDefaultState();
  private static final BlockState GLASS_PANE_X = GLASS_PANE.with(PaneBlock.WEST, true).with(PaneBlock.EAST, true);
  private static final BlockState GLASS_PANE_Z = GLASS_PANE.with(PaneBlock.NORTH, true).with(PaneBlock.SOUTH, true);
  private static final BlockState SAND = Blocks.SAND.getDefaultState();

  private static final BlockState STAIRS_BASE = ModBlocks.LIGHT_GRAY_CONCRETE_STAIRS.getDefaultState();
  private static final BlockState STAIRS_SOUTH = STAIRS_BASE.with(StairsBlock.FACING, Direction.SOUTH);
  private static final BlockState STAIRS_SOUTH_TOP = STAIRS_SOUTH.with(StairsBlock.HALF, BlockHalf.TOP);
  private static final BlockState STAIRS_NORTH = STAIRS_BASE.with(StairsBlock.FACING, Direction.NORTH);
  private static final BlockState STAIRS_NORTH_TOP = STAIRS_NORTH.with(StairsBlock.HALF, BlockHalf.TOP);
  private static final BlockState STAIRS_WEST = STAIRS_BASE.with(StairsBlock.FACING, Direction.WEST);
  private static final BlockState STAIRS_WEST_TOP = STAIRS_WEST.with(StairsBlock.HALF, BlockHalf.TOP);
  private static final BlockState STAIRS_EAST = STAIRS_BASE.with(StairsBlock.FACING, Direction.EAST);
  private static final BlockState STAIRS_EAST_TOP = STAIRS_EAST.with(StairsBlock.HALF, BlockHalf.TOP);

  private static final ChunkGridManager LAYER_6_GRID_MANAGER = new ChunkGridManager(14, 4, 0, 0, false);
  private static final List<ChunkGridManager> LAYER_8_GRID_MANAGERS = List.of(
      new ChunkGridManager(8, 28, 12, 12, true),
      new ChunkGridManager(8, 28, -6, -6, true)
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
    final BlockPos.Mutable mutable = new BlockPos.Mutable();
    final ChunkPos chunkPos = chunk.getPos();
    final int chunkX = chunkPos.x;
    final int chunkZ = chunkPos.z;
    generateBedrockLayer(chunk, mutable, chunkX, chunkZ);
    generateBottomLayer(chunk, mutable, chunkX, chunkZ);
    generateBuildingsLayer(chunk, mutable, chunkX, chunkZ, structureAccessor);
    generateTopOfBuildingsLayer(chunk, mutable, chunkX, chunkZ);
    generateLayerWithHoles(chunk, mutable, chunkX, chunkZ);
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
    int edgeTop = LAYER_11 + 8;
    if (LAYER_10_GRID_MANAGER.shouldBeFilled(chunkX, chunkZ)) {
      fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_10, LAYER_11);
      var atEdge = LAYER_10_GRID_MANAGER.isAtEdge(chunkX, chunkZ);
      if (atEdge.isPresent()) {
        fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_11, edgeTop); // Block edge
        generateDesertEgdePillars(chunk, mutable, chunkX, chunkZ, edgeTop);
        generateBigBlocksInnerEdges(chunk, mutable, chunkX, chunkZ, edgeTop, atEdge.get());
      } else {
        generateDunes(chunk, mutable, chunkX, chunkZ, structureAccessor);
        erodeDunesNearEdge(chunk, mutable, chunkX, chunkZ, edgeTop);
      }
    } else
      LAYER_10_GRID_MANAGER.isPastEdge(chunkX, chunkZ)
          .ifPresent(d -> generateBigBlocksOuterEdges(chunk, mutable, chunkX, chunkZ, edgeTop, d));
  }

  private static void generateDesertEgdePillars(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int edgeTop) {
    int y = edgeTop;
    fill(chunk, mutable, chunkX, chunkZ, 6, 10, 6, 10, y, y += 4, TERRAIN);
    fill(chunk, mutable, chunkX, chunkZ, 5, 11, 5, 11, y, y += 6, TERRAIN);
    fill(chunk, mutable, chunkX, chunkZ, 4, 12, 4, 12, y, y + 8, TERRAIN);
  }

  private static void generateBigBlocksInnerEdges(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int edgeTop, ChunkGridManager.HoleDirection holeDirection) {
    final int smallBlockBottomY = edgeTop - 2;
    final int smallBlockTopY = edgeTop + 2;
    switch (holeDirection) {
      case NORTH -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, edgeTop, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, edgeTop, smallBlockTopY, TERRAIN);
      }
      case SOUTH -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, edgeTop, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, edgeTop, smallBlockTopY, TERRAIN);
      }
      case WEST -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, edgeTop, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, edgeTop, smallBlockTopY, TERRAIN);
      }
      case EAST -> {
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
      case NORTH_WEST -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
      case NORTH_EAST -> {
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
      case SOUTH_WEST -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
      case SOUTH_EAST -> {
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
    }
  }

  private static void generateBigBlocksOuterEdges(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int edgeTop, ChunkGridManager.HoleDirection holeDirection) {
    final int blockBottomY = LAYER_10;
    final int smallBlockBottomY = edgeTop - 2;
    final int smallBlockTopY = edgeTop + 2;
    switch (holeDirection) {
      case NORTH -> {
        final int z = getHPos(chunkZ, 15);
        for (int dx = 0; dx < 16; dx += 15) {
          final int x = getHPos(chunkX, dx);
          for (int y = blockBottomY; y < smallBlockBottomY; y++) {
            chunk.setBlockState(mutable.set(x, y, z), TERRAIN, false);
          }
        }
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
      case SOUTH -> {
        final int z = getHPos(chunkZ, 0);
        for (int dx = 0; dx < 16; dx += 15) {
          final int x = getHPos(chunkX, dx);
          for (int y = blockBottomY; y < smallBlockBottomY; y++) {
            chunk.setBlockState(mutable.set(x, y, z), TERRAIN, false);
          }
        }
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
      case WEST -> {
        final int x = getHPos(chunkX, 15);
        for (int dz = 0; dz < 16; dz += 15) {
          final int z = getHPos(chunkZ, dz);
          for (int y = blockBottomY; y < smallBlockBottomY; y++) {
            chunk.setBlockState(mutable.set(x, y, z), TERRAIN, false);
          }
        }
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
      case EAST -> {
        final int x = getHPos(chunkX, 0);
        for (int dz = 0; dz < 16; dz += 15) {
          final int z = getHPos(chunkZ, dz);
          for (int y = blockBottomY; y < smallBlockBottomY; y++) {
            chunk.setBlockState(mutable.set(x, y, z), TERRAIN, false);
          }
        }
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
      case NORTH_WEST -> {
        final int x = getHPos(chunkX, 15);
        final int z = getHPos(chunkZ, 15);
        for (int y = blockBottomY; y < smallBlockBottomY; y++) {
          chunk.setBlockState(mutable.set(x, y, z), TERRAIN, false);
        }
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
      case NORTH_EAST -> {
        final int x = getHPos(chunkX, 0);
        final int z = getHPos(chunkZ, 15);
        for (int y = blockBottomY; y < smallBlockBottomY; y++) {
          chunk.setBlockState(mutable.set(x, y, z), TERRAIN, false);
        }
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
      case SOUTH_WEST -> {
        final int x = getHPos(chunkX, 15);
        final int z = getHPos(chunkZ, 0);
        for (int y = blockBottomY; y < smallBlockBottomY; y++) {
          chunk.setBlockState(mutable.set(x, y, z), TERRAIN, false);
        }
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
      case SOUTH_EAST -> {
        final int x = getHPos(chunkX, 0);
        final int z = getHPos(chunkZ, 0);
        for (int y = blockBottomY; y < smallBlockBottomY; y++) {
          chunk.setBlockState(mutable.set(x, y, z), TERRAIN, false);
        }
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, smallBlockBottomY, smallBlockTopY, TERRAIN);
      }
    }
  }

  private static void generateDunes(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, StructureAccessor structureAccessor) {
    final DoublePerlinNoiseSampler sampler =
        DoublePerlinNoiseSampler.create(getRandom(structureAccessor), -6, 1.0, 0.5);
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

  private static void erodeDunesNearEdge(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int edgeTop) {
    if (LAYER_10_GRID_MANAGER.isAtEdge(chunkX - 1, chunkZ).isPresent()) {
      for (int i = 0; i < 10; i++)
        fill(chunk, mutable, chunkX, chunkZ, i, i + 1, 0, 16, edgeTop + i, edgeTop + 16, AIR);
    } else if (LAYER_10_GRID_MANAGER.isAtEdge(chunkX + 1, chunkZ).isPresent()) {
      for (int i = 0; i < 10; i++)
        fill(chunk, mutable, chunkX, chunkZ, 15 - i, 16 - i, 0, 16, edgeTop + i, edgeTop + 16, AIR);
    }

    if (LAYER_10_GRID_MANAGER.isAtEdge(chunkX, chunkZ - 1).isPresent()) {
      for (int i = 0; i < 10; i++)
        fill(chunk, mutable, chunkX, chunkZ, 0, 16, i, i + 1, edgeTop + i, edgeTop + 16, AIR);
    } else if (LAYER_10_GRID_MANAGER.isAtEdge(chunkX, chunkZ + 1).isPresent()) {
      for (int i = 0; i < 10; i++)
        fill(chunk, mutable, chunkX, chunkZ, 0, 16, 15 - i, 16 - i, edgeTop + i, edgeTop + 16, AIR);
    }
  }

  private static void generateBuildingsLayer(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, StructureAccessor structureAccessor) {
    final int edgeHeight = 8;
    if (LAYER_6_GRID_MANAGER.shouldBeFilled(chunkX, chunkZ)) {
      fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_6, LAYER_7);
      LAYER_6_GRID_MANAGER.isAtEdge(chunkX, chunkZ).ifPresent(
          d -> generateBuildingFacade(chunk, mutable, chunkX, chunkZ, d, edgeHeight, structureAccessor));
    } else {
      LAYER_6_GRID_MANAGER.isPastEdge(chunkX, chunkZ).ifPresent(d -> {
        generateBuildingFacadeEdge(chunk, mutable, chunkX, chunkZ, d, edgeHeight, true);
        generateBuildingFacadeEdge(chunk, mutable, chunkX, chunkZ, d, edgeHeight, false);
      });
    }
  }

  @SuppressWarnings("SameParameterValue")
  private static void generateBuildingFacade(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, ChunkGridManager.HoleDirection holeDirection, int edgeHeight, StructureAccessor structureAccessor) {
    final DoublePerlinNoiseSampler sampler =
        DoublePerlinNoiseSampler.create(getRandom(structureAccessor), 0, 1.0);
    final int topY = LAYER_7 - edgeHeight;
    final int bottomY = LAYER_6 + edgeHeight;
    final Function<Double, BlockState> getBlockState = sample -> sample > 0.25 ? LIGHT_BLOCK : BLACK;
    // TODO generate features in gaps
    for (int y = topY; y - 3 > bottomY; y -= 3) {
      for (int d = 1; d < 15; d += 2) {
        if (holeDirection.faces(Direction.NORTH) || holeDirection.faces(Direction.SOUTH)) {
          final int z = holeDirection.faces(Direction.NORTH) ? 0 : 15;
          setBlock(chunk, mutable, chunkX, chunkZ, d, z, y - 2, GLASS_PANE_X);
          setBlock(chunk, mutable, chunkX, chunkZ, d, z, y - 3, GLASS_PANE_X);
          final int z1 = z + (holeDirection.faces(Direction.NORTH) ? 1 : -1);
          final double sample = sampler.sample(getHPos(chunkX, d), y, z1);
          final BlockState blockState = getBlockState.apply(sample);
          setBlock(chunk, mutable, chunkX, chunkZ, d, z1, y - 2, blockState);
          setBlock(chunk, mutable, chunkX, chunkZ, d, z1, y - 3, blockState);
        }
        if (holeDirection.faces(Direction.WEST) || holeDirection.faces(Direction.EAST)) {
          final int x = holeDirection.faces(Direction.WEST) ? 0 : 15;
          setBlock(chunk, mutable, chunkX, chunkZ, x, d, y - 2, GLASS_PANE_Z);
          setBlock(chunk, mutable, chunkX, chunkZ, x, d, y - 3, GLASS_PANE_Z);
          final int x1 = x + (holeDirection.faces(Direction.WEST) ? 1 : -1);
          final double sample = sampler.sample(x1, y, getHPos(chunkZ, d));
          final BlockState blockState = getBlockState.apply(sample);
          setBlock(chunk, mutable, chunkX, chunkZ, x1, d, y - 2, blockState);
          setBlock(chunk, mutable, chunkX, chunkZ, x1, d, y - 3, blockState);
        }
        if (d == 5) d += 3; // Leave 4-block empty space at middle
      }
    }
  }

  @SuppressWarnings("SameParameterValue")
  private static void generateBuildingFacadeEdge(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, ChunkGridManager.HoleDirection holeDirection, int edgeHeight, boolean top) {
    final int edgeMinY = top ? LAYER_7 - edgeHeight : LAYER_6;
    final int edgeMaxY = top ? LAYER_7 : LAYER_6 + edgeHeight;
    final int width = 4;
    switch (holeDirection) {
      case NORTH -> {
        final int z0 = 16 - width;
        fill(chunk, mutable, chunkX, chunkZ, 0, 16, z0, z0 + width, edgeMinY, edgeMaxY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 16, z0, z0 + 1, edgeMaxY - 1, edgeMaxY, STAIRS_SOUTH);
        fill(chunk, mutable, chunkX, chunkZ, 0, 16, z0, z0 + 1, edgeMinY, edgeMinY + 1, STAIRS_SOUTH_TOP);
      }
      case SOUTH -> {
        final int z0 = width - 1;
        fill(chunk, mutable, chunkX, chunkZ, 0, 16, 0, width, edgeMinY, edgeMaxY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 16, z0, z0 + 1, edgeMaxY - 1, edgeMaxY, STAIRS_NORTH);
        fill(chunk, mutable, chunkX, chunkZ, 0, 16, z0, z0 + 1, edgeMinY, edgeMinY + 1, STAIRS_NORTH_TOP);
      }
      case WEST -> {
        final int x0 = 16 - width;
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + width, 0, 16, edgeMinY, edgeMaxY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, 0, 16, edgeMaxY - 1, edgeMaxY, STAIRS_EAST);
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, 0, 16, edgeMinY, edgeMinY + 1, STAIRS_EAST_TOP);
      }
      case EAST -> {
        final int x0 = width - 1;
        fill(chunk, mutable, chunkX, chunkZ, 0, width, 0, 16, edgeMinY, edgeMaxY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, 0, 16, edgeMaxY - 1, edgeMaxY, STAIRS_WEST);
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, 0, 16, edgeMinY, edgeMinY + 1, STAIRS_WEST_TOP);
      }
      case NORTH_WEST -> {
        final int x0 = 16 - width;
        final int z0 = 16 - width;
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + width, z0, z0 + width, edgeMinY, edgeMaxY, TERRAIN);
        // North
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + width, z0, z0 + 1, edgeMaxY - 1, edgeMaxY, STAIRS_SOUTH);
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + width, z0, z0 + 1, edgeMinY, edgeMinY + 1, STAIRS_SOUTH_TOP);
        // West
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, z0, z0 + width, edgeMaxY - 1, edgeMaxY, STAIRS_EAST);
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, z0, z0 + width, edgeMinY, edgeMinY + 1, STAIRS_EAST_TOP);
        // Corner
        setBlock(chunk, mutable, chunkX, chunkZ, x0, z0, edgeMaxY - 1, STAIRS_EAST.with(StairsBlock.SHAPE, StairShape.OUTER_RIGHT));
        setBlock(chunk, mutable, chunkX, chunkZ, x0, z0, edgeMinY, STAIRS_EAST_TOP.with(StairsBlock.SHAPE, StairShape.OUTER_RIGHT));
      }
      case NORTH_EAST -> {
        final int x0 = width - 1;
        final int z0 = 16 - width;
        fill(chunk, mutable, chunkX, chunkZ, 0, width, z0, z0 + width, edgeMinY, edgeMaxY, TERRAIN);
        // North
        fill(chunk, mutable, chunkX, chunkZ, 0, width, z0, z0 + 1, edgeMaxY - 1, edgeMaxY, STAIRS_SOUTH);
        fill(chunk, mutable, chunkX, chunkZ, 0, width, z0, z0 + 1, edgeMinY, edgeMinY + 1, STAIRS_SOUTH_TOP);
        // East
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, z0, z0 + width, edgeMaxY - 1, edgeMaxY, STAIRS_WEST);
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, z0, z0 + width, edgeMinY, edgeMinY + 1, STAIRS_WEST_TOP);
        // Corner
        setBlock(chunk, mutable, chunkX, chunkZ, x0, z0, edgeMaxY - 1, STAIRS_WEST.with(StairsBlock.SHAPE, StairShape.OUTER_LEFT));
        setBlock(chunk, mutable, chunkX, chunkZ, x0, z0, edgeMinY, STAIRS_WEST_TOP.with(StairsBlock.SHAPE, StairShape.OUTER_LEFT));
      }
      case SOUTH_WEST -> {
        final int x0 = 16 - width;
        final int z0 = width - 1;
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + width, 0, width, edgeMinY, edgeMaxY, TERRAIN);
        // South
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + width, z0, z0 + 1, edgeMaxY - 1, edgeMaxY, STAIRS_NORTH);
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + width, z0, z0 + 1, edgeMinY, edgeMinY + 1, STAIRS_NORTH_TOP);
        // West
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, 0, width, edgeMaxY - 1, edgeMaxY, STAIRS_EAST);
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, 0, width, edgeMinY, edgeMinY + 1, STAIRS_EAST_TOP);
        // Corner
        setBlock(chunk, mutable, chunkX, chunkZ, x0, z0, edgeMaxY - 1, STAIRS_EAST.with(StairsBlock.SHAPE, StairShape.OUTER_LEFT));
        setBlock(chunk, mutable, chunkX, chunkZ, x0, z0, edgeMinY, STAIRS_EAST_TOP.with(StairsBlock.SHAPE, StairShape.OUTER_LEFT));
      }
      case SOUTH_EAST -> {
        final int x0 = width - 1;
        final int z0 = width - 1;
        fill(chunk, mutable, chunkX, chunkZ, 0, width, 0, width, edgeMinY, edgeMaxY, TERRAIN);
        // South
        fill(chunk, mutable, chunkX, chunkZ, 0, width, z0, z0 + 1, edgeMaxY - 1, edgeMaxY, STAIRS_NORTH);
        fill(chunk, mutable, chunkX, chunkZ, 0, width, z0, z0 + 1, edgeMinY, edgeMinY + 1, STAIRS_NORTH_TOP);
        // East
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, 0, width, edgeMaxY - 1, edgeMaxY, STAIRS_WEST);
        fill(chunk, mutable, chunkX, chunkZ, x0, x0 + 1, 0, width, edgeMinY, edgeMinY + 1, STAIRS_WEST_TOP);
        // Corner
        setBlock(chunk, mutable, chunkX, chunkZ, x0, z0, edgeMaxY - 1, STAIRS_WEST.with(StairsBlock.SHAPE, StairShape.OUTER_RIGHT));
        setBlock(chunk, mutable, chunkX, chunkZ, x0, z0, edgeMinY, STAIRS_WEST_TOP.with(StairsBlock.SHAPE, StairShape.OUTER_RIGHT));
      }
    }
  }

  private static void generateTopOfBuildingsLayer(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ) {
    // Build columns around holes of layer above
    for (ChunkGridManager gm : LAYER_8_GRID_MANAGERS) {
      final int blockSize = gm.getBlockSize();
      final int totalSize = blockSize + gm.getBlockSpacing();
      final var xz = gm.getGridXZ(chunkX, chunkZ);
      final int gx = xz.getLeft();
      final int gz = xz.getRight();
      if ((gx == blockSize + 1 || gx == blockSize + 2) && (gz == blockSize + 1 || gz == blockSize + 2)
          || (gx == blockSize + 1 || gx == blockSize + 2) && (gz == totalSize - 2 || gz == totalSize - 3)
          || (gx == totalSize - 2 || gx == totalSize - 3) && (gz == blockSize + 1 || gz == blockSize + 2)
          || (gx == totalSize - 2 || gx == totalSize - 3) && (gz == totalSize - 2 || gz == totalSize - 3)) {
        fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_7, LAYER_8);
        // TODO add side/corner ornamentations
      }
    }
  }

  private static void generateLayerWithHoles(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ) {
    if (LAYER_8_GRID_MANAGERS.stream().allMatch(gm -> gm.shouldBeFilled(chunkX, chunkZ)))
      fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_8, LAYER_9);
    for (ChunkGridManager chunkGridManager : LAYER_8_GRID_MANAGERS) {
      chunkGridManager.isAtEdge(chunkX, chunkZ).ifPresent(d -> {
        fillChunkTerrain(chunk, mutable, chunkX, chunkZ, LAYER_9, LAYER_9 + 2);
        generateHoleOuterEdges(chunk, mutable, chunkX, chunkZ, d);
      });
      chunkGridManager.isPastEdge(chunkX, chunkZ)
          .ifPresent(d -> generateHoleInnerEdges(chunk, mutable, chunkX, chunkZ, d));
    }
  }

  private static void generateHoleInnerEdges(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, ChunkGridManager.HoleDirection holeDirection) {
    final int bottomY = LAYER_9;
    final int topY = LAYER_9 + 4;
    switch (holeDirection) {
      case SOUTH -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, bottomY, topY, TERRAIN);
      }
      case NORTH -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, bottomY, topY, TERRAIN);
      }
      case EAST -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, bottomY, topY, TERRAIN);
      }
      case WEST -> {
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, bottomY, topY, TERRAIN);
      }
      case SOUTH_EAST -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, bottomY, topY, TERRAIN);
      }
      case SOUTH_WEST -> {
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, bottomY, topY, TERRAIN);
      }
      case NORTH_EAST -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, bottomY, topY, TERRAIN);
      }
      case NORTH_WEST -> {
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, bottomY, topY, TERRAIN);
      }
    }
  }

  private static void generateHoleOuterEdges(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, ChunkGridManager.HoleDirection holeDirection) {
    final int bottomY = LAYER_9 + 2;
    final int topY = LAYER_9 + 4;
    switch (holeDirection) {
      case SOUTH -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, bottomY, topY, TERRAIN);
      }
      case NORTH -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, bottomY, topY, TERRAIN);
      }
      case EAST -> {
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, bottomY, topY, TERRAIN);
      }
      case WEST -> {
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, bottomY, topY, TERRAIN);
        fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, bottomY, topY, TERRAIN);
      }
      case SOUTH_EAST -> fill(chunk, mutable, chunkX, chunkZ, 14, 16, 14, 16, bottomY, topY, TERRAIN);
      case SOUTH_WEST -> fill(chunk, mutable, chunkX, chunkZ, 0, 2, 14, 16, bottomY, topY, TERRAIN);
      case NORTH_EAST -> fill(chunk, mutable, chunkX, chunkZ, 14, 16, 0, 2, bottomY, topY, TERRAIN);
      case NORTH_WEST -> fill(chunk, mutable, chunkX, chunkZ, 0, 2, 0, 2, bottomY, topY, TERRAIN);
    }
  }

  private static void fillChunkTerrain(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int bottomY, int topY) {
    fillChunk(chunk, mutable, chunkX, chunkZ, bottomY, topY, TERRAIN);
  }

  private static void fillChunk(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int bottomY, int topY, BlockState blockState) {
    fill(chunk, mutable, chunkX, chunkZ, 0, 16, 0, 16, bottomY, topY, blockState);
  }

  private static void fill(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int fromX, int toX, int fromZ, int toZ, int bottomY, int topY, BlockState blockState) {
    for (int y = bottomY; y < topY; y++) {
      for (int dx = fromX; dx < toX; dx++) {
        final int x = getHPos(chunkX, dx);
        for (int dz = fromZ; dz < toZ; dz++) {
          final int z = getHPos(chunkZ, dz);
          chunk.setBlockState(mutable.set(x, y, z), blockState, false);
        }
      }
    }
  }

  private static void setBlock(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, int x, int z, int y, BlockState blockState) {
    chunk.setBlockState(mutable.set(getHPos(chunkX, x), y, getHPos(chunkZ, z)), blockState, false);
  }

  /**
   * Place the surface blocks of the biomes after the noise has been generated.
   */
  @Override
  public void buildSurface(ChunkRegion region, StructureAccessor structureAccessor, NoiseConfig noiseConfig, Chunk chunk) {
    final BlockPos.Mutable mutable = new BlockPos.Mutable();
    final ChunkPos chunkPos = chunk.getPos();
    final int chunkX = chunkPos.x;
    final int chunkZ = chunkPos.z;
    // TODO generate structures in layers 3 and 7, in deserts of 5 and below 10
    generateBaseLayerElevation(chunk, mutable, chunkX, chunkZ, structureAccessor);
  }

  private static void generateBaseLayerElevation(Chunk chunk, BlockPos.Mutable mutable, int chunkX, int chunkZ, StructureAccessor structureAccessor) {
    final DoublePerlinNoiseSampler sampler =
        DoublePerlinNoiseSampler.create(getRandom(structureAccessor), 0, 1.0);
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

  private static int getHPos(int chunkCoord, int d) {
    return ChunkSectionPos.getOffsetPos(chunkCoord, d);
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