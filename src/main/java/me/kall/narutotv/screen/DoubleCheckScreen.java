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
    private final String existing;

    public DoubleCheckScreen(@NotNull BooleanConsumer onChecked, @Nullable Screen lastScreen, String existing) {
        super(AbstractNarutoScreen.SCREEN);
        this.onChecked = onChecked;
        this.lastScreen = lastScreen;
        this.existing = existing;
    }

    @Override
    public void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("button.narutotv.existing"), button -> this.onDone(true)).bounds(centerX - AbstractNarutoScreen.BUTTON_WIDTH - 5, centerY, AbstractNarutoScreen.BUTTON_WIDTH, AbstractNarutoScreen.BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.narutotv.replace"), button -> this.onDone(false)).bounds(centerX + 5, centerY, AbstractNarutoScreen.BUTTON_WIDTH, AbstractNarutoScreen.BUTTON_HEIGHT).build());
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
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, Component.translatable("note.narutotv.double_check", this.existing), this.width / 2, this.height / 2 - 50, Color.WHITE.getRGB());
    }
}
