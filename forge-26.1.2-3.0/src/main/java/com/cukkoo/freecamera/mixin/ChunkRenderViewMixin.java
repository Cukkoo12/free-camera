package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import com.cukkoo.freecamera.render.CameraRenderEffectsBridge;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public abstract class ChunkRenderViewMixin {
    @ModifyArg(
            method = "extractLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareChunkRenders(Lorg/joml/Matrix4fc;)Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"
            ),
            index = 0
    )
    private Matrix4fc freecamera$supplyCanonicalChunkView(Matrix4fc vanillaView) {
        CameraRenderEffectsBridge bridge = FreeCameraClient.renderEffectsBridgeOrNull();
        return bridge == null ? vanillaView : bridge.selectCanonicalWorldView(vanillaView);
    }
}
