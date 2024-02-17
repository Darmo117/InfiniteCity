package net.darmo_creations.infinitecity.blocks;

import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.fluid.*;
import net.minecraft.item.*;
import net.minecraft.state.*;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * A block composed of simple 8x8x8 pixels corners.
 */
public class CompositeBlock extends Block implements Waterloggable {
  public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

  public static final BooleanProperty NORTH_EAST_TOP = BooleanProperty.of("north_east_top");
  public static final BooleanProperty NORTH_WEST_TOP = BooleanProperty.of("north_west_top");
  public static final BooleanProperty NORTH_EAST_BOTTOM = BooleanProperty.of("north_east_bottom");
  public static final BooleanProperty NORTH_WEST_BOTTOM = BooleanProperty.of("north_west_bottom");
  public static final BooleanProperty SOUTH_EAST_TOP = BooleanProperty.of("south_east_top");
  public static final BooleanProperty SOUTH_WEST_TOP = BooleanProperty.of("south_west_top");
  public static final BooleanProperty SOUTH_EAST_BOTTOM = BooleanProperty.of("south_east_bottom");
  public static final BooleanProperty SOUTH_WEST_BOTTOM = BooleanProperty.of("south_west_bottom");

  public static final BooleanProperty[] CORNER_PROPERTIES = {
      NORTH_EAST_TOP, NORTH_WEST_TOP, NORTH_EAST_BOTTOM, NORTH_WEST_BOTTOM,
      SOUTH_EAST_TOP, SOUTH_WEST_TOP, SOUTH_EAST_BOTTOM, SOUTH_WEST_BOTTOM,
  };

  private static final VoxelShape NORTH_WEST_TOP_SHAPE = createCuboidShape(0, 8, 0, 8, 16, 8);
  private static final VoxelShape NORTH_WEST_BOTTOM_SHAPE = createCuboidShape(0, 0, 0, 8, 8, 8);
  private static final VoxelShape NORTH_EAST_TOP_SHAPE = createCuboidShape(8, 8, 0, 16, 16, 8);
  private static final VoxelShape NORTH_EAST_BOTTOM_SHAPE = createCuboidShape(8, 0, 0, 16, 8, 8);
  private static final VoxelShape SOUTH_WEST_TOP_SHAPE = createCuboidShape(0, 8, 8, 8, 16, 16);
  private static final VoxelShape SOUTH_WEST_BOTTOM_SHAPE = createCuboidShape(0, 0, 8, 8, 8, 16);
  private static final VoxelShape SOUTH_EAST_TOP_SHAPE = createCuboidShape(8, 8, 8, 16, 16, 16);
  private static final VoxelShape SOUTH_EAST_BOTTOM_SHAPE = createCuboidShape(8, 0, 8, 16, 8, 16);

