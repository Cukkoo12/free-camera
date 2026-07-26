package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import com.cukkoo.freecamera.render.CameraRenderEffectsBridge;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameRenderer.class)
public abstract class WorldRenderViewMixin {
    @ModifyArg(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZLnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V"
            ),
            index = 4
    )
    private Matrix4fc freecamera$supplyCanonicalWorldView(Matrix4fc vanillaView) {
        CameraRenderEffectsBridge bridge = FreeCameraClient.renderEffectsBridgeOrNull();
        return bridge == null ? vanillaView : bridge.selectCanonicalWorldView(vanillaView);
    }
}
