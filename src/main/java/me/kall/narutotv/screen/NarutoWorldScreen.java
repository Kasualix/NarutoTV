package me.kall.narutotv.screen;


import me.kall.narutotv.app.FFmpeg;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.config.NarutoConfig;
import me.kall.narutotv.core.world.WallTV;
import me.kall.narutotv.data.file.GamePaths;
import me.kall.narutotv.data.file.Sources;
import me.kall.narutotv.data.world.ClientWalls;
import me.kall.narutotv.data.world.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.packet.WallCleanPacket;
import me.kall.narutotv.network.packet.WallUpdatePacket;
import me.kall.narutotv.produce.util.LifetimeController;
import me.kall.narutotv.produce.video.AbstractFrameProducer;
import me.kall.narutotv.util.AudioZipGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class NarutoWorldScreen extends AbstractNarutoScreen {
    static final Component LOCAL_SOUND = Component.translatable("checkbox.narutotv.local_sound");

    static final Component CLEAN = Component.translatable("button.narutotv.clean");
    static final Component CLEAN_TOOLTIP = Component.translatable("tooltip.narutotv.clean");

    static final Component SWAP_TOOLTIP_UNIVERSAL = Component.translatable("tooltip.narutotv.swap.universal");
    static final Component SWAP_TOOLTIP_COMPAT = Component.translatable("tooltip.narutotv.swap.compat");

    final Wall wall;

    Checkbox localSoundCheck;

    Button cleanButton;

    final AtomicBoolean doing = new AtomicBoolean();

    public NarutoWorldScreen(@Nullable Screen lastScreen, Wall wall) {
        super(AbstractNarutoScreen.SCREEN, lastScreen);
        this.wall = wall;
    }

    @Override
    protected void initWidgets() {
        int centerX = this.width / 2;

        this.videoBox = this.initEditBox(centerX, VIDEO, GamePaths.absConfig(this.wall.video));
        this.audioBox = this.initEditBox(centerX, AUDIO, GamePaths.absConfig(this.wall.audio));
        this.volumeBox = this.initEditBox(centerX, VOLUME, String.valueOf(this.wall.volume));
        this.volumeBox.setFilter(NUMERIC);

        this.initMaxSizeBoxes(centerX);

        this.initCheckboxes(centerX);

        this.initRandomButton(centerX);
        this.initSwapButton(centerX, ClientWalls::swap);
        this.initCleanButton(centerX);
        this.initConfirmButtons(centerX);
    }

    void initCheckboxes(int centerX) {
        int localSoundWidth = this.font.width(LOCAL_SOUND) + 24;

        this.localSoundCheck = new Checkbox(centerX - BUTTON_WIDTH - 5, this.currentY, localSoundWidth, BUTTON_HEIGHT, LOCAL_SOUND, this.wall.hasLocalSound());
        this.addRenderableWidget(this.localSoundCheck);

        this.currentY += BUTTON_HEIGHT + 5;
    }

    void initCleanButton(int centerX) {
        this.cleanButton = Button.builder(CLEAN, button -> NarutoPackets.INSTANCE.sendToServer(new WallCleanPacket(this.wall)))
                .bounds(centerX - BUTTON_WIDTH - 5, this.currentY, BUTTON_WIDTH * 2 + 10, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(this.cleanButton);
        this.currentY += BUTTON_HEIGHT + BUTTON_SPACING;
    }

    @Override
    protected void onRandom() {
        MediaArgs mediaArgs = Sources.random(true);
        this.videoBox.setValue(mediaArgs.absVideoPath());
        this.audioBox.setValue(mediaArgs.absAudioPath());
    }

    @Override
    protected void onDone() {
        if (this.doing.get()) return;
        this.doing.set(true);
        String absVideoBox = this.videoBox.getValue();
        String absAudioBox = this.audioBox.getValue();
        boolean localSoundCheck = this.localSoundCheck.selected();

        int maxWidthBox = Integer.parseInt(this.maxWidthBox.getValue());
        int maxHeightBox = Integer.parseInt(this.maxHeightBox.getValue());

        float newVolume = Float.parseFloat(this.volumeBox.getValue().trim());

        WallTV<?> renderer = ClientWalls.get(this.wall);
        assert renderer != null;

        MediaArgs mediaArgs = renderer.mediaArgs;
        assert mediaArgs != null;

        String videoNow = mediaArgs.absVideoPath();
        String audioNow = mediaArgs.absAudioPath();

        boolean sourceChanged = !absVideoBox.equals(videoNow) || !absAudioBox.equals(audioNow);
        boolean volumeChanged = newVolume != renderer.getVolume();
        boolean localSoundChanged = localSoundCheck != this.wall.hasLocalSound();
        boolean soundChanged = localSoundChanged || volumeChanged;
        boolean sizeChanged = maxWidthBox != NarutoConfig.maxWidth() || maxHeightBox != NarutoConfig.maxHeight();

        if (!soundChanged && !sourceChanged && !sizeChanged) {
            this.onClose();
            return;
        }

        if (volumeChanged && !sourceChanged && !localSoundChanged && !sizeChanged) {
            this.wall.volume = newVolume;
            renderer.setVolume(newVolume);
            NarutoPackets.INSTANCE.sendToServer(new WallUpdatePacket(this.wall));
            this.doing.set(false);
            this.onClose();
            return;
        }

        NarutoConfig.maxWidth(maxWidthBox);
        NarutoConfig.maxHeight(maxHeightBox);

        AbstractFrameProducer<?> video = renderer.video;
        if (video != null) {
            LifetimeController life = video.life();
            if (life != null) life.pause();
        }

        NarutoWorldScreen.getAudioFuture(localSoundCheck, absAudioBox)
                .whenCompleteAsync((converted, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace(System.err);
                        throw new RuntimeException(throwable);
                    }

                    if (localSoundCheck) {
                        AudioZipGenerator.get(converted).generate((id) -> this.applyDone(absVideoBox, absAudioBox, id, newVolume));
                    } else {
                        this.applyDone(absVideoBox, absAudioBox, Wall.NO_LOCAL_SOUND, newVolume);
                    }
                }, this.minecraft);
    }

    private static CompletableFuture<@Nullable String> getAudioFuture(boolean localSound, String absAudioPath) {
        return localSound ? FFmpeg.toMonoOgg(absAudioPath) : CompletableFuture.completedFuture(absAudioPath);
    }

    private void applyDone(String absVideo, String absAudio, ResourceLocation localSound, float newVolume) {
        this.wall.video = GamePaths.relConfig(absVideo);
        this.wall.audio = GamePaths.relConfig(absAudio);
        this.wall.localSound = localSound;
        this.wall.volume = newVolume;

        NarutoPackets.INSTANCE.sendToServer(new WallUpdatePacket(this.wall));

        ClientWalls.add(this.wall).ifPresent(outdated -> {
            WallTV<?> latest = ClientWalls.get(this.wall);
            assert latest != null;
            latest.mediaArgs = Sources.get(absVideo);
            outdated.shutdownEntire(false);
        });

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
    protected void onSwapNote(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.renderComponentTooltip(this.font, List.of(SWAP_TOOLTIP, SWAP_TOOLTIP_UNIVERSAL, SWAP_TOOLTIP_COMPAT, ClientWalls.isCompatMode() ? COMPATIBILITY : PERFORMANCE), mouseX, mouseY);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.cleanButton.isHovered()) guiGraphics.renderTooltip(this.font, CLEAN_TOOLTIP, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}