package com.cukkoo.freecamera;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = FreeCameraClient.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeFreeCamera {
    public NeoForgeFreeCamera(IEventBus modEventBus) {
        FreeCameraClient.initialize(FMLPaths.CONFIGDIR.get());
        modEventBus.addListener(this::registerKeyMappings);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        FreeCameraClient.registerKeyMappings(event::register);
    }
}
