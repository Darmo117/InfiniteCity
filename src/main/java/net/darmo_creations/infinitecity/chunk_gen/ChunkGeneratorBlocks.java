package net.darmo_creations.infinitecity.chunk_gen;

import net.darmo_creations.infinitecity.blocks.*;
import net.minecraft.block.*;

final class ChunkGeneratorBlocks {
  public static final BlockState AIR = Blocks.AIR.getDefaultState();
  public static final BlockState BEDROCK = Blocks.BEDROCK.getDefaultState();
  public static final BlockState TERRAIN = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
  public static final BlockState BLACK = Blocks.BLACK_CONCRETE.getDefaultState();
  public static final BlockState SLAB = ModBlocks.LIGHT_GRAY_CONCRETE_SLAB.getDefaultState();
  public static final BlockState LIGHT_BLOCK = ModBlocks.LIGHT_BLOCKS[14].getDefaultState();
  public static final BlockState GLASS_PANE = Blocks.LIGHT_GRAY_STAINED_GLASS_PANE.getDefaultState();
  public static final BlockState GLASS_PANE_X = GLASS_PANE.with(PaneBlock.WEST, true).with(PaneBlock.EAST, true);
  public static final BlockState GLASS_PANE_Z = GLASS_PANE.with(PaneBlock.NORTH, true).with(PaneBlock.SOUTH, true);
  public static final BlockState SAND = Blocks.SAND.getDefaultState();
  public static final BlockState STAIRS = ModBlocks.LIGHT_GRAY_CONCRETE_STAIRS.getDefaultState();
  public static final BlockState COMPOSITE = ModBlocks.LIGHT_GRAY_CONCRETE_COMPOSITE_BLOCK.getDefaultState();
  public static final BlockState THICK_POST = ModBlocks.LIGHT_GRAY_CONCRETE_THICK_POST.getDefaultState();
  public static final BlockState POST = ModBlocks.LIGHT_GRAY_CONCRETE_POST.getDefaultState();

  private ChunkGeneratorBlocks() {
  }
}
