package me.kall.narutotv.screen;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.config.NarutoConfig;
import me.kall.narutotv.data.file.GamePaths;
import me.kall.narutotv.data.file.Sources;
import me.kall.narutotv.override.GuiSceneControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class NarutoSceneScreen extends AbstractMediaScreen {
    static final Component FADABLE = Component.translatable("checkbox.narutotv.fadable");
    static final Component MUTE_MUSIC = Component.translatable("checkbox.narutotv.mute_music");
    static final Component TICKS = Component.translatable("box.narutotv.ticks");

    private EditBox ticksBox;

    private String video, audio;

    public NarutoSceneScreen(@Nullable net.minecraft.client.gui.screens.Screen lastScreen, @NotNull MediaArgs mediaArgs) {
        super(SCREEN, lastScreen);
        this.video = mediaArgs.absVideoPath();
        this.audio = mediaArgs.absAudioPath();
    }

    public static void sync(String video, String audio) {
        if (Minecraft.getInstance().screen instanceof NarutoSceneScreen screen) {
            if (screen.videoBox != null) screen.videoBox.setValue(video);
            if (screen.audioBox != null) screen.audioBox.setValue(audio);
        }
    }

    @Override
    protected String initialVideoPath() {
        return GamePaths.absConfig(this.video);
    }

    @Override
    protected String initialAudioPath() {
        return GamePaths.absConfig(this.audio);
    }

    @Override
    protected void initWidgets(int centerX) {
        AbstractMediaScreen.EditBoxPair ticksAndVolume = AbstractMediaScreen.EditBoxPair.of(this.addHalfBoxRow(centerX, TICKS, VOLUME, NUMERIC));
        this.ticksBox = ticksAndVolume.left();
        this.volumeBox = ticksAndVolume.right();
        this.ticksBox.setMaxLength(this.font.width(String.valueOf(Integer.MAX_VALUE)));
        this.ticksBox.setValue(String.valueOf(NarutoConfig.ticksBeforeFade()));
        this.volumeBox.setMaxLength(this.font.width(String.valueOf(Integer.MAX_VALUE)));
        this.volumeBox.setValue(String.valueOf(NarutoConfig.volume()));
        this.bindLabel(TICKS, this.ticksBox);
        this.bindLabel(VOLUME, this.volumeBox);

        AbstractMediaScreen.EditBoxPair sizeBoxes = AbstractMediaScreen.EditBoxPair.of(this.addHalfBoxRow(centerX, MAX_WIDTH, MAX_HEIGHT, NUMERIC));
        this.maxWidthBox = sizeBoxes.left();
        this.maxHeightBox = sizeBoxes.right();
        this.maxWidthBox.setValue(String.valueOf(NarutoConfig.maxWidth()));
        this.maxHeightBox.setValue(String.valueOf(NarutoConfig.maxHeight()));
        this.bindLabel(MAX_WIDTH, this.maxWidthBox);
        this.bindLabel(MAX_HEIGHT, this.maxHeightBox);

        this.addImmediateCheckbox(centerX - BUTTON_WIDTH - 5, FADABLE, NarutoConfig.fadable(), NarutoConfig::fadable);
        this.addImmediateCheckbox(centerX + 5, MUTE_MUSIC, NarutoConfig.muteMusic(), NarutoConfig::muteMusic);
        this.currentY += BUTTON_HEIGHT + 5;

        this.addRandomSwapRow(centerX);
        this.addRestartSwitchRow(centerX);
        this.addConfirmRow(centerX);
    }

    @Override
    protected boolean isCompatMode() {
        return GuiSceneControl.isCompatMode();
    }

    @Override
    protected void onRandom() {
        GuiSceneControl.active.shutdownEntire(false);
        GuiSceneControl.active.setup(0D);

        MediaArgs mediaArgs = GuiSceneControl.active.mediaArgs;
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

        MediaArgs current = GuiSceneControl.active.mediaArgs;

        if (current != null) {
            this.video = current.absVideoPath();
            this.audio = current.absAudioPath();
        }

        NarutoConfig.ticksBeforeFade(Integer.parseInt(this.ticksBox.getValue()));

        if (NarutoConfig.volume(Double.parseDouble(this.volumeBox.getValue()))) GuiSceneControl.active.setVolume(NarutoConfig.volume());

        boolean maxWidthChanged = NarutoConfig.maxWidth(maxWidthBox);
        boolean maxHeightChanged = NarutoConfig.maxHeight(maxHeightBox);

        if (!videoBox.equals(this.video) || !audioBox.equals(this.audio) || maxHeightChanged || maxWidthChanged) {
            GuiSceneControl.active.shutdownEntire(false);
            GuiSceneControl.active.mediaArgs = Sources.get(videoBox);
        }

        this.onClose();
    }

    @Override
    protected void swapAction() {
        GuiSceneControl.swap();
    }

    @Override
    protected void restartAction() {
        GuiSceneControl.active.shutdownEntire(true);
    }

    @Override
    protected void switchAction() {
        NarutoConfig.enableGuiScreen(!NarutoConfig.enableGuiScreen());
        GuiSceneControl.active.shutdownEntire(true);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        MediaArgs mediaArgs = GuiSceneControl.active.mediaArgs;
        if (mediaArgs == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().getWindow();

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F12) != GLFW.GLFW_PRESS || minecraft.screen instanceof NarutoSceneScreen) return;

        minecraft.setScreen(new NarutoSceneScreen(minecraft.screen, mediaArgs));
    }
}