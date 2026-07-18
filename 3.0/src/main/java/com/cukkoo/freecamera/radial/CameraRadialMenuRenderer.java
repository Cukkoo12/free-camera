package com.cukkoo.freecamera.radial;

import com.cukkoo.freecamera.state.CameraStateMachine;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CameraRadialMenuRenderer {
    private static final int OUTER_RADIUS = 76;
    private static final int INNER_RADIUS = 25;
    private static final int OVERLAY_COLOR = 0x66000000;
    private static final int NORMAL_COLOR = 0xB0282828;
    private static final int CURRENT_COLOR = 0xC0445464;
    private static final int HOVER_COLOR = 0xE078A8D8;
    private static final int CENTER_COLOR = 0xDC181818;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final Component CANCEL_LABEL = Component.translatable("radial.free-camera.cancel");

    private final CameraRadialMenuState state;
    private final CameraStateMachine stateMachine;

    public CameraRadialMenuRenderer(
            CameraRadialMenuState state,
            CameraStateMachine stateMachine
    ) {
        this.state = state;
        this.stateMachine = stateMachine;
    }

    public void register() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("free-camera", "camera_radial_menu"),
                this::extractRenderState
        );
    }

    private void extractRenderState(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker tracker) {
        if (!state.isOpen()) {
            return;
        }
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        graphics.nextStratum();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), OVERLAY_COLOR);
        drawWheel(graphics, centerX, centerY);
        drawLabels(graphics, centerX, centerY);
        graphics.centeredText(Minecraft.getInstance().font, CANCEL_LABEL,
                centerX, centerY - Minecraft.getInstance().font.lineHeight / 2, TEXT_COLOR);
    }

    private void drawWheel(GuiGraphicsExtractor graphics, int centerX, int centerY) {
        int outerSquared = OUTER_RADIUS * OUTER_RADIUS;
        int innerSquared = INNER_RADIUS * INNER_RADIUS;
        for (int y = -OUTER_RADIUS; y <= OUTER_RADIUS; y++) {
            int runStart = Integer.MIN_VALUE;
            int runColor = 0;
            for (int x = -OUTER_RADIUS; x <= OUTER_RADIUS + 1; x++) {
                int radiusSquared = x * x + y * y;
                int color = 0;
                if (x <= OUTER_RADIUS && radiusSquared <= outerSquared
                        && radiusSquared >= innerSquared) {
                    int entryIndex = CameraRadialEntry.indexForVector(x, y);
                    color = colorForEntry(entryIndex);
                }
                if (color != runColor) {
                    if (runColor != 0) {
                        graphics.fill(centerX + runStart, centerY + y,
                                centerX + x, centerY + y + 1, runColor);
                    }
                    runStart = x;
                    runColor = color;
                }
            }
        }
        for (int y = -INNER_RADIUS; y <= INNER_RADIUS; y++) {
            int halfWidth = (int) Math.sqrt(INNER_RADIUS * INNER_RADIUS - y * y);
            graphics.fill(centerX - halfWidth, centerY + y,
                    centerX + halfWidth + 1, centerY + y + 1, CENTER_COLOR);
        }
    }

    private int colorForEntry(int entryIndex) {
        if (entryIndex == state.highlightedIndex()) {
            return HOVER_COLOR;
        }
        CameraRadialEntry entry = CameraRadialEntry.at(entryIndex);
        return entry != null && entry.mode() == stateMachine.activeModeOrNull()
                ? CURRENT_COLOR
                : NORMAL_COLOR;
    }

    private static void drawLabels(GuiGraphicsExtractor graphics, int centerX, int centerY) {
        for (int index = 0; index < CameraRadialEntry.count(); index++) {
            CameraRadialEntry entry = CameraRadialEntry.at(index);
            int x = centerX + (int) Math.round(entry.directionX() * 52.0);
            int y = centerY + (int) Math.round(entry.directionY() * 52.0)
                    - Minecraft.getInstance().font.lineHeight / 2;
            graphics.centeredText(Minecraft.getInstance().font, entry.label(), x, y, TEXT_COLOR);
        }
    }
}
