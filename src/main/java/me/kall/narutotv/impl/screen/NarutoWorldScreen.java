package me.kall.narutotv.impl.screen;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppInstances;
import me.kall.narutotv.base.data.Paths;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.impl.world.network.packet.ScreenCleanPacket;
import me.kall.narutotv.impl.world.network.packet.ScreenUpdatePacket;
import me.kall.narutotv.impl.world.util.AudioZipGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.kall.narutotv.impl.screen.NarutoGuiScreen.*;

public class NarutoWorldScreen extends Screen {
    static final Component LOCAL_SOUND = Component.translatable("checkbox.narutotv.local_sound");

    static final Component CLEAN = Component.translatable("button.narutotv.clean");

    static final Component CLEAN_TOOLTIP = Component.translatable("tooltip.narutotv.clean");

    static final Component SWAP_TOOLTIP_UNIVERSAL = Component.translatable("tooltip.narutotv.swap.universal");
    static final Component SWAP_TOOLTIP_COMPAT = Component.translatable("tooltip.narutotv.swap.compat");

    final BlockScreen blockScreen;

    EditBox videoBox, audioBox;

    Checkbox localSoundCheck;

    Button randomButton, swapButton, cleanButton, yesButton, noButton;

    int currentY;

    Screen lastScreen;

    final AtomicBoolean doing = new AtomicBoolean();

    public NarutoWorldScreen(Screen lastScreen, BlockScreen blockScreen) {
        super(SCREEN);
        this.blockScreen = blockScreen;
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        int boxWidth = 200;
        int boxHeight = 20;
        int boxSpacing = 18;

        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonSpacing = 5;

        int checkboxHeight = 20;
        int checkboxSpacing = 5;

        this.currentY = 25;

        this.editBoxes(centerX, boxWidth, boxHeight, boxSpacing);
        this.checkboxes(centerX, buttonWidth, checkboxHeight, checkboxSpacing);
        this.buttons(centerX, buttonWidth, buttonHeight, buttonSpacing);
    }

    void editBoxes(int centerX, int boxWidth, int boxHeight, int boxSpacing) {
        this.videoBox = new EditBox(this.font, centerX - boxWidth / 2, this.currentY, boxWidth, boxHeight, VIDEO);
        this.videoBox.setMaxLength(Integer.MAX_VALUE);
        this.videoBox.setValue(Paths.absolute(this.blockScreen.video));
        this.addRenderableWidget(this.videoBox);
        this.currentY += boxHeight + boxSpacing;

        this.audioBox = new EditBox(this.font, centerX - boxWidth / 2, this.currentY, boxWidth, boxHeight, AUDIO);
        this.audioBox.setMaxLength(Integer.MAX_VALUE);
        this.audioBox.setValue(Paths.absolute(this.blockScreen.audio));
        this.addRenderableWidget(this.audioBox);
        this.currentY += boxHeight + boxSpacing;
    }

    void checkboxes(int centerX, int buttonWidth, int checkboxHeight, int checkboxSpacing) {
        int localSoundWidth = this.font.width(LOCAL_SOUND) + 24;

        this.localSoundCheck = new Checkbox(centerX - buttonWidth - 5, this.currentY, localSoundWidth, checkboxHeight, LOCAL_SOUND, this.blockScreen.hasLocalSound());
        this.addRenderableWidget(this.localSoundCheck);

        this.currentY += checkboxHeight + checkboxSpacing;
    }

