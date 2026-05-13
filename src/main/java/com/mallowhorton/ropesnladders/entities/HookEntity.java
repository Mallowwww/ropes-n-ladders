package com.mallowhorton.ropesnladders.entities;

import com.mallowhorton.ropesnladders.RopesMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class HookEntity extends ThrowableProjectile {
//    private boolean stuck = false;
    public static final EntityDataAccessor<Boolean> IS_STUCK = SynchedEntityData.defineId(HookEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Optional<UUID>> PLAYER = SynchedEntityData.defineId(HookEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<BlockPos> STUCK_BLOCK_POS = SynchedEntityData.defineId(HookEntity.class, EntityDataSerializers.BLOCK_POS);
    private int timesStale = 0;
    private float goalDistance = 4f;
    public HookEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_STUCK, false);
        builder.define(PLAYER, Optional.of(UUID.randomUUID()));
        builder.define(STUCK_BLOCK_POS, BlockPos.ZERO);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
//        super.onHit(result);
        entityData.set(IS_STUCK, true);
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(result.getLocation());
    }


    @Override
    public void tick() {
        super.tick();

        var isStale = new AtomicBoolean(true);
        RopesMod.LOOKUP.iterable().forEach(p -> {
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
        var sublevel = Sable.HELPER.getContaining(this);
//        if (level().isClientSide) return;

        if (sublevel == null) {
            if (this.entityData.get(IS_STUCK)) {
                this.setDeltaMovement(Vec3.ZERO);
                AtomicBoolean flag = new AtomicBoolean(false);
                var level = level();
                var col = level.getBlockCollisions(this, this.getBoundingBox());
                col.forEach(voxelShape -> {
                    var pos = voxelShape.bounds().getCenter();
                    var bPos = BlockPos.containing(pos.x, pos.y, pos.z);
                    var block = level.getBlockState(bPos);
                    if (!block.is(Blocks.AIR) && !flag.get()) {
                        flag.set(true);
                    }
                });

                if (!flag.get()) {
                        entityData.set(IS_STUCK, false);
                }
                entityData.get(PLAYER).ifPresent(uuid -> {
                    var player = level.getPlayerByUUID(uuid);
                    if (player == null) return;
                    var factor = Math.log(player.position().distanceTo(this.position()) / goalDistance);
                    player.addDeltaMovement(this.position().subtract(player.position()).normalize().scale(factor * .1f));
                });

            } else {
//                    this.addDeltaMovement(new Vec3(0, -0.2, 0));
                var level = level();
//                var col = level.getBlockCollisions(this, this.getBoundingBox());
//                AtomicBoolean flag = new AtomicBoolean(false);
//                col.forEach(voxelShape -> {
//                    var pos = voxelShape.bounds().getCenter();
//                    var bPos = BlockPos.containing(pos.x, pos.y, pos.z);
//                    var block = level.getBlockState(bPos);
//                    if (!block.is(Blocks.AIR) && !flag.get()) {
//                        flag.set(true);
//                        entityData.set(STUCK_BLOCK_POS, bPos);
//                        entityData.set(IS_STUCK, true);
//                        entityData.get(PLAYER).ifPresent(uuid -> {
//                            var player = level.getPlayerByUUID(uuid);
//                            if (player == null) return;
//                            player.swing(InteractionHand.MAIN_HAND);
//                            player.addDeltaMovement(this.position().subtract(player.position()).normalize().scale(.2f));
//                        });
////                            System.out.printf("Found a block to stick to! %s%n", bPos);
//                    }
//                });
            }

        } else {

            // Check if we're already stuck
            if (this.entityData.get(IS_STUCK)) {
                this.setDeltaMovement(Vec3.ZERO);
                var level = level();
//                if (level.isClientSide) return;

                if (!(this.level() instanceof final SubLevelContainerHolder holder)) {
                    return;
                }

                entityData.get(PLAYER).ifPresent(uuid -> {
                    var player = level.getPlayerByUUID(uuid);
                    if (player == null) return;
                    var fixedPos = player.position().subtract(Sable.HELPER.projectOutOfSubLevel(level, position()));
                    var factor = Math.log(player.position().distanceTo(Sable.HELPER.projectOutOfSubLevel(level, this.position())) / goalDistance);
                    player.addDeltaMovement(fixedPos.normalize().scale(factor * .1f).reverse());
//                        System.out.printf("Testing: \n    %s\n    %s\n", sublevel.logicalPose().transformPosition(player.position()), position());
                    if (level.isClientSide) return;
                    if (!(sublevel instanceof ServerSubLevel serverSubLevel)) return;
                    var plotC = holder.sable$getPlotContainer();
                    if (!(plotC instanceof final ServerSubLevelContainer serverContainer)) return;
                    var phys = serverContainer.physicsSystem();
                    var handle = phys.getPhysicsHandle(serverSubLevel);
                    handle.applyImpulseAtPoint(
                            position(),
                            (
                                    sublevel.logicalPose().transformPositionInverse(player.position())
                            ).subtract(
                                    position()
                            ).normalize().scale(.2f * factor).yRot((float) Math.PI / 4f * 0));
//                        handle.applyImpulseAtPoint(
//                                position(),
//                                new Vec3(0, 0, .1f)
//                        );
                });

            } else {
                // If we're unstuck but in a sublevel, that's a problem
                EntitySubLevelUtil.kickEntity(sublevel, this);

            }
        }

    }

    public static class Renderer extends EntityRenderer<HookEntity> {
        public static final RenderType RENDER_TYPE = RenderType.entityCutout(ResourceLocation.parse("ropesnladders:hook"));
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
                poseStack.pushPose();
                poseStack.scale(0.5F, 0.5F, 0.5F);
                poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
                PoseStack.Pose posestack$pose = poseStack.last();
                VertexConsumer vertexconsumer = bufferSource.getBuffer(RENDER_TYPE);
                vertex(vertexconsumer, posestack$pose, packedLight, 0.0F, 0, 0, 1);
                vertex(vertexconsumer, posestack$pose, packedLight, 1.0F, 0, 1, 1);
                vertex(vertexconsumer, posestack$pose, packedLight, 1.0F, 1, 1, 0);
                vertex(vertexconsumer, posestack$pose, packedLight, 0.0F, 1, 0, 0);
                poseStack.popPose();
//                var lineBuffer = bufferSource.getBuffer(RenderType.lineStrip());
//                var pose = poseStack.last();
//                lineBuffer.addVertex((float)p_entity.position().x, (float)p_entity.position().y, (float)p_entity.position().z, 0xFFFFFFFF, 0, 0, 0, packedLight, 0, 0, 0);
//                lineBuffer.addVertex(0, 0, 0, 0xFFFFFFFF, 0, 0, 1, packedLight, 0, 0, 0);
                LaunchedPlungerEntityRenderer.renderRope(
                        List.of(
                                Vec3.ZERO, new Vec3(0, .1, 0), new Vec3(0, .2, 0), new Vec3(0, .3, 0)
                        ), bufferSource, p_entity.level(), poseStack
                );
                poseStack.popPose();
            }
        }
        private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight, float x, float y, int u, int v) {
            consumer.addVertex(pose, x - 0.5F, (float)y - 0.5F, 0.0F).setColor(-1).setUv((float)u, (float)v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
    }
}
