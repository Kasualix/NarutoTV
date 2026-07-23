package me.kall.narutotv.impl.screen;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.impl.config.NarutoConfig;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class NarutoGuiScreen extends Screen {
    static final Component SCREEN = Component.translatable("screen.narutotv.gui.title");

    static final Component RANDOM = Component.translatable("button.narutotv.random");
    static final Component SWAP = Component.translatable("button.narutotv.swap");

    static final Component FADABLE = Component.translatable("checkbox.narutotv.fadable");
    static final Component MUTE_MUSIC = Component.translatable("checkbox.narutotv.mute_music");

    static final Component RANDOM_TOOLTIP = Component.translatable("tooltip.narutotv.random");
    static final Component SWAP_TOOLTIP = Component.translatable("tooltip.narutotv.swap");

    static final Component VIDEO = Component.translatable("box.narutotv.video");
    static final Component AUDIO = Component.translatable("box.narutotv.audio");

    static final Component COMPATIBILITY = Component.translatable("mode.narutotv.compatibility").withStyle(ChatFormatting.GREEN);
    static final Component PERFORMANCE = Component.translatable("mode.narutotv.performance").withStyle(ChatFormatting.RED);

    final @Nullable Screen lastScreen;

    EditBox videoBox, audioBox;

    Button randomButton, swapButton, yesButton, noButton;
    Checkbox fadableCheckbox, muteMusicCheckbox;

    String video, audio;

    public NarutoGuiScreen(@Nullable Screen lastScreen, @NotNull MediaArgs mediaArgs) {
        super(SCREEN);
        this.lastScreen = lastScreen;
        this.video = mediaArgs.absVideoPath();
        this.audio = mediaArgs.absAudioPath();
    }

    public static void sync(String video, String audio) {
        if (Minecraft.getInstance().screen instanceof NarutoGuiScreen screen) {
            if (screen.videoBox != null) screen.videoBox.setValue(video);
            if (screen.audioBox != null) screen.audioBox.setValue(audio);
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        int boxWidth = 200;
        int boxHeight = 20;
        int boxSpacing = 18;

        int currentY = 50;

        this.videoBox = new EditBox(this.font, centerX - boxWidth / 2, currentY, boxWidth, boxHeight, VIDEO);
        this.videoBox.setMaxLength(Integer.MAX_VALUE);
        this.videoBox.setValue(this.video);
        this.addRenderableWidget(this.videoBox);

        currentY += boxHeight + boxSpacing;

        this.audioBox = new EditBox(this.font, centerX - boxWidth / 2, currentY, boxWidth, boxHeight, AUDIO);
        this.audioBox.setMaxLength(Integer.MAX_VALUE);
        this.audioBox.setValue(this.audio);
        this.addRenderableWidget(this.audioBox);

        currentY += boxHeight + boxSpacing;

        int buttonWidth = 80;
        int buttonHeight = 20;

        this.randomButton = Button.builder(RANDOM, button -> this.onRandom()).bounds(centerX - buttonWidth - 5, currentY, buttonWidth * 2 + 10, buttonHeight).build();
        this.addRenderableWidget(this.randomButton);

        currentY += buttonHeight + 5;

        this.swapButton = Button.builder(SWAP, button -> this.onSwap()).bounds(centerX - buttonWidth - 5, currentY, buttonWidth * 2 + 10, buttonHeight).build();
        this.addRenderableWidget(this.swapButton);

        currentY += buttonHeight + 5;

        int checkboxHeight = 20;
        int fadableWidth = this.font.width(FADABLE) + 24;
        int muteMusicWidth = this.font.width(MUTE_MUSIC) + 24;

        this.fadableCheckbox = new Checkbox(centerX - buttonWidth - 5, currentY, fadableWidth, checkboxHeight, FADABLE, NarutoConfig.Client.fadable()) {
            @Override
            public void onPress() {
                super.onPress();
                NarutoConfig.Client.toggleFadable();
            }
        };
        this.addRenderableWidget(this.fadableCheckbox);

        this.muteMusicCheckbox = new Checkbox(centerX + 5, currentY, muteMusicWidth, checkboxHeight, MUTE_MUSIC, NarutoConfig.Client.musicMuted()) {
            @Override
            public void onPress() {
                super.onPress();
                NarutoConfig.Client.toggleMuteMusic();
            }
        };
        this.addRenderableWidget(this.muteMusicCheckbox);

        currentY += checkboxHeight + 5;

        this.yesButton = Button.builder(Component.translatable("gui.yes"), button -> this.onDone()).bounds(centerX - buttonWidth - 5, currentY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.yesButton);

        this.noButton = Button.builder(Component.translatable("gui.no"), button -> this.onClose()).bounds(centerX + 5, currentY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.noButton);
    }

    void onRandom() {
        Sources.noLineCut();

        NarutoGuiCenter.getActive().shutdown();
        NarutoGuiCenter.getActive().setup(0D);

        MediaArgs mediaArgs = NarutoGuiCenter.getActive().mediaArgs();
        assert mediaArgs != null;

        this.videoBox.setValue(mediaArgs.absVideoPath());
        this.audioBox.setValue(mediaArgs.absAudioPath());
    }

    void onSwap() {
        NarutoGuiCenter.swap();
    }

    void onDone() {
        String videoBox = this.videoBox.getValue();
        String audioBox = this.audioBox.getValue();

        MediaArgs current = NarutoGuiCenter.getActive().mediaArgs();
        if (current != null) {
            this.video = current.absVideoPath();
            this.audio = current.absAudioPath();
        }

        if (!videoBox.equals(this.video) || !audioBox.equals(this.audio)) {
            Sources.cutInLine(videoBox, audioBox);
            NarutoGuiCenter.getActive().shutdown();
        }

        this.onClose();
    }

    @Override
    public void tick() {
        super.tick();
        this.videoBox.tick();
        this.audioBox.tick();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        guiGraphics.drawCenteredString(this.font, VIDEO, centerX, this.videoBox.getY() - 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, AUDIO, centerX, this.audioBox.getY() - 12, 0xFFFFFF);

        if (this.randomButton.isHovered()) guiGraphics.renderTooltip(this.font, RANDOM_TOOLTIP, mouseX, mouseY);

        if (this.swapButton.isHovered()) {
            boolean isCompatMode = NarutoGuiCenter.isImageRenderer();
            guiGraphics.renderComponentTooltip(this.font, List.of(SWAP_TOOLTIP, isCompatMode ? COMPATIBILITY : PERFORMANCE), mouseX, mouseY);
        }

        long window = this.getMinecraft().getWindow().getWindow();

        boolean shift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean mouseRight = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (shift && mouseRight) {
            if (this.videoBox.isFocused()) this.videoBox.setValue("");
            if (this.audioBox.isFocused()) this.audioBox.setValue("");
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.lastScreen);
        super.onClose();
    }
}
