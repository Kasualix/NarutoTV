package me.kall.narutotv.screen;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings({"SameParameterValue", "DataFlowIssue"})
public abstract class AbstractMediaScreen extends Screen {
    static final Component SCREEN = Component.translatable("screen.narutotv.gui.title");

    protected static final Component VIDEO = Component.translatable("box.narutotv.video");
    protected static final Component AUDIO = Component.translatable("box.narutotv.audio");
    protected static final Component VOLUME = Component.translatable("box.narutotv.volume");
    protected static final Component MAX_WIDTH = Component.translatable("box.narutotv.max_width");
    protected static final Component MAX_HEIGHT = Component.translatable("box.narutotv.max_height");

    protected static final Component COMPATIBILITY = Component.translatable("mode.narutotv.compatibility").withStyle(ChatFormatting.GREEN);
    protected static final Component PERFORMANCE = Component.translatable("mode.narutotv.performance").withStyle(ChatFormatting.RED);

    protected static final Component RANDOM = Component.translatable("button.narutotv.random");
    protected static final Component SWAP = Component.translatable("button.narutotv.swap");
    protected static final Component RANDOM_TOOLTIP = Component.translatable("tooltip.narutotv.random");
    protected static final Component SWAP_TOOLTIP = Component.translatable("tooltip.narutotv.swap");

    protected static final int BOX_WIDTH = 200, BOX_HEIGHT = 20, BOX_SPACING = 18;
    protected static final int BUTTON_WIDTH = 80, BUTTON_HEIGHT = 20, BUTTON_SPACING = 5;

    protected static final Predicate<String> NUMERIC = string -> string.matches("-?\\d*\\.?\\d*");

    protected final @Nullable Screen lastScreen;

    protected EditBox videoBox, audioBox, volumeBox, maxWidthBox, maxHeightBox;
    protected Button randomButton, swapButton, yesButton, noButton, restartButton, switchButton;

    protected int currentY;
    protected int offset;

    private final List<LabelBinding> labelBindings = new ArrayList<>();

    protected AbstractMediaScreen(Component title, @Nullable Screen lastScreen) {
        super(title);
        this.lastScreen = lastScreen;
    }

    @Override
    protected final void init() {
        this.offset = 0;
        this.currentY = 25;
        this.labelBindings.clear();

        int centerX = this.width / 2;

        this.videoBox = this.addFullBox(centerX, VIDEO, null);
        this.videoBox.setValue(this.initialVideoPath());
        this.bindLabel(VIDEO, this.videoBox);

        this.audioBox = this.addFullBox(centerX, AUDIO, null);
        this.audioBox.setValue(this.initialAudioPath());
        this.bindLabel(AUDIO, this.audioBox);

        this.initWidgets(centerX);

        this.offset = this.clampOffset();
    }

    protected abstract void initWidgets(int centerX);

    protected abstract String initialVideoPath();
    protected abstract String initialAudioPath();

    protected EditBox addFullBox(int centerX, Component label, Predicate<String> filter) {
        EditBox box = new EditBox(this.font, centerX - BOX_WIDTH / 2, this.currentY, BOX_WIDTH, BOX_HEIGHT, label);
        box.setMaxLength(Integer.MAX_VALUE);
        if (filter != null) box.setFilter(filter);
        this.addRenderableWidget(box);
        this.currentY += BOX_HEIGHT + BOX_SPACING;
        return box;
    }

    protected EditBox[] addHalfBoxRow(int centerX, Component firstLabel, Component secLabel, Predicate<String> filter) {
        EditBox left = new EditBox(this.font, centerX - BOX_WIDTH / 2, this.currentY, BOX_WIDTH / 2 - 5, BOX_HEIGHT, firstLabel);
        left.setMaxLength(Integer.MAX_VALUE);
        left.setFilter(filter);
        this.addRenderableWidget(left);

        EditBox right = new EditBox(this.font, centerX + 5, this.currentY, BOX_WIDTH / 2 - 5, BOX_HEIGHT, secLabel);
        right.setMaxLength(Integer.MAX_VALUE);
        right.setFilter(filter);
        this.addRenderableWidget(right);

        this.currentY += BOX_HEIGHT + BOX_SPACING;
        return new EditBox[]{left, right};
    }

    protected Checkbox[] addCheckboxRow(int centerX, Component leftLabel, boolean leftSelected, Component rightLabel, boolean rightSelected) {
        int leftWidth = this.font.width(leftLabel) + 24;
        int rightWidth = this.font.width(rightLabel) + 24;

        Checkbox left = Checkbox.builder(leftLabel, this.font).pos(centerX - 5 - leftWidth, this.currentY).maxWidth(leftWidth).selected(leftSelected).build();
        this.addRenderableWidget(left);

        Checkbox right = Checkbox.builder(rightLabel, this.font).pos(centerX + 5, this.currentY).maxWidth(rightWidth).selected(rightSelected).build();
        this.addRenderableWidget(right);

        this.currentY += BUTTON_HEIGHT + 5;
        return new Checkbox[]{left, right};
    }

