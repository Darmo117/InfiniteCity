package net.darmo_creations.infinitecity.blocks;

import net.minecraft.block.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;

/**
 * This class represents a single fence post.
 */
public class PostBlock extends AbstractPostBlock {
  private static final VoxelShape SHAPE_X = createCuboidShape(0, 6, 6, 16, 10, 10);
  private static final VoxelShape SHAPE_Y = createCuboidShape(6, 0, 6, 10, 16, 10);
  private static final VoxelShape SHAPE_Z = createCuboidShape(6, 6, 0, 10, 10, 16);

  public PostBlock(Settings settings) {
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
