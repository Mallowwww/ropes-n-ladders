package com.mallowhorton.ropesnladders.mixins;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface PlayerMixin {

    @Accessor("jumping")
    boolean ropes_n_ladders$getJumping();
}
