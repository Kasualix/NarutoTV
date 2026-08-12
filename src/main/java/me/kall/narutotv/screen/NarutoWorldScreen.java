package me.kall.narutotv.screen;

import com.google.common.collect.Lists;
import me.kall.narutotv.app.FFmpeg;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.config.NarutoConfig;
import me.kall.narutotv.data.file.GamePaths;
import me.kall.narutotv.data.file.Sources;
import me.kall.narutotv.data.world.wall.ClientWalls;
import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.packet.wall.WallCleanPacket;
import me.kall.narutotv.network.packet.wall.WallUpdatePacket;
import me.kall.narutotv.util.AudioZipGenerator;
import me.kall.narutotv.world.WallTV;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class NarutoWorldScreen extends AbstractMediaScreen {
    static final Component LOCAL_SOUND = Component.translatable("checkbox.narutotv.local_sound");
    static final Component LIGHT = Component.translatable("checkbox.narutotv.light");

    static final Component CLEAN = Component.translatable("button.narutotv.clean");
    static final Component CLEAN_TOOLTIP = Component.translatable("tooltip.narutotv.clean");

    static final Component SWAP_TOOLTIP_UNIVERSAL = Component.translatable("tooltip.narutotv.swap.universal");
    static final Component SWAP_TOOLTIP_COMPAT = Component.translatable("tooltip.narutotv.swap.compat");

    static final Component LIGHT_TOOLTIP = Component.translatable("tooltip.narutotv.light");

    final Wall wall;

    Checkbox localSoundCheck, lightCheck;

    final AtomicBoolean doing = new AtomicBoolean();

    public NarutoWorldScreen(@Nullable Screen lastScreen, Wall wall) {
        super(SCREEN, lastScreen);
        this.wall = wall;
    }

    @Override
    protected String initialVideoPath() {
        return GamePaths.absConfig(this.wall.video);
    }

    @Override
    protected String initialAudioPath() {
        return GamePaths.absConfig(this.wall.audio);
    }

    @Override
    protected void initWidgets(int centerX) {
        this.volumeBox = this.addFullBox(centerX, VOLUME, NUMERIC);
        this.volumeBox.setValue(String.valueOf(this.wall.volume));
        this.bindLabel(VOLUME, this.volumeBox);

        AbstractMediaScreen.EditBoxPair sizeBoxes = AbstractMediaScreen.EditBoxPair.of(this.addHalfBoxRow(centerX, MAX_WIDTH, MAX_HEIGHT, NUMERIC));
        this.maxWidthBox = sizeBoxes.left();
        this.maxHeightBox = sizeBoxes.right();
        this.maxWidthBox.setValue(String.valueOf(NarutoConfig.maxWidth()));
        this.maxHeightBox.setValue(String.valueOf(NarutoConfig.maxHeight()));
        this.bindLabel(MAX_WIDTH, this.maxWidthBox);
        this.bindLabel(MAX_HEIGHT, this.maxHeightBox);

        Checkbox[] checks = this.addCheckboxRow(centerX, LOCAL_SOUND, this.wall.hasLocalSound(), LIGHT, this.wall.light);
        this.localSoundCheck = checks[0];
        this.lightCheck = checks[1];

        this.addRandomSwapRow(centerX);
        this.addRestartSwitchRow(centerX);
        this.addFullRow(CLEAN, () -> NarutoPackets.INSTANCE.sendToServer(new WallCleanPacket(this.wall)), Tooltip.create(CLEAN_TOOLTIP));
        this.addConfirmRow(centerX);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.lightCheck.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, LIGHT_TOOLTIP, mouseX, mouseY);
        }
    }

    @Override
    protected List<Component> swapTooltipLines() {
        return Lists.newArrayList(SWAP_TOOLTIP, SWAP_TOOLTIP_UNIVERSAL, SWAP_TOOLTIP_COMPAT);
    }

    @Override
    protected boolean isCompatMode() {
        return ClientWalls.isCompatMode();
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
        boolean lightCheck = this.lightCheck.selected();

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
        boolean lightChanged = lightCheck != this.wall.light;

        if (!soundChanged && !sourceChanged && !sizeChanged && !lightChanged) {
            this.onClose();
            return;
        }

        if ((volumeChanged || lightChanged) && !sourceChanged && !localSoundChanged && !sizeChanged) {
            this.wall.volume = newVolume;
            renderer.setVolume(newVolume);
            this.wall.light = lightCheck;
            renderer.setLight(lightCheck);
            if (!lightCheck) renderer.checkLight();
            NarutoPackets.INSTANCE.sendToServer(new WallUpdatePacket(this.wall));
            this.doing.set(false);
            this.onClose();
            return;
        }

        NarutoConfig.maxWidth(maxWidthBox);
        NarutoConfig.maxHeight(maxHeightBox);

        NarutoWorldScreen.getAudioFuture(localSoundCheck, absAudioBox)
                .whenCompleteAsync((converted, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace(System.err);
                        throw new RuntimeException(throwable);
                    }

                    if (localSoundCheck) {
                        AudioZipGenerator.get(converted).generate((id) -> this.applyDone(absVideoBox, absAudioBox, id, newVolume, lightCheck));
                    } else {
                        this.applyDone(absVideoBox, absAudioBox, Wall.NO_LOCAL_SOUND, newVolume, lightCheck);
                    }
                }, this.minecraft);
    }

    @Contract("_, _ -> !null")
    private static CompletableFuture<@Nullable String> getAudioFuture(boolean localSound, String absAudioPath) {
        return localSound ? FFmpeg.toMonoOgg(absAudioPath) : CompletableFuture.completedFuture(absAudioPath);
    }

    private void applyDone(String absVideo, String absAudio, ResourceLocation localSound, float newVolume, boolean light) {
        this.wall.video = GamePaths.relConfig(absVideo);
        this.wall.audio = GamePaths.relConfig(absAudio);
        this.wall.localSound = localSound;
        this.wall.volume = newVolume;
        this.wall.light = light;

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

    @Override
    protected void restartAction() {
        WallTV<?> tv = ClientWalls.get(this.wall);
        if (tv == null) return;
        tv.shutdownEntire(true);
    }

    @Override
    protected void switchAction() {
        if (ClientWalls.get(this.wall) != null) {
            ClientWalls.remove(this.wall).ifPresent(WallTV.DEATH);
        } else {
            ClientWalls.add(this.wall).ifPresent(WallTV.DEATH);
        }
    }

    @Override
    protected void swapAction() {
        ClientWalls.swap();
    }

    public static void sync(String video, String audio) {
        if (Minecraft.getInstance().screen instanceof NarutoWorldScreen screen) {
            if (screen.videoBox != null) screen.videoBox.setValue(video);
            if (screen.audioBox != null) screen.audioBox.setValue(audio);
        }
    }
}