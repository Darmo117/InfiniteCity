package net.darmo_creations.infinitecity.blocks;

import net.minecraft.block.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;

/**
 * This class represent a single wall post.
 */
public class ThickPostBlock extends AbstractPostBlock {
  private static final VoxelShape SHAPE_X = createCuboidShape(0, 4, 4, 16, 12, 12);
  private static final VoxelShape SHAPE_Y = createCuboidShape(4, 0, 4, 12, 16, 12);
  private static final VoxelShape SHAPE_Z = createCuboidShape(4, 4, 0, 12, 12, 16);

  public ThickPostBlock(Settings settings) {
    super(settings);
  }

  @Override
  public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return switch (state.get(AXIS)) {
      case X -> SHAPE_X;
      case Y -> SHAPE_Y;
      case Z -> SHAPE_Z;
    };
  }
}
