package com.resistancedlc.inventory;

import com.resistancedlc.AccordionScreen;
import com.resistancedlc.ConfigManager;
import com.resistancedlc.LocalizationManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * InventoryRenameScreen — mini-экран для переименования слота инвентаря.
 * Открывается из панели InventoryManager в AccordionScreen.
 */
public class InventoryRenameScreen extends Screen {

    private static final int W = 260;
    private static final int H = 100;

    private final int slotIndex;
    private final AccordionScreen parent;

    private EditBox nameField;
    private int panelX, panelY;

    public InventoryRenameScreen(int slotIndex, AccordionScreen parent) {
        super(Component.literal(LocalizationManager.get("gui.resistancedlc.inventory.rename.title")));
        this.slotIndex = slotIndex;
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.panelX = (this.width - W) / 2;
        this.panelY = (this.height - H) / 2;

        InventorySnapshot snap = InventorySnapshotManager.load(slotIndex);
        String currentName = (snap != null) ? snap.getName()
                : InventorySnapshotManager.defaultNameFor(slotIndex);

        this.nameField = new EditBox(this.font,
                panelX + 15, panelY + 35, W - 30, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.inventory.rename.hint")));
        this.nameField.setMaxLength(40);
        this.nameField.setValue(currentName);
        this.addRenderableWidget(this.nameField);
        this.setFocused(this.nameField);

        this.addRenderableWidget(Button.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.inventory.rename.save")),
                        (b) -> applyRename())
                .bounds(panelX + 15, panelY + 65, (W - 40) / 2, 20).build());

        this.addRenderableWidget(Button.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.inventory.rename.cancel")),
                        (b) -> this.onClose())
                .bounds(panelX + W - 15 - (W - 40) / 2, panelY + 65, (W - 40) / 2, 20).build());
    }

    private void applyRename() {
        if (nameField == null) return;
        String newName = nameField.getValue().trim();
        if (newName.isEmpty()) {
            newName = InventorySnapshotManager.defaultNameFor(slotIndex);
        }
        InventorySnapshotManager.rename(slotIndex, newName);
        ConfigManager.save();
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x80000000);
        graphics.fill(panelX, panelY, panelX + W, panelY + H, 0xC0000000);
        graphics.fill(panelX, panelY, panelX + W, panelY + 2, 0xFF60A0FF);
        graphics.fill(panelX, panelY + H - 2, panelX + W, panelY + H, 0xFF60A0FF);
        graphics.fill(panelX, panelY, panelX + 2, panelY + H, 0xFF60A0FF);
        graphics.fill(panelX + W - 2, panelY, panelX + W, panelY + H, 0xFF60A0FF);

        graphics.drawString(this.font,
                "§l" + LocalizationManager.get("gui.resistancedlc.inventory.rename.title")
                        + " §7# " + slotIndex,
                panelX + 15, panelY + 12, 0xFFFFFFFF, true);

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            applyRename();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        Minecraft mc = Minecraft.getInstance();
        if (parent != null) {
            mc.setScreen(parent);
        } else {
            mc.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}