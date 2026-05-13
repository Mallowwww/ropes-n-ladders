package com.mallowhorton.ropesnladders;

import com.mallowhorton.ropesnladders.entities.HookEntity;
import com.tterrag.registrate.util.entry.EntityEntry;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.service.SimEntityService;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final EntityEntry<HookEntity> HOOK_ENTITY = RopesMod.REGISTRATE.entity("hook", HookEntity::new, MobCategory.MISC)
            .transform((builder) -> SimEntityService.INSTANCE.loaderEntityTransform(builder,
                    new SimEntityTypes.EntityLoaderData(10, 5, 0.5f, 0.5f, 0.25f,true, true, false)))
            .renderer(() -> HookEntity.Renderer::new)
            .register();
    public static void init() {
        
    }
}