    protected void addImmediateCheckbox(int x, Component label, boolean selected, Consumer<Boolean> onChange) {
        int width = this.font.width(label) + 24;
        Checkbox box = Checkbox.builder(label, this.font).pos(x, this.currentY).maxWidth(width).selected(selected).onValueChange((checkbox, value) -> onChange.accept(checkbox.selected())).build();
        this.addRenderableWidget(box);
    }

    protected void addRandomSwapRow(int centerX) {
        this.randomButton = Button.builder(RANDOM, button -> this.onRandom()).bounds(centerX - BUTTON_WIDTH - 5, this.currentY, BUTTON_WIDTH, BUTTON_HEIGHT).tooltip(Tooltip.create(RANDOM_TOOLTIP)).build();
        this.addRenderableWidget(this.randomButton);

        this.swapButton = Button.builder(SWAP, button -> this.swapAction()).bounds(centerX + 5, this.currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.swapButton);

        this.currentY += BUTTON_HEIGHT + BUTTON_SPACING;
    }

    protected void addRestartSwitchRow(int centerX) {
        this.restartButton = Button.builder(Component.translatable("button.narutotv.restart"), button -> this.restartAction()).bounds(centerX - BUTTON_WIDTH - 5, this.currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.restartButton);

        this.switchButton = Button.builder(Component.translatable("button.narutotv.switch"), button -> this.switchAction()).bounds(centerX + 5, this.currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.switchButton);

        this.currentY += BUTTON_HEIGHT + BUTTON_SPACING;
    }

    protected void addFullRow(Component label, Runnable action, Tooltip tooltip) {
        int centerX = this.width / 2;
        Button.Builder builder = Button.builder(label, button -> action.run()).bounds(centerX - BUTTON_WIDTH - 5, this.currentY, BUTTON_WIDTH * 2 + 10, BUTTON_HEIGHT);
        if (tooltip != null) builder.tooltip(tooltip);
        Button button = builder.build();
        this.addRenderableWidget(button);
        this.currentY += BUTTON_HEIGHT + BUTTON_SPACING;
    }

    protected void addConfirmRow(int centerX) {
        this.yesButton = Button.builder(Component.translatable("gui.yes"), button -> this.onDone()).bounds(centerX - BUTTON_WIDTH - 5, this.currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.yesButton);

        this.noButton = Button.builder(Component.translatable("gui.no"), button -> this.onClose()).bounds(centerX + 5, this.currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.noButton);

        this.currentY += BUTTON_HEIGHT + BUTTON_SPACING;
    }

    protected void bindLabel(Component label, AbstractWidget widget) {
        this.labelBindings.add(new LabelBinding(label, widget));
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double delta = scrollY == 0.0 && Minecraft.ON_OSX ? scrollX : scrollY;
        int lastOffset = this.offset;
        this.offset -= (int) (delta * 20D);
        this.offset = this.clampOffset();

        int deltaY = lastOffset - this.offset;
        if (delta != 0) {
            for (Renderable renderable : this.renderables) {
                if (renderable instanceof AbstractWidget widget) {
                    widget.setY(widget.getY() + deltaY);
                }
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    protected int clampOffset() {
        return Math.clamp(this.offset, 0, Math.max(0, this.currentY - this.height + 10));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        Font font = this.font;
        for (LabelBinding binding : this.labelBindings) {
            int centerX = binding.widget.getX() + binding.widget.getWidth() / 2;
            guiGraphics.drawCenteredString(font, binding.label, centerX, binding.widget.getY() - 12, 0xFFFFFF);
        }

        if (this.swapButton.isMouseOver(mouseX, mouseY)) {
            List<Component> lines = this.swapTooltipLines();
            lines.add(this.isCompatMode() ? COMPATIBILITY : PERFORMANCE);
            guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    protected List<Component> swapTooltipLines() {
        return Lists.newArrayList(SWAP_TOOLTIP);
    }

    protected abstract boolean isCompatMode();

    protected abstract void onRandom();
    protected abstract void onDone();
    protected abstract void swapAction();
    protected abstract void restartAction();
    protected abstract void switchAction();

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
        super.onClose();
    }

    private record LabelBinding(Component label, AbstractWidget widget) {}

    protected record EditBoxPair(EditBox left, EditBox right) {
        @Contract("_ -> new")
        static @NotNull EditBoxPair of(EditBox @NotNull [] pair) {
            return new EditBoxPair(pair[0], pair[1]);
        }
    }
}