    void buttons(int centerX, int buttonWidth, int buttonHeight, int buttonSpacing) {
        this.randomButton = Button.builder(RANDOM, button -> this.onRandom())
                .bounds(centerX - buttonWidth - 5, this.currentY, buttonWidth * 2 + 10, buttonHeight)
                .build();
        this.addRenderableWidget(this.randomButton);
        this.currentY += buttonHeight + buttonSpacing;

        this.swapButton = Button.builder(SWAP, button -> ClientRenderers.swap())
                .bounds(centerX - buttonWidth - 5, this.currentY, buttonWidth * 2 + 10, buttonHeight)
                .build();
        this.addRenderableWidget(this.swapButton);
        this.currentY += buttonHeight + buttonSpacing;

        this.cleanButton = Button.builder(CLEAN, button -> NarutoPackets.INSTANCE.sendToServer(new ScreenCleanPacket(this.blockScreen)))
                .bounds(centerX - buttonWidth - 5, this.currentY, buttonWidth * 2 + 10, buttonHeight)
                .build();
        this.addRenderableWidget(this.cleanButton);
        this.currentY += buttonHeight + buttonSpacing;

        this.yesButton = Button.builder(Component.translatable("gui.yes"), button -> this.onDone())
                .bounds(centerX - buttonWidth - 5, this.currentY, buttonWidth, buttonHeight)
                .build();
        this.addRenderableWidget(this.yesButton);

        this.noButton = Button.builder(Component.translatable("gui.no"), button -> this.onClose())
                .bounds(centerX + 5, this.currentY, buttonWidth, buttonHeight)
                .build();
        this.addRenderableWidget(this.noButton);
    }

    void onRandom() {
        Sources.noLineCut();
        MediaArgs mediaArgs = Sources.get();
        this.videoBox.setValue(mediaArgs.absVideoPath());
        this.audioBox.setValue(mediaArgs.absAudioPath());
    }

    void onDone() {
        if (this.doing.get()) return;
        this.doing.set(true);
        String absVideoBox = this.videoBox.getValue();
        String absAudioBox = this.audioBox.getValue();
        boolean localSoundCheck = this.localSoundCheck.selected();

        AbstractRenderer<?> renderer = ClientRenderers.get(this.blockScreen);
        assert renderer != null;

        MediaArgs mediaArgs = renderer.mediaArgs();
        assert mediaArgs != null;

        String videoNow = mediaArgs.absVideoPath();
        String audioNow = mediaArgs.absAudioPath();

        boolean sourceChanged = !absVideoBox.equals(videoNow) || !absAudioBox.equals(audioNow);
        boolean soundChanged = localSoundCheck != this.blockScreen.hasLocalSound();

        if (!soundChanged && !sourceChanged) {
            this.onClose();
            return;
        }

        renderer.pause();

        NarutoWorldScreen.getAudioFuture(localSoundCheck, absAudioBox)
                .whenCompleteAsync((converted, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace(System.err);
                        throw new RuntimeException(throwable);
                    }

                    if (localSoundCheck) {
                        AudioZipGenerator.get(converted).generate((id) -> this.applyDone(absVideoBox, absAudioBox, id));
                    } else {
                        this.applyDone(absVideoBox, absAudioBox, BlockScreen.NO_LOCAL_SOUND);
                    }
                }, this.minecraft);
    }

    private static CompletableFuture<@Nullable String> getAudioFuture(boolean localSound, String absAudioPath) {
        return localSound ? AppInstances.ffmpeg().convertAudio(absAudioPath) : CompletableFuture.completedFuture(absAudioPath);
    }

    private void applyDone(String absVideo, String absAudio, ResourceLocation localSound) {
        this.blockScreen.video = Paths.relative(absVideo);
        this.blockScreen.audio = Paths.relative(absAudio);
        this.blockScreen.localSound = localSound;

        NarutoPackets.INSTANCE.sendToServer(new ScreenUpdatePacket(this.blockScreen));

        Sources.cutInLine(absVideo, absAudio);

        ClientRenderers.add(this.blockScreen).ifPresent(AbstractRenderer::shutdown);

        this.doing.set(false);
        this.onClose();
    }

    public static void sync(String video, String audio) {
        if (Minecraft.getInstance().screen instanceof NarutoWorldScreen screen) {
            if (screen.videoBox != null) screen.videoBox.setValue(video);
            if (screen.audioBox != null) screen.audioBox.setValue(audio);
        }
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
        if (this.swapButton.isHovered()) guiGraphics.renderComponentTooltip(this.font, List.of(SWAP_TOOLTIP, SWAP_TOOLTIP_UNIVERSAL, SWAP_TOOLTIP_COMPAT, ClientRenderers.isImageRenderer() ? COMPATIBILITY : PERFORMANCE), mouseX, mouseY);
        if (this.cleanButton.isHovered()) guiGraphics.renderTooltip(this.font, CLEAN_TOOLTIP, mouseX, mouseY);

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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}