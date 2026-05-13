package com.mallowhorton.ropesnladders.items;

import com.mallowhorton.ropesnladders.ModEntities;
import com.mallowhorton.ropesnladders.RopesMod;
import com.mallowhorton.ropesnladders.entities.HookEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class GrappleItem extends Item {
    public GrappleItem() {
        super(new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
//        return super.use(level, player, usedHand);
        player.swing(usedHand);
//        System.out.println("Hit!");
        if (RopesMod.LOOKUP.isHooked(player.getUUID())) {
//            System.out.println("Already has hook");
            if (level.isClientSide)
                return InteractionResultHolder.success(player.getItemInHand(usedHand));

            RopesMod.LOOKUP.hookEntity(player.getUUID()).remove(Entity.RemovalReason.DISCARDED);
            RopesMod.LOOKUP.setUnhooked(player.getUUID());
            player.swing(usedHand);
            return InteractionResultHolder.success(player.getItemInHand(usedHand));
        }
//        System.out.println("Making new hook");

        var hook = ModEntities.HOOK_ENTITY.create(level);
        if (hook == null) {
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));
        }
//        System.out.println("Successfully created hook");
        level.addFreshEntity(hook);
        hook.getEntityData().set(HookEntity.PLAYER, Optional.of(player.getUUID()));
        hook.teleportTo(player.getX(), player.getY() + 1.5, player.getZ());
        var dir = player.getLookAngle();
        hook.addDeltaMovement(dir.scale(1f));
        if (!level.isClientSide)
            RopesMod.LOOKUP.setHooked(player.getUUID(), hook);
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }
}
