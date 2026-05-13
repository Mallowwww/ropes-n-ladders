package com.mallowhorton.ropesnladders;

import com.mallowhorton.ropesnladders.items.GrappleItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, RopesMod.MODID);

    public static final DeferredHolder<Item, GrappleItem> GRAPPLE = ITEMS.register("grapple", GrappleItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
