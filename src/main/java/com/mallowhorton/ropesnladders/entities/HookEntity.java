package com.mallowhorton.ropesnladders.entities;

import com.mallowhorton.ropesnladders.ModBlocks;
import com.mallowhorton.ropesnladders.RopesMod;
import com.mallowhorton.ropesnladders.blocks.PlayerGrappleHelperBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.AssemblyException;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlock;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.rope_connector.RopeConnectorBlock;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntityRenderer;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class HookEntity extends ThrowableProjectile {
//    private boolean stuck = false;
    public static final EntityDataAccessor<Optional<UUID>> PLAYER = SynchedEntityData.defineId(HookEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private int timesStale = 0;
//    private float goalDistance = 4f;
    public HookEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PLAYER, Optional.of(UUID.randomUUID()));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
//        super.onHit(result);
        this.setDeltaMovement(Vec3.ZERO);
        moveTo(result.getLocation());
        var uuidOpt = entityData.get(PLAYER);
        if (uuidOpt.isEmpty()) {
            remove(RemovalReason.DISCARDED);
            RopesMod.LOGGER.atInfo().log("Discarded because there is no table entry for this player");
        }
        uuidOpt.ifPresent(uuid -> {
            var player = level().getPlayerByUUID(uuid);
            if (player == null) {
                remove(RemovalReason.DISCARDED);
                RopesMod.LOGGER.atInfo().log("Discarded because this player does not exist");

                return;
            }
            player.swing(InteractionHand.MAIN_HAND);
            if (Sable.HELPER.isInPlotGrid(this)) {

            } else {
                var airPos = result.getBlockPos().relative(result.getDirection());
                var state = level().getBlockState(airPos);
                if (state.canBeReplaced()) {
                    level().setBlockAndUpdate(airPos, SimBlocks.ROPE_CONNECTOR.getDefaultState().setValue(RopeConnectorBlock.FACING, result.getDirection()));
                    level().setBlockAndUpdate(player.blockPosition(), ModBlocks.PLAYER_GRAPPLE_HELPER_BLOCK.getDefaultState());
                    try {
                        if (level().isClientSide) {
                            var sublevel = RopesMod.ACTIVE_GRAPPLE_LOOKUP.playerLevel(player.getUUID());
                            if (sublevel != null) {
//                                player.moveTo(sublevel.getPlot().getCenterBlock().getCenter());
                                var blockEntityActor = sublevel.getPlot().getBlockEntityActors().iterator().next();
                                if (!(blockEntityActor instanceof PlayerGrappleHelperBlock.PlayerGrappleHelperBlockEntity blockEntity)) return;
                                blockEntity.trackingPlayer = player;
                            }
                            return;
                        };
                        var assemblyResult = SimAssemblyHelper.assembleFromSingleBlock(
                                level(), player.blockPosition(), player.blockPosition(), true, false
                        );
                        var blockEntityActor = assemblyResult.subLevel().getPlot().getBlockEntityActors().iterator().next();
                        if (!(blockEntityActor instanceof PlayerGrappleHelperBlock.PlayerGrappleHelperBlockEntity blockEntity)) return;
                        blockEntity.trackingPlayer = player;
                        ((RopeStrandHolderBlockEntity)level().getBlockEntity(airPos)).getBehavior().createRope(blockEntity.getBehavior());
                        RopesMod.ACTIVE_GRAPPLE_LOOKUP.grapple(player.getUUID(), assemblyResult.subLevel(), player.level());
                        if (!(assemblyResult.subLevel() instanceof ServerSubLevel serverSubLevel)) return;

                        serverSubLevel.getTrackingPlayers().add(player.getUUID());

                    } catch (AssemblyException e) {
                        RopesMod.LOGGER.atError().log("Failed to assemble player sublevel!");
                    }
                }
                remove(RemovalReason.DISCARDED);
            }
        });

    }


    @Override
    public void tick() {
        super.tick();

        var isStale = new AtomicBoolean(true);
        RopesMod.PROJECTILE_LOOKUP.iterable().forEach(p -> {
            if (p.getSecond().getUUID().equals(this.uuid))
                isStale.set(false);
        });
        if (isStale.get()) {
            if (timesStale > 10)
                this.remove(RemovalReason.DISCARDED);
            timesStale++;
            return;
        } else
            timesStale = 0;



    }

    public static class Renderer extends EntityRenderer<HookEntity> {
        public static final RenderType RENDER_TYPE = RenderType.entityCutout(ResourceLocation.parse("ropesnladders:textures/hook.png"));
        public Renderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(HookEntity hookEntity) {
            return ResourceLocation.parse("ropesnladders:hook");
        }

        @Override
        public void render(HookEntity p_entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            super.render(p_entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            Player player = p_entity.level().getPlayerByUUID(p_entity.entityData.get(HookEntity.PLAYER).get());
            if (player != null) {
                poseStack.pushPose();
                poseStack.scale(0.5F, 0.5F, 0.5F);

                if (Sable.HELPER.isInPlotGrid(p_entity)) {
                    var sublevel = Sable.HELPER.getContaining(p_entity);
                    var logicPose = sublevel.logicalPose();
//                    poseStack.rotateAround(new Quaternionf(logicPose.orientation()), (float)logicPose.rotationPoint().x, (float)logicPose.rotationPoint().y, (float)logicPose.rotationPoint().z);
                    var orientation = new Quaternionf(new Quaterniond(logicPose.orientation()).invert());

//                    poseStack.rotateAround(orientation, (float)logicPose.rotationPoint().x, (float)logicPose.rotationPoint().y, (float)logicPose.rotationPoint().z);
                    poseStack.rotateAround(orientation, 0, 1f, 0);
                    poseStack.translate(0, .5f, 0);
                    poseStack.mulPose(new Quaternionf(orientation));
                }
                poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

                PoseStack.Pose posestack$pose = poseStack.last();
                VertexConsumer vertexconsumer = bufferSource.getBuffer(RENDER_TYPE);
                vertex(vertexconsumer, posestack$pose, packedLight, 0.0F, 0, 0, 1);
                vertex(vertexconsumer, posestack$pose, packedLight, 1.0F, 0, 1, 1);
                vertex(vertexconsumer, posestack$pose, packedLight, 1.0F, 1, 1, 0);
                vertex(vertexconsumer, posestack$pose, packedLight, 0.0F, 1, 0, 0);
//                var lineBuffer = bufferSource.getBuffer(RenderType.lineStrip());
//                var pose = poseStack.last();
//                lineBuffer.addVertex((float)p_entity.position().x, (float)p_entity.position().y, (float)p_entity.position().z, 0xFFFFFFFF, 0, 0, 0, packedLight, 0, 0, 0);
//                lineBuffer.addVertex(0, 0, 0, 0xFFFFFFFF, 0, 0, 1, packedLight, 0, 0, 0);
                poseStack.scale(2, 2, 2);
                var list = new LinkedList<Vec3>();
                var playerPos = player.getPosition(partialTick).add(new Vec3(0, 1.3, 0));
                var entityPos = p_entity.getPosition(partialTick);
                list.add(entityPos.add(new Vec3(0, 0.1, 0)));
                list.add(entityPos.add(new Vec3(0, 0.1, 0)));
                for (int i = 2; i < 5; i++) {
                    list.add(entityPos.lerp(playerPos, (float) i / 5)
                            .add(new Vec3(
                                    0, -((float) i - 4) * ((float) i - 4) / 4 * 0, 0
                            )));

                }

                list.add(playerPos);
                list.add(playerPos);
                LaunchedPlungerEntityRenderer.renderRope(
                        list, bufferSource, p_entity.level(), poseStack
                );
                poseStack.popPose();
            }
        }
        private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight, float x, float y, int u, int v) {
            consumer.addVertex(pose, x - 0.5F, (float)y - 0.5F, 0.0F).setColor(-1).setUv((float)u, (float)v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
    }
}
