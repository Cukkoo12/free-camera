package com.cukkoo.freecamera.integration;

import com.cukkoo.freecamera.FreeCameraClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class FreeCameraModMenuIntegration implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> FreeCameraClient.createSettingsScreen(parent);
    }
}
