package me.kall.narutotv.screen;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public class DoubleCheckScreen extends Screen {
    private @Nullable BooleanConsumer onChecked;
    private final Screen lastScreen;
    private final Component note;
    private final String onTrue, onFalse;

    public DoubleCheckScreen(@NotNull BooleanConsumer onChecked, @Nullable Screen lastScreen, Component note, String onTrue, String onFalse) {
        super(AbstractMediaScreen.SCREEN);
        this.onChecked = onChecked;
        this.lastScreen = lastScreen;
        this.note = note;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }

    @Override
    public void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.translatable(this.onTrue), button -> this.onDone(true)).bounds(centerX - AbstractMediaScreen.BUTTON_WIDTH - 5, centerY, AbstractMediaScreen.BUTTON_WIDTH, AbstractMediaScreen.BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable(this.onFalse), button -> this.onDone(false)).bounds(centerX + 5, centerY, AbstractMediaScreen.BUTTON_WIDTH, AbstractMediaScreen.BUTTON_HEIGHT).build());
    }

    @SuppressWarnings("DataFlowIssue")
    void onDone(boolean result) {
        this.minecraft.setScreen(this.lastScreen);
        if (this.onChecked != null) {
            this.onChecked.accept(result);
            this.onChecked = null;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.note, this.width / 2, this.height / 2 - 50, Color.YELLOW.getRGB());
    }
}
