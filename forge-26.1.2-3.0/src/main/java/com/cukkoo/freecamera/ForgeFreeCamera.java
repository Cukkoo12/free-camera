package com.cukkoo.freecamera;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(FreeCameraClient.MOD_ID)
public final class ForgeFreeCamera {
    public ForgeFreeCamera(FMLJavaModLoadingContext context) {
        FreeCameraClient.initialize(FMLPaths.CONFIGDIR.get());
        RegisterKeyMappingsEvent.BUS
                .addListener(event -> FreeCameraClient.registerKeyMappings(event::register));
    }
}