  /**
   * Creates a corner block for the given color.
   *
   * @param settings Block’s settings.
   */
  public CompositeBlock(Settings settings) {
    super(settings);
    this.setDefaultState(this.getStateManager().getDefaultState()
        .with(NORTH_EAST_TOP, false)
        .with(NORTH_WEST_TOP, false)
        .with(NORTH_EAST_BOTTOM, false)
        .with(NORTH_WEST_BOTTOM, false)
        .with(SOUTH_EAST_TOP, false)
        .with(SOUTH_WEST_TOP, false)
        .with(SOUTH_EAST_BOTTOM, false)
        .with(SOUTH_WEST_BOTTOM, false)
        .with(WATERLOGGED, false));
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean isFullBlock(final BlockState state) {
    return Arrays.stream(CORNER_PROPERTIES).allMatch(state::get); // All true
  }

  public boolean isEmptyBlock(final BlockState state) {
    return Arrays.stream(CORNER_PROPERTIES).noneMatch(state::get); // None true
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder.add(WATERLOGGED,
        NORTH_EAST_TOP, NORTH_EAST_BOTTOM, NORTH_WEST_TOP, NORTH_WEST_BOTTOM,
        SOUTH_EAST_TOP, SOUTH_WEST_TOP, SOUTH_EAST_BOTTOM, SOUTH_WEST_BOTTOM));
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return VoxelShapes.union(
        state.get(NORTH_WEST_TOP) ? NORTH_WEST_TOP_SHAPE : VoxelShapes.empty(),
        state.get(NORTH_WEST_BOTTOM) ? NORTH_WEST_BOTTOM_SHAPE : VoxelShapes.empty(),
        state.get(NORTH_EAST_TOP) ? NORTH_EAST_TOP_SHAPE : VoxelShapes.empty(),
        state.get(NORTH_EAST_BOTTOM) ? NORTH_EAST_BOTTOM_SHAPE : VoxelShapes.empty(),
        state.get(SOUTH_WEST_TOP) ? SOUTH_WEST_TOP_SHAPE : VoxelShapes.empty(),
        state.get(SOUTH_WEST_BOTTOM) ? SOUTH_WEST_BOTTOM_SHAPE : VoxelShapes.empty(),
        state.get(SOUTH_EAST_TOP) ? SOUTH_EAST_TOP_SHAPE : VoxelShapes.empty(),
        state.get(SOUTH_EAST_BOTTOM) ? SOUTH_EAST_BOTTOM_SHAPE : VoxelShapes.empty()
    );
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean hasSidedTransparency(BlockState state) {
    return !this.isFullBlock(state);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
    if (this.isEmptyBlock(state)) {
      world.setBlockState(pos, Blocks.AIR.getDefaultState());
    } else {
      super.onBlockAdded(state, world, pos, oldState, notify);
    }
  }

  @Override
  public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
    if (this.isEmptyBlock(state)) {
      world.setBlockState(pos, Blocks.AIR.getDefaultState());
    } else {
      super.onPlaced(world, pos, state, placer, itemStack);
    }
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    BlockPos blockPos = ctx.getBlockPos();
    BlockState blockState = ctx.getWorld().getBlockState(blockPos);
    BlockState newState = this.getNewState(blockState.isOf(this) ? blockState : this.getDefaultState(), ctx);
    if (this.isFullBlock(newState)) {
      return newState.with(WATERLOGGED, false);
    }
    FluidState fluidState = ctx.getWorld().getFluidState(blockPos);
    return newState.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean canReplace(BlockState state, ItemPlacementContext context) {
    if (!context.getStack().isOf(this.asItem()) || this.isFullBlock(state)) {
      return false;
    }
    BlockState newState = this.getNewState(state, context);
    return Arrays.stream(CORNER_PROPERTIES).anyMatch(p -> state.get(p) != newState.get(p));
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
    if (this.isEmptyBlock(state)) {
      return Blocks.AIR.getDefaultState();
    }
    return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
  }

  @SuppressWarnings("deprecation")
  @Override
  public FluidState getFluidState(BlockState state) {
    if (state.get(WATERLOGGED)) {
      return Fluids.WATER.getStill(false);
    }
    return super.getFluidState(state);
  }

  @Override
  public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluidState) {
    return !this.isFullBlock(state) && Waterloggable.super.tryFillWithFluid(world, pos, state, fluidState);
  }

