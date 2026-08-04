package me.kall.narutotv.impl.screen;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.impl.config.NarutoConfig;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NarutoGuiScreen extends AbstractNarutoScreen {
    static final Component FADABLE = Component.translatable("checkbox.narutotv.fadable");
    static final Component MUTE_MUSIC = Component.translatable("checkbox.narutotv.mute_music");

    static final Component TICKS = Component.translatable("box.narutotv.ticks");

    EditBox ticksBox;

    Checkbox fadableCheck, muteMusicCheck;

    String video, audio;

    public NarutoGuiScreen(@Nullable Screen lastScreen, @NotNull MediaArgs mediaArgs) {
        super(SCREEN, lastScreen);
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
    protected void initWidgets() {
        int centerX = this.width / 2;

        this.videoBox = this.initEditBox(centerX, VIDEO, this.video);
        this.audioBox = this.initEditBox(centerX, AUDIO, this.audio);
        this.ticksBox = this.initEditBox(centerX, TICKS, String.valueOf(NarutoConfig.ticksBeforeFade()));
        this.volumeBox = this.initEditBox(centerX, VOLUME, String.valueOf(NarutoConfig.volume()));
        this.volumeBox.setFilter(NUMERIC);

        this.initMaxSizeBoxes(centerX);

        this.initCheckboxes(centerX);
        this.initRandomButton(centerX);
        this.initSwapButton(centerX, NarutoGuiCenter::swap);
        this.initConfirmButtons(centerX);
    }

    void initCheckboxes(int centerX) {
        int fadableWidth = this.font.width(FADABLE) + 24;
        int muteMusicWidth = this.font.width(MUTE_MUSIC) + 24;

        this.fadableCheck = new Checkbox(centerX - BUTTON_WIDTH - 5, this.currentY, fadableWidth, BUTTON_HEIGHT, FADABLE, NarutoConfig.fadable()) {
            @Override
            public void onPress() {
                super.onPress();
                NarutoConfig.fadable(this.selected());
            }
        };
        this.addRenderableWidget(this.fadableCheck);

        this.muteMusicCheck = new Checkbox(centerX + 5, this.currentY, muteMusicWidth, BUTTON_HEIGHT, MUTE_MUSIC, NarutoConfig.muteMusic()) {
            @Override
            public void onPress() {
                super.onPress();
                NarutoConfig.muteMusic(this.selected());
            }
        };
        this.addRenderableWidget(this.muteMusicCheck);

        this.currentY += BUTTON_HEIGHT + 5;
    }

    @Override
    protected void onRandom() {
        Sources.noLineCut();

        NarutoGuiCenter.getActive().shutdown();
        NarutoGuiCenter.getActive().setup(0D);

        MediaArgs mediaArgs = NarutoGuiCenter.getActive().mediaArgs;
        assert mediaArgs != null;

        this.videoBox.setValue(mediaArgs.absVideoPath());
        this.audioBox.setValue(mediaArgs.absAudioPath());
    }

    @Override
    protected void onDone() {
        String videoBox = this.videoBox.getValue();
        String audioBox = this.audioBox.getValue();
        int maxWidthBox = Integer.parseInt(this.maxWidthBox.getValue());
        int maxHeightBox = Integer.parseInt(this.maxHeightBox.getValue());

        MediaArgs current = NarutoGuiCenter.getActive().mediaArgs;

        if (current != null) {
            this.video = current.absVideoPath();
            this.audio = current.absAudioPath();
        }

        NarutoConfig.ticksBeforeFade(Integer.parseInt(this.ticksBox.getValue()));

        if (NarutoConfig.volume(Double.parseDouble(this.volumeBox.getValue()))) NarutoGuiCenter.getActive().setVolume(NarutoConfig.volume());

        boolean maxWidthChanged = NarutoConfig.maxWidth(maxWidthBox);
        boolean maxHeightChanged = NarutoConfig.maxHeight(maxHeightBox);

        if (!videoBox.equals(this.video) || !audioBox.equals(this.audio) || maxHeightChanged || maxWidthChanged) {
            Sources.cutInLine(videoBox, audioBox);
            NarutoGuiCenter.getActive().shutdown();
        }

        this.onClose();
    }

    @Override
    public void tick() {
        super.tick();
        this.ticksBox.tick();
    }

    @Override
    protected void onTitles(@NotNull GuiGraphics guiGraphics, int centerX) {
        guiGraphics.drawCenteredString(this.font, TICKS, centerX, this.ticksBox.getY() - 12, 0xFFFFFF);
    }

    @Override
    protected void onSwapNote(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.renderComponentTooltip(this.font, List.of(SWAP_TOOLTIP, NarutoGuiCenter.isImageRenderer() ? COMPATIBILITY : PERFORMANCE), mouseX, mouseY);
    }

    @Override
    protected void onClearFocused() {
        if (this.ticksBox.isFocused()) this.ticksBox.setValue("");
    }
}