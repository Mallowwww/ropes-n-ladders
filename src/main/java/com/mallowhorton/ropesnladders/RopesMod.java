package com.mallowhorton.ropesnladders;

import com.mallowhorton.ropesnladders.entities.HookEntity;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.HashMap;
import java.util.UUID;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RopesMod.MODID)
public class RopesMod {
    public static final String MODID = "ropesnladders";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = new RopeRegistrate(MODID);
    public static final PlayerGrappleLookup LOOKUP = new PlayerGrappleLookup();

    public RopesMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        REGISTRATE.registerEventListeners(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModItems.register(modEventBus);
        ModEntities.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {


    }
    public static class PlayerGrappleLookup {
        public record Data(HookEntity entity, Vec3 hookPosition, boolean isHooked) {}

        private final HashMap<UUID, Data> map;
        private PlayerGrappleLookup() {
            map = new HashMap<>();
        }
        public void setHooked(UUID uuid, HookEntity entity) {
            map.put(uuid, new Data(entity, entity.position(), true));
        }
        public void setUnhooked(UUID uuid) {
            map.put(uuid, new Data(null, null, false));
        }
        public boolean isHooked(UUID uuid) {
            return map.getOrDefault(uuid, new Data(null, null, false)).isHooked;
        }
        public Vec3 hookPos(UUID uuid) {
            return map.getOrDefault(uuid, new Data(null, null, false)).hookPosition;
        }
        public HookEntity hookEntity(UUID uuid) {
            return map.getOrDefault(uuid, new Data(null, null, false)).entity;
        }
        public Iterable<Pair<UUID, HookEntity>> iterable() {
            return map.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue().entity)).filter(p -> p.getSecond() != null).toList();
        }
    }
}
