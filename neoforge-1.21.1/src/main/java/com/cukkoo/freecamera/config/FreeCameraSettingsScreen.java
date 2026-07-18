package com.cukkoo.freecamera.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class FreeCameraSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int LABEL_X = 40;
    private static final int CONTROL_X = 200;
    private static final int CONTROL_W = 160;

    private final Screen parent;
    private final FreeCameraConfig config;
    private int yPos;

    public FreeCameraSettingsScreen(Screen parent) {
        super(Component.translatable("screen.freecamera.settings"));
        this.parent = parent;
        this.config = FreeCameraConfig.getInstance();
    }

    @Override
    protected void init() {
        yPos = 36;

        addHeader("Free Camera Settings");

        yPos += 6;
        addSection("Flight");
        addSlider("Flight Speed",     config.flightSpeedMultiplier,    0.1f, 10.0f, 0.05f, v -> config.flightSpeedMultiplier = v,     "%.2fx");
        addSlider("Scroll Speed",     config.scrollSpeedFactor,        1.05f, 3.0f, 0.05f, v -> config.scrollSpeedFactor = v,         "%.2fx");
        addToggle("Show Speed",       config.showSpeedInActionBar,     v -> config.showSpeedInActionBar = v);

        yPos += 4;
        addSection("Camera Control");
        addSlider("Mouse Sens.",      config.mouseSensitivity,         0.1f,  5.0f, 0.05f, v -> config.mouseSensitivity = v,         "%.2fx");
        addSlider("Orbit Sens.",      config.orbitSensitivity,         0.05f, 5.0f, 0.05f, v -> config.orbitSensitivity = v,         "%.2fx");
        addToggle("Invert Orbit Y",   config.invertOrbitY,             v -> config.invertOrbitY = v);
        addSlider("Roll Increment",   config.rollIncrement,            0.5f, 45.0f, 0.5f, v -> config.rollIncrement = v,             "%.1f°");

        yPos += 4;
        addSection("Camera");
        addSlider("Smoothing",        config.cinematicSmoothingFactor, 0.01f, 0.50f, 0.01f, v -> config.cinematicSmoothingFactor = v, "%.3f");
        addSlider("Zoom Factor",      config.zoomFactor,               0.05f, 1.0f, 0.05f, v -> config.zoomFactor = v,               "%.2fx");

        yPos += 10;
        int cx = (width - 340) / 2;
        addRenderableWidget(Button.builder(
            Component.translatable("screen.freecamera.reset_defaults"),
            btn -> resetDefaults()
        ).bounds(cx, yPos, 160, 20).build());
        addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            btn -> onClose()
        ).bounds(cx + 180, yPos, 160, 20).build());
    }

    private int ctrlX() {
        return ((width - 340) / 2) + CONTROL_X;
    }

    private int labelX() {
        return ((width - 340) / 2) + LABEL_X;
    }

    private void addHeader(String text) {
        int cx = (width - 340) / 2;
        addRenderableWidget(new HeaderLabel(cx, yPos, 340, 24, text));
        yPos += 28;
    }

    private void addSection(String text) {
        int cx = (width - 340) / 2;
        addRenderableWidget(new SectionLabel(cx, yPos, 340, 18, text));
        yPos += 20;
    }

    private void addSlider(String label, float current,
                           float min, float max, float step,
                           Consumer<Float> onChange, String valueFormat) {
        var slider = new FloatSlider(ctrlX(), yPos, CONTROL_W, 20, label, current,
                                      min, max, step, onChange, valueFormat);
        addRenderableWidget(slider);
        addRenderableWidget(new SettingLabel(labelX(), yPos, CONTROL_X - LABEL_X - 4, 20, label));
        yPos += ROW_HEIGHT;
    }

    private void addToggle(String label, boolean current,
                           Consumer<Boolean> onChange) {
        boolean[] state = {current};
        var btn = Button.builder(
            Component.literal(state[0] ? "ON" : "OFF"),
            b -> {
                state[0] = !state[0];
                onChange.accept(state[0]);
                b.setMessage(Component.literal(state[0] ? "ON" : "OFF"));
            }
        ).bounds(ctrlX(), yPos, CONTROL_W, 20).build();
        addRenderableWidget(btn);
        addRenderableWidget(new SettingLabel(labelX(), yPos, CONTROL_X - LABEL_X - 4, 20, label));
        yPos += ROW_HEIGHT;
    }

    private void resetDefaults() {
        FreeCameraConfig defaults = new FreeCameraConfig();
        config.flightSpeedMultiplier    = defaults.flightSpeedMultiplier;
        config.mouseSensitivity         = defaults.mouseSensitivity;
        config.cinematicSmoothingFactor = defaults.cinematicSmoothingFactor;
        config.orbitSensitivity         = defaults.orbitSensitivity;
        config.zoomFactor               = defaults.zoomFactor;
        config.rollIncrement            = defaults.rollIncrement;
        config.scrollSpeedFactor        = defaults.scrollSpeedFactor;
        config.invertOrbitY             = defaults.invertOrbitY;
        config.showSpeedInActionBar     = defaults.showSpeedInActionBar;
        this.rebuildWidgets();
    }

    @Override
    public void onClose() {
        config.save();
        if (parent != null) {
            this.minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static class HeaderLabel extends AbstractWidget {
        private final String text;

        HeaderLabel(int x, int y, int w, int h, String text) {
            super(x, y, w, h, Component.empty());
            this.text = text;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mx, int my, float pt) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, getX() + this.width / 2, getY() + 6, 0xffffff);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) { }
    }

    private static class SectionLabel extends AbstractWidget {
        private final String text;

        SectionLabel(int x, int y, int w, int h, String text) {
            super(x, y, w, h, Component.empty());
            this.text = text;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mx, int my, float pt) {
            int col = 0x55aaff;
            Font font = Minecraft.getInstance().font;
            guiGraphics.drawString(font, text, getX() + 2, getY() + 4, col, false);
            guiGraphics.hLine(getX() + font.width(text) + 6, getX() + this.width, getY() + 9, col);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) { }
    }

    private static class SettingLabel extends AbstractWidget {
        private final String text;

        SettingLabel(int x, int y, int w, int h, String text) {
            super(x, y, w, h, Component.empty());
            this.text = text;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mx, int my, float pt) {
            guiGraphics.drawString(Minecraft.getInstance().font, text, getX(), getY() + 6, 0xc0c0c0, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) { }
    }

    private static class FloatSlider extends AbstractSliderButton {
        private final String label;
        private final float min;
        private final float max;
        private final float step;
        private final Consumer<Float> onChange;
        private final String valueFormat;

        FloatSlider(int x, int y, int w, int h, String label, float current,
                    float min, float max, float step,
                    Consumer<Float> onChange, String valueFormat) {
            super(x, y, w, h, Component.empty(), doubleFrom(current, min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.onChange = onChange;
            this.valueFormat = valueFormat;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(formatValue(getValue())));
        }

        @Override
        protected void applyValue() {
            onChange.accept(getValue());
        }

        private float getValue() {
            float raw = min + (float) this.value * (max - min);
            if (step <= 0) return raw;
            return Math.round(raw / step) * step;
        }

        private static double doubleFrom(float v, float min, float max) {
            if (max <= min) return 0;
            return (v - min) / (max - min);
        }

        private String formatValue(float v) {
            return String.format(valueFormat, v);
        }
    }
}
