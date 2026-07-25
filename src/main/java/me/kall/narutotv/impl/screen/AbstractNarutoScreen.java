package me.kall.narutotv.impl.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

public abstract class AbstractNarutoScreen extends Screen {
    protected static final Component RANDOM = Component.translatable("button.narutotv.random");
    protected static final Component SWAP = Component.translatable("button.narutotv.swap");

    protected static final Component RANDOM_TOOLTIP = Component.translatable("tooltip.narutotv.random");
    protected static final Component SWAP_TOOLTIP = Component.translatable("tooltip.narutotv.swap");

    protected static final Component VIDEO = Component.translatable("box.narutotv.video");
    protected static final Component AUDIO = Component.translatable("box.narutotv.audio");
    protected static final Component VOLUME = Component.translatable("box.narutotv.volume");

    protected static final Component COMPATIBILITY = Component.translatable("mode.narutotv.compatibility").withStyle(ChatFormatting.GREEN);
    protected static final Component PERFORMANCE = Component.translatable("mode.narutotv.performance").withStyle(ChatFormatting.RED);

    protected static final int BOX_WIDTH = 200, BOX_HEIGHT = 20, BOX_SPACING = 18;
    protected static final int BUTTON_WIDTH = 80, BUTTON_HEIGHT = 20, BUTTON_SPACING = 5;

    protected static final Predicate<String> NUMERIC = s -> s.matches("-?\\d*\\.?\\d*");

    protected final @Nullable Screen lastScreen;

    protected EditBox videoBox, audioBox, volumeBox;
    protected Button randomButton, swapButton, yesButton, noButton;

    protected int currentY;

    protected AbstractNarutoScreen(Component title, @Nullable Screen lastScreen) {
        super(title);
        this.lastScreen = lastScreen;
    }

    @Override
    protected final void init() {
        this.currentY = 25;
        this.initWidgets();
    }

    protected abstract void initWidgets();

    protected EditBox initEditBox(int centerX, Component label, String initialValue) {
        EditBox box = new EditBox(this.font, centerX - BOX_WIDTH / 2, this.currentY, BOX_WIDTH, BOX_HEIGHT, label);
        box.setMaxLength(Integer.MAX_VALUE);
        box.setValue(initialValue);
        this.addRenderableWidget(box);
        this.currentY += BOX_HEIGHT + BOX_SPACING;
        return box;
    }

    protected void initRandomButton(int centerX) {
        this.randomButton = Button.builder(RANDOM, button -> this.onRandom())
                .bounds(centerX - BUTTON_WIDTH - 5, this.currentY, BUTTON_WIDTH * 2 + 10, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(this.randomButton);
        this.currentY += BUTTON_HEIGHT + BUTTON_SPACING;
    }

    protected void initSwapButton(int centerX, Runnable onSwap) {
        this.swapButton = Button.builder(SWAP, button -> onSwap.run())
                .bounds(centerX - BUTTON_WIDTH - 5, this.currentY, BUTTON_WIDTH * 2 + 10, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(this.swapButton);
        this.currentY += BUTTON_HEIGHT + BUTTON_SPACING;
    }

    protected void initConfirmButtons(int centerX) {
        this.yesButton = Button.builder(Component.translatable("gui.yes"), button -> this.onDone())
                .bounds(centerX - BUTTON_WIDTH - 5, this.currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(this.yesButton);

        this.noButton = Button.builder(Component.translatable("gui.no"), button -> this.onClose())
                .bounds(centerX + 5, this.currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(this.noButton);
    }

    protected abstract void onRandom();

    protected abstract void onDone();

    @Override
    public void tick() {
        super.tick();
        this.videoBox.tick();
        this.audioBox.tick();
        this.onTickBox();
    }

    protected void onTickBox() {}

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        guiGraphics.drawCenteredString(this.font, VIDEO, centerX, this.videoBox.getY() - 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, AUDIO, centerX, this.audioBox.getY() - 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, VOLUME, centerX, this.volumeBox.getY() - 12, 0xFFFFFF);
        this.onTitles(guiGraphics, centerX);

        if (this.randomButton.isHovered()) guiGraphics.renderTooltip(this.font, RANDOM_TOOLTIP, mouseX, mouseY);
        if (this.swapButton.isHovered()) this.onSwapNote(guiGraphics, mouseX, mouseY);

        long window = this.getMinecraft().getWindow().getWindow();

        boolean shift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean mouseRight = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (shift && mouseRight) {
            if (this.videoBox.isFocused()) this.videoBox.setValue("");
            if (this.audioBox.isFocused()) this.audioBox.setValue("");
            this.onClearFocused();
        }
    }

    protected void onTitles(GuiGraphics guiGraphics, int centerX) {}

    protected void onSwapNote(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.renderTooltip(this.font, SWAP_TOOLTIP, mouseX, mouseY);
    }

    protected void onClearFocused() {}

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.lastScreen);
        super.onClose();
    }
}