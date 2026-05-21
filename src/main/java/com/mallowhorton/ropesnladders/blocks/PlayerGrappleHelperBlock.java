package com.mallowhorton.ropesnladders.blocks;

import com.mallowhorton.ropesnladders.ModBlocks;
import com.mallowhorton.ropesnladders.RopesMod;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.rope.RopeHolderBlock;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlayerGrappleHelperBlock extends Block implements RopeHolderBlock<PlayerGrappleHelperBlock.PlayerGrappleHelperBlockEntity> {

    public PlayerGrappleHelperBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<PlayerGrappleHelperBlockEntity> getBlockEntityClass() {
        return PlayerGrappleHelperBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PlayerGrappleHelperBlockEntity> getBlockEntityType() {
        return ModBlocks.PLAYER_GRAPPLE_HELPER_BLOCK_ENTITY.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);

    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (getBlockEntity(level, pos) != null)
            getBlockEntity(level, pos).destroy();
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    public static class PlayerGrappleHelperBlockEntity extends SmartBlockEntity implements RopeStrandHolderBlockEntity, BlockEntitySubLevelActor {
        public Player trackingPlayer = null;
        private RopeStrandHolderBehavior ropeHolder;
        public PlayerGrappleHelperBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }
        @Override
        public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
            behaviours.add(this.ropeHolder = new RopeStrandHolderBehavior(this));
        }

        @Override
        public RopeStrandHolderBehavior getBehavior() {
            return ropeHolder;
        }

        @Override
        public Vec3 getAttachmentPoint(BlockPos pos, BlockState state) {
            return this.worldPosition.getCenter();
        }

        @Override
        public boolean isValidBlockState(BlockState blockState) {
            return blockState.is(ModBlocks.PLAYER_GRAPPLE_HELPER_BLOCK);
        }

        @Override
        public void tick() {
            super.tick();
        }

        @Override
        public void sable$tick(ServerSubLevel subLevel) {

            if (trackingPlayer != null) {
//                subLevel.getTrackingPlayers().add(trackingPlayer.getUUID());
                trackingPlayer.moveTo(Sable.HELPER.projectOutOfSubLevel(level, getBlockPos().getCenter()));
                RopesMod.LOGGER.atInfo().log("Attempting to move player {} into sublevel pos", trackingPlayer.getDisplayName());
            } else
                subLevel.getTrackingPlayers().clear();

            if (trackingPlayer != null && Sable.HELPER.isInPlotGrid(trackingPlayer))
                try {
                    EntitySubLevelUtil.kickEntity(subLevel, trackingPlayer);
                } catch (Exception e) {

                }
            if (!ropeHolder.isAttached()) {
                level.destroyBlock(worldPosition, false);
            }
        }

        @Override
        public void destroy() {
            ropeHolder.detachRope();
            super.destroy();
//            ropeHolder.destroy();
        }

        @Override
        public void remove() {
            ropeHolder.detachRope();
            super.remove();
//            destroy();
        }
    }

}
