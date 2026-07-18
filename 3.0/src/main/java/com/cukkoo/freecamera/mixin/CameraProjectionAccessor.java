package com.cukkoo.freecamera.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraProjectionAccessor {
    @Accessor("projection")
    Projection freecamera$projection();

    @Accessor("cullFrustum")
    Frustum freecamera$cullFrustum();
}
