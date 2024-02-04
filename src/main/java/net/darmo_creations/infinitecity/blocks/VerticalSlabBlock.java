package net.darmo_creations.infinitecity.blocks;

import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.player.*;
import net.minecraft.fluid.*;
import net.minecraft.item.*;
import net.minecraft.registry.tag.*;
import net.minecraft.state.*;
import net.minecraft.state.property.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;
import org.jetbrains.annotations.*;

/**
 * Base class for vertical slabs.
 * Vertical slabs are waterloggable.
 */
public class VerticalSlabBlock extends Block implements Waterloggable {
  public static final EnumProperty<VerticalSlabType> TYPE = EnumProperty.of("type", VerticalSlabType.class);
  public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

  protected static final VoxelShape NORTH_SHAPE = createCuboidShape(0, 0, 0, 16, 16, 8);
  protected static final VoxelShape SOUTH_SHAPE = createCuboidShape(0, 0, 8, 16, 16, 16);
  protected static final VoxelShape WEST_SHAPE = createCuboidShape(0, 0, 0, 8, 16, 16);
  protected static final VoxelShape EAST_SHAPE = createCuboidShape(8, 0, 0, 16, 16, 16);

  public VerticalSlabBlock(Settings settings) {
    super(settings);
    this.setDefaultState(this.getStateManager().getDefaultState()
        .with(TYPE, VerticalSlabType.NORTH)
        .with(WATERLOGGED, false));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder.add(TYPE, WATERLOGGED));
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean hasSidedTransparency(BlockState state) {
    return state.get(TYPE) != VerticalSlabType.DOUBLE;
  }

  @SuppressWarnings("deprecation")
  @Override
  public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return switch (state.get(TYPE)) {
      case NORTH -> NORTH_SHAPE;
      case SOUTH -> SOUTH_SHAPE;
      case EAST -> EAST_SHAPE;
      case WEST -> WEST_SHAPE;
      case DOUBLE -> VoxelShapes.fullCube();
    };
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    final BlockPos blockPos = ctx.getBlockPos();
    final BlockState blockState = ctx.getWorld().getBlockState(blockPos);
    if (blockState.isOf(this)) {
      return blockState.with(TYPE, VerticalSlabType.DOUBLE).with(WATERLOGGED, false);
    }
    final VerticalSlabType type;
    final Direction direction = ctx.getSide();
    if (direction.getAxis().isHorizontal()) {
      type = VerticalSlabType.forDirection(direction.getOpposite());
    } else {
      final Vec3d hitPos = ctx.getHitPos();
      if (ctx.getHorizontalPlayerFacing().getAxis() == Direction.Axis.Z) {
        type = hitPos.z - blockPos.getZ() > 0.5 ? VerticalSlabType.SOUTH : VerticalSlabType.NORTH;
      } else {
        type = hitPos.x - blockPos.getX() > 0.5 ? VerticalSlabType.EAST : VerticalSlabType.WEST;
      }
    }
    return this.getDefaultState()
        .with(TYPE, type)
        .with(WATERLOGGED, ctx.getWorld().getFluidState(blockPos).getFluid() == Fluids.WATER);
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean canReplace(BlockState state, ItemPlacementContext context) {
    final ItemStack itemStack = context.getStack();
    final VerticalSlabType slabType = state.get(TYPE);
    if (slabType == VerticalSlabType.DOUBLE || !itemStack.isOf(this.asItem())) {
      return false;
    }
    if (context.canReplaceExisting()) {
      final Direction side = context.getSide();
      if (side.getAxis() == Direction.Axis.Y) {
        final Vec3d hitPos = context.getHitPos();
        final BlockPos blockPos = context.getBlockPos();
        final double xHit = hitPos.x - blockPos.getX();
        final double zHit = hitPos.z - blockPos.getZ();
        return switch (slabType) {
          case NORTH -> zHit > 0.5;
          case SOUTH -> zHit < 0.5;
          case WEST -> xHit > 0.5;
          case EAST -> xHit < 0.5;
          default -> false;
        };
      }
      return slabType.getDirection().map(d -> d == side.getOpposite()).orElse(false);
    }
    return true;
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
    if (state.get(TYPE) != VerticalSlabType.DOUBLE) {
      return Waterloggable.super.tryFillWithFluid(world, pos, state, fluidState);
    }
    return false;
  }

  @Override
  public boolean canFillWithFluid(@Nullable PlayerEntity player, BlockView world, BlockPos pos, BlockState state, Fluid fluid) {
    if (state.get(TYPE) != VerticalSlabType.DOUBLE) {
      return Waterloggable.super.canFillWithFluid(player, world, pos, state, fluid);
    }
    return false;
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
    if (state.get(WATERLOGGED)) {
      world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
    }
    return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
    return type == NavigationType.WATER && world.getFluidState(pos).isIn(FluidTags.WATER);
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState rotate(BlockState state, BlockRotation rotation) {
    return state.with(TYPE, state.get(TYPE).rotate(rotation));
  }

  @SuppressWarnings("deprecation")
  @Override
  public BlockState mirror(BlockState state, BlockMirror mirror) {
    return state.with(TYPE, state.get(TYPE).mirror(mirror));
  }
}
