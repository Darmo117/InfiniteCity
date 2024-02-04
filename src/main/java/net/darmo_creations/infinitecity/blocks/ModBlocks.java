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
@SuppressWarnings("unused")
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

  /**
   * Registers a block and its item, and puts it in the given item group.
   *
   * @param name  Block’s name.
   * @param block Block to register.
   * @param <T>   Type of the block to register.
   * @return The registered block.
   */
  private static <T extends Block> T register(final String name, final T block) {
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
    });
  }

  private ModBlocks() {
  }
}
