package net.darmo_creations.infinitecity.blocks;

import net.minecraft.block.*;
import net.minecraft.fluid.*;
import net.minecraft.item.*;
import net.minecraft.state.*;
import net.minecraft.state.property.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;

/**
 * Base class for posts.
 */
public abstract class AbstractPostBlock extends PillarBlock implements Waterloggable {
  public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

  public AbstractPostBlock(Settings settings) {
    super(settings);
    this.setDefaultState(this.getStateManager().getDefaultState()
        .with(AXIS, Direction.Axis.Y)
        .with(WATERLOGGED, false));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder.add(WATERLOGGED));
  }

  @SuppressWarnings("deprecation")
  @Override
  public abstract VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context);

  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
    return this.getDefaultState()
        .with(AXIS, ctx.getSide().getAxis())
        .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
  }

  @SuppressWarnings("deprecation")
  @Override
  public FluidState getFluidState(BlockState state) {
    if (state.get(WATERLOGGED)) {
      return Fluids.WATER.getStill(false);
    }
    return super.getFluidState(state);
  }
}
