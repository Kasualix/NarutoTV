package me.kall.narutotv.screen;


import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class NameSetScreen extends Screen {
    static final Component NAME = Component.translatable("box.narutotv.name");
    static final Component TYPE = Component.translatable("tooltip.narutotv.type");
    static final Component CONFIRM = Component.translatable("note.narutotv.confirm").withStyle(ChatFormatting.GRAY);

    final @Nullable Screen lastScreen;

    @Nullable Consumer<String> nameAction;

    EditBox nameBox;

    public NameSetScreen(@NotNull Consumer<String> nameAction, @Nullable Screen lastScreen) {
        super(AbstractMediaScreen.SCREEN);
        this.nameAction = nameAction;
        this.lastScreen = lastScreen;
    }

    @Override
    public void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.nameBox = new EditBox(this.font, centerX - AbstractMediaScreen.BOX_WIDTH / 2, centerY, AbstractMediaScreen.BOX_WIDTH, AbstractMediaScreen.BOX_HEIGHT, NAME);
        this.nameBox.setMaxLength(Integer.MAX_VALUE);
        this.addRenderableWidget(this.nameBox);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, NAME, this.width / 2, this.nameBox.getY() - 12, 0xFFFFFF);

        if (this.nameBox.isHovered()) guiGraphics.renderTooltip(this.font, TYPE, mouseX, mouseY);

        if (this.nameBox.getValue().isBlank() || this.nameAction == null) return;

        guiGraphics.drawCenteredString(this.font, CONFIRM, this.width / 2, this.nameBox.getY() + this.nameBox.getHeight() + 12, 0xFFFFFF);

        long window = this.getMinecraft().getWindow().getWindow();

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_ENTER) == GLFW.GLFW_PRESS) {
            this.onClose();
            this.nameAction.accept(this.nameBox.getValue());
            this.nameAction = null;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.lastScreen);
        super.onClose();
    }
}
