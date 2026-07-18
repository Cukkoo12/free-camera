package com.cukkoo.freecamera.render;

import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class Minecraft261RenderPipelineDevelopmentChecks {
    private static final String GAME_RENDERER = "net/minecraft/client/renderer/GameRenderer";
    private static final String LEVEL_RENDERER = "net/minecraft/client/renderer/LevelRenderer";

    private Minecraft261RenderPipelineDevelopmentChecks() {
    }

    public static void main(String[] args) throws IOException {
        ClassNode gameRenderer = readClass(GAME_RENDERER);
        ClassNode levelRenderer = readClass(LEVEL_RENDERER);
        verifyGameRendererWorldAndHudBoundary(gameRenderer);
        verifyChunkSnapshotCapture(levelRenderer);
        verifySharedWorldPasses(levelRenderer);
        System.out.println("Minecraft 26.1.2 render-pipeline target checks passed.");
    }

    private static void verifyGameRendererWorldAndHudBoundary(ClassNode owner) {
        MethodNode renderLevel = requireMethod(owner, "renderLevel");
        int viewField = findField(renderLevel, "net/minecraft/client/renderer/state/level/CameraRenderState",
                "viewRotationMatrix");
        int levelRenderCall = findCall(renderLevel, LEVEL_RENDERER, "renderLevel");
        int hudProjection = findField(renderLevel, GAME_RENDERER, "hudProjection");
        require(viewField >= 0 && levelRenderCall > viewField,
                "GameRenderer does not pass its captured camera view into LevelRenderer");
        require(hudProjection > levelRenderCall,
                "HUD projection is not separated after the world-render call");
    }

    private static void verifyChunkSnapshotCapture(ClassNode owner) {
        MethodNode extractLevel = requireMethod(owner, "extractLevel");
        int cameraView = findCall(extractLevel, "net/minecraft/client/Camera", "getViewRotationMatrix");
        int prepareChunks = findCall(extractLevel, LEVEL_RENDERER, "prepareChunkRenders");
        require(cameraView >= 0 && prepareChunks > cameraView,
                "Chunk preparation no longer captures a Camera view matrix during extractLevel");

        MethodNode prepareChunkRenders = requireMethod(owner, "prepareChunkRenders");
        int matrixCopy = findConstructor(
                prepareChunkRenders,
                "org/joml/Matrix4f",
                "(Lorg/joml/Matrix4fc;)V"
        );
        int chunkUniform = findCall(prepareChunkRenders,
                "net/minecraft/client/renderer/DynamicUniforms$ChunkSectionInfo", "<init>");
        require(matrixCopy >= 0 && chunkUniform > matrixCopy,
                "Chunk section uniforms no longer snapshot the supplied model-view matrix");
    }

    private static void verifySharedWorldPasses(ClassNode owner) {
        MethodNode renderLevel = requireMethod(owner, "renderLevel");
        int modelViewStack = findCall(renderLevel,
                "com/mojang/blaze3d/systems/RenderSystem", "getModelViewStack");
        int multiplyWorldView = findCall(renderLevel, "org/joml/Matrix4fStack", "mul");
        int skyPass = findCall(renderLevel, LEVEL_RENDERER, "addSkyPass");
        int mainPass = findCall(renderLevel, LEVEL_RENDERER, "addMainPass");
        int cloudPass = findCall(renderLevel, LEVEL_RENDERER, "addCloudsPass");
        int weatherPass = findCall(renderLevel, LEVEL_RENDERER, "addWeatherPass");
        int executeFrame = findCall(renderLevel,
                "com/mojang/blaze3d/framegraph/FrameGraphBuilder", "execute");
        require(modelViewStack >= 0 && multiplyWorldView > modelViewStack,
                "LevelRenderer no longer installs its model-view argument globally");
        require(skyPass > multiplyWorldView && mainPass > multiplyWorldView
                        && cloudPass > multiplyWorldView && weatherPass > multiplyWorldView
                        && executeFrame > weatherPass,
                "Sky, main, cloud, or weather passes escaped the installed world view");

        MethodNode main = requireMethod(owner, "lambda$addMainPass$0");
        require(countCalls(main, "net/minecraft/client/renderer/chunk/ChunkSectionsToRender",
                        "renderGroup") == 2,
                "Opaque and translucent chunk groups are not both present");
        require(findCall(main, LEVEL_RENDERER, "submitEntities") >= 0,
                "Entity submission escaped the main world pass");
        require(findCall(main, LEVEL_RENDERER, "submitBlockEntities") >= 0,
                "Block-entity submission escaped the main world pass");
        require(findCall(main,
                        "net/minecraft/client/renderer/state/level/ParticlesRenderState", "submit") >= 0,
                "Particle submission escaped the main world pass");

        MethodNode sky = requireMethod(owner, "lambda$addSkyPass$0");
        MethodNode clouds = requireMethod(owner, "lambda$addCloudsPass$0");
        MethodNode weather = requireMethod(owner, "lambda$addWeatherPass$0");
        require(countOwnerCalls(sky, "net/minecraft/client/renderer/SkyRenderer") > 0,
                "Sky and celestial rendering escaped the installed world view");
        require(findCall(clouds, "net/minecraft/client/renderer/CloudRenderer", "render") >= 0,
                "Cloud rendering escaped the installed world view");
        require(findCall(weather,
                        "net/minecraft/client/renderer/WeatherEffectRenderer", "render") >= 0,
                "Weather rendering escaped the installed world view");
    }

    private static ClassNode readClass(String internalName) throws IOException {
        try (InputStream input = Minecraft261RenderPipelineDevelopmentChecks.class
                .getResourceAsStream('/' + internalName + ".class")) {
            if (input == null) {
                throw new AssertionError("Missing runtime class: " + internalName);
            }
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static MethodNode requireMethod(ClassNode owner, String name) {
        return owner.methods.stream().filter(method -> method.name.equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing method " + owner.name + '.' + name));
    }

    private static int findCall(MethodNode method, String owner, String name) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner) && call.name.equals(name)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static int countCalls(MethodNode method, String owner, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner) && call.name.equals(name)) {
                count++;
            }
        }
        return count;
    }

    private static int countOwnerCalls(MethodNode method, String owner) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)) {
                count++;
            }
        }
        return count;
    }

    private static int findField(MethodNode method, String owner, String name) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.owner.equals(owner) && field.name.equals(name)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static int findConstructor(MethodNode method, String owner, String descriptor) {
        int index = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)
                    && call.name.equals("<init>") && call.desc.equals(descriptor)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