  @Override
  public boolean canFillWithFluid(@Nullable PlayerEntity player, BlockView world, BlockPos pos, BlockState state, Fluid fluid) {
    return !this.isFullBlock(state) && Waterloggable.super.canFillWithFluid(player, world, pos, state, fluid);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState rotate(BlockState state, BlockRotation rotation) {
    return switch (rotation) {
      case NONE -> state;
      case CLOCKWISE_90 -> state
          .with(NORTH_WEST_TOP, state.get(SOUTH_WEST_TOP))
          .with(NORTH_EAST_TOP, state.get(NORTH_WEST_TOP))
          .with(SOUTH_EAST_TOP, state.get(NORTH_EAST_TOP))
          .with(SOUTH_WEST_TOP, state.get(SOUTH_EAST_TOP))
          .with(NORTH_WEST_BOTTOM, state.get(SOUTH_WEST_BOTTOM))
          .with(NORTH_EAST_BOTTOM, state.get(NORTH_WEST_BOTTOM))
          .with(SOUTH_EAST_BOTTOM, state.get(NORTH_EAST_BOTTOM))
          .with(SOUTH_WEST_BOTTOM, state.get(SOUTH_EAST_BOTTOM));
      case COUNTERCLOCKWISE_90 -> state
          .with(NORTH_WEST_TOP, state.get(NORTH_EAST_TOP))
          .with(NORTH_EAST_TOP, state.get(SOUTH_EAST_TOP))
          .with(SOUTH_EAST_TOP, state.get(SOUTH_WEST_TOP))
          .with(SOUTH_WEST_TOP, state.get(NORTH_WEST_TOP))
          .with(NORTH_WEST_BOTTOM, state.get(NORTH_EAST_BOTTOM))
          .with(NORTH_EAST_BOTTOM, state.get(SOUTH_EAST_BOTTOM))
          .with(SOUTH_EAST_BOTTOM, state.get(SOUTH_WEST_BOTTOM))
          .with(SOUTH_WEST_BOTTOM, state.get(NORTH_WEST_BOTTOM));
      case CLOCKWISE_180 -> swap(swap(swap(swap(state,
                      NORTH_WEST_TOP, SOUTH_EAST_TOP),
                  NORTH_EAST_TOP, SOUTH_WEST_TOP),
              NORTH_WEST_BOTTOM, SOUTH_EAST_BOTTOM),
          NORTH_EAST_BOTTOM, SOUTH_WEST_BOTTOM);
    };
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState mirror(BlockState state, BlockMirror mirror) {
    return switch (mirror) {
      case NONE -> state;
      case LEFT_RIGHT -> swap(swap(swap(swap(state,
                      NORTH_WEST_BOTTOM, SOUTH_WEST_BOTTOM),
                  NORTH_EAST_BOTTOM, SOUTH_EAST_BOTTOM),
              NORTH_WEST_TOP, SOUTH_WEST_TOP),
          NORTH_EAST_TOP, SOUTH_EAST_TOP);
      case FRONT_BACK -> swap(swap(swap(swap(state,
                      NORTH_WEST_BOTTOM, NORTH_EAST_BOTTOM),
                  NORTH_WEST_TOP, NORTH_EAST_TOP),
              SOUTH_WEST_BOTTOM, SOUTH_EAST_BOTTOM),
          SOUTH_WEST_TOP, SOUTH_EAST_TOP);
    };
  }

  private BlockState getNewState(final BlockState currentState, final ItemPlacementContext ctx) {
    BlockPos pos = ctx.getBlockPos();
    double xHit = ctx.getHitPos().getX() - pos.getX();
    double yHit = ctx.getHitPos().getY() - pos.getY();
    double zHit = ctx.getHitPos().getZ() - pos.getZ();
    boolean nwt = currentState.get(NORTH_WEST_TOP);
    boolean net = currentState.get(NORTH_EAST_TOP);
    boolean nwb = currentState.get(NORTH_WEST_BOTTOM);
    boolean neb = currentState.get(NORTH_EAST_BOTTOM);
    boolean swt = currentState.get(SOUTH_WEST_TOP);
    boolean set = currentState.get(SOUTH_EAST_TOP);
    boolean swb = currentState.get(SOUTH_WEST_BOTTOM);
    boolean seb = currentState.get(SOUTH_EAST_BOTTOM);
    return currentState
        .with(NORTH_WEST_TOP, nwt || (xHit < 0.5 || xHit == 0.5 && net) && (yHit > 0.5 || yHit == 0.5 && nwb) && (zHit < 0.5 || zHit == 0.5 && swt))
        .with(NORTH_EAST_TOP, net || (xHit > 0.5 || xHit == 0.5 && nwt) && (yHit > 0.5 || yHit == 0.5 && neb) && (zHit < 0.5 || zHit == 0.5 && set))
        .with(NORTH_WEST_BOTTOM, nwb || (xHit < 0.5 || xHit == 0.5 && neb) && (yHit < 0.5 || yHit == 0.5 && nwt) && (zHit < 0.5 || zHit == 0.5 && swb))
        .with(NORTH_EAST_BOTTOM, neb || (xHit > 0.5 || xHit == 0.5 && nwb) && (yHit < 0.5 || yHit == 0.5 && net) && (zHit < 0.5 || zHit == 0.5 && seb))
        .with(SOUTH_WEST_TOP, swt || (xHit < 0.5 || xHit == 0.5 && set) && (yHit > 0.5 || yHit == 0.5 && swb) && (zHit > 0.5 || zHit == 0.5 && nwt))
        .with(SOUTH_EAST_TOP, set || (xHit > 0.5 || xHit == 0.5 && swt) && (yHit > 0.5 || yHit == 0.5 && seb) && (zHit > 0.5 || zHit == 0.5 && net))
        .with(SOUTH_WEST_BOTTOM, swb || (xHit < 0.5 || xHit == 0.5 && seb) && (yHit < 0.5 || yHit == 0.5 && swt) && (zHit > 0.5 || zHit == 0.5 && nwb))
        .with(SOUTH_EAST_BOTTOM, seb || (xHit > 0.5 || xHit == 0.5 && swb) && (yHit < 0.5 || yHit == 0.5 && set) && (zHit > 0.5 || zHit == 0.5 && neb));
  }

  private static BlockState swap(final BlockState state, final BooleanProperty p1, final BooleanProperty p2) {
    return state.with(p1, state.get(p2)).with(p2, state.get(p1));
  }
}
