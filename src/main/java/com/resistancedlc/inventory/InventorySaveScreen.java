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
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * InventorySaveScreen — mini-экран для сохранения снапшота.
 * Имя можно ввести своё, но если оставить пустым —
 * сохранится как "Inventory N".
 */
public class InventorySaveScreen extends Screen {

    private static final int W = 260;
    private static final int H = 100;

    private final int slotIndex;
    private final AccordionScreen parent;

    private EditBox nameField;
    private int panelX, panelY;

    public InventorySaveScreen(int slotIndex, AccordionScreen parent) {
        super(Component.literal(LocalizationManager.get("gui.resistancedlc.inventory.save.title")));
        this.slotIndex = slotIndex;
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.panelX = (this.width - W) / 2;
        this.panelY = (this.height - H) / 2;

        InventorySnapshot existing = InventorySnapshotManager.load(slotIndex);
        String defaultName = (existing != null && !existing.getName().isEmpty())
                ? existing.getName()
                : InventorySnapshotManager.defaultNameFor(slotIndex);

        this.nameField = new EditBox(this.font,
                panelX + 15, panelY + 35, W - 30, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.inventory.save.hint")));
        this.nameField.setMaxLength(40);
        this.nameField.setValue(defaultName);
        this.addRenderableWidget(this.nameField);
        this.setFocused(this.nameField);

        this.addRenderableWidget(Button.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.inventory.save.confirm")),
                        (b) -> applySave())
                .bounds(panelX + 15, panelY + 65, (W - 40) / 2, 20).build());

        this.addRenderableWidget(Button.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.inventory.save.cancel")),
                        (b) -> this.onClose())
                .bounds(panelX + W - 15 - (W - 40) / 2, panelY + 65, (W - 40) / 2, 20).build());
    }

    private void applySave() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            this.onClose();
            return;
        }

        String newName = (nameField != null) ? nameField.getValue().trim() : "";
        if (newName.isEmpty()) {
            newName = InventorySnapshotManager.defaultNameFor(slotIndex);
        }

        InventorySnapshot snap = InventorySnapshotManager.captureFromPlayer(mc.player);
        snap.setName(newName);
        snap.setTimestamp(System.currentTimeMillis());
        InventorySnapshotManager.save(slotIndex, snap);
        ConfigManager.save();

        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(
                            "§a[Inventory] " + LocalizationManager.get(
                                    "gui.resistancedlc.inventory.saved_msg", slotIndex)),
                    true);
        }

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
                "§l" + LocalizationManager.get("gui.resistancedlc.inventory.save.title")
                        + " §7# " + slotIndex,
                panelX + 15, panelY + 12, 0xFFFFFFFF, true);

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            applySave();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
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