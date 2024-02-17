package net.darmo_creations.infinitecity.blocks;

import net.darmo_creations.infinitecity.*;
import net.fabricmc.fabric.api.item.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.*;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.util.*;

/**
 * Declares all blocks added by this mod.
 */
public final class ModBlocks {
  public static final StairsBlock LIGHT_GRAY_CONCRETE_STAIRS = register(
      "light_gray_concrete_stairs",
      new StairsBlock(Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(), Blocks.LIGHT_GRAY_CONCRETE.getSettings())
  );
  public static final SlabBlock LIGHT_GRAY_CONCRETE_SLAB = register(
      "light_gray_concrete_slab",
      new SlabBlock(Blocks.LIGHT_GRAY_CONCRETE.getSettings())
  );
  public static final VerticalSlabBlock LIGHT_GRAY_CONCRETE_VSLAB = register(
      "light_gray_concrete_vertical_slab",
      new VerticalSlabBlock(Blocks.LIGHT_GRAY_CONCRETE.getSettings())
  );
  public static final WallBlock LIGHT_GRAY_CONCRETE_WALL = register(
      "light_gray_concrete_wall",
      new WallBlock(Blocks.LIGHT_GRAY_CONCRETE.getSettings())
  );
  public static final ThickPostBlock LIGHT_GRAY_CONCRETE_THICK_POST = register(
      "light_gray_concrete_thick_post",
      new ThickPostBlock(Blocks.LIGHT_GRAY_CONCRETE.getSettings())
  );
  public static final PostBlock LIGHT_GRAY_CONCRETE_POST = register(
      "light_gray_concrete_post",
      new PostBlock(Blocks.LIGHT_GRAY_CONCRETE.getSettings())
  );
  public static final CompositeBlock LIGHT_GRAY_CONCRETE_COMPOSITE_BLOCK = register(
      "light_gray_concrete_composite_block",
      new CompositeBlock(Blocks.LIGHT_GRAY_CONCRETE.getSettings())
  );
  public static final Block[] LIGHT_BLOCKS = new Block[15];

  static {
    for (int i = 1; i < 16; i++) {
      final int luminance = i; // Required by the lambda
      LIGHT_BLOCKS[i - 1] = register(
          "light_block_" + i,
          new Block(Blocks.LIGHT_GRAY_CONCRETE.getSettings()
              .mapColor(MapColor.WHITE)
              .luminance(value -> luminance))
      );
    }
  }

  /**
   * Registers a block and its item, and puts it in the given item group.
   *
   * @param name  Block’s name.
   * @param block Block to register.
   * @param <T>   Type of the block to register.
   * @return The registered block.
   */
  private static <T extends Block> T register(String name, final T block) {
    Registry.register(Registries.BLOCK, new Identifier(InfiniteCity.MOD_ID, name), block);
    Item.Settings settings = new FabricItemSettings();
    if (block instanceof OperatorBlock) {
      settings = settings.rarity(Rarity.EPIC);
    }
    final BlockItem blockItem = new BlockItem(block, settings);
    Registry.register(Registries.ITEM, new Identifier(InfiniteCity.MOD_ID, name), blockItem);
    return block;
  }

  /**
   * Register the items for this mod’s blocks.
   * <p>
   * Must be called on both clients and server.
   */
  public static void init() {
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.COLORED_BLOCKS).register(content -> {
      content.addAfter(Items.LIGHT_GRAY_CONCRETE, LIGHT_GRAY_CONCRETE_STAIRS.asItem());
      content.addAfter(LIGHT_GRAY_CONCRETE_STAIRS, LIGHT_GRAY_CONCRETE_SLAB.asItem());
      content.addAfter(LIGHT_GRAY_CONCRETE_SLAB, LIGHT_GRAY_CONCRETE_VSLAB.asItem());
      content.addAfter(LIGHT_GRAY_CONCRETE_VSLAB, LIGHT_GRAY_CONCRETE_WALL.asItem());
      content.addAfter(LIGHT_GRAY_CONCRETE_WALL, LIGHT_GRAY_CONCRETE_THICK_POST.asItem());
      content.addAfter(LIGHT_GRAY_CONCRETE_THICK_POST, LIGHT_GRAY_CONCRETE_POST.asItem());
      content.addAfter(LIGHT_GRAY_CONCRETE_POST, LIGHT_GRAY_CONCRETE_COMPOSITE_BLOCK.asItem());
    });

    ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> {
      Item prev = Items.PEARLESCENT_FROGLIGHT;
      for (Block lightBlock : LIGHT_BLOCKS) {
        content.addAfter(prev, lightBlock);
        prev = lightBlock.asItem();
      }
    });
  }

  private ModBlocks() {
  }
}
