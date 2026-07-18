package com.cukkoo.freecamera.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Frustum.class)
public interface FrustumAccessor {
    @Accessor("intersection")
    FrustumIntersection freecamera$intersection();

    @Accessor("matrix")
    Matrix4f freecamera$matrix();

    @Accessor("viewVector")
    Vector4f freecamera$viewVector();
}
