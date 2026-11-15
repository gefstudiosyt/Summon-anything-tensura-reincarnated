package dev.summonanything;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Magic Station screen: search box, scrollable list of every mob from every mod, Create button. */
public class StationScreen extends HandledScreen<StationScreenHandler> {
    private static final int LIST_X = 8, LIST_Y = 34, ROW_H = 12, ROWS = 8, LIST_W = 112;

    private static List<Identifier> allMobs;

    private final List<Identifier> filtered = new ArrayList<>();
    private TextFieldWidget search;
    private ButtonWidget create;
    private Identifier selected;
    private int scroll = 0;

    public StationScreen(StationScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 222;
        this.playerInventoryTitleY = 10000;   // hidden: the space is used by the list
    }

    private static List<Identifier> mobs() {
        if (allMobs == null) {
            List<Identifier> list = new ArrayList<>();
            for (EntityType<?> type : Registries.ENTITY_TYPE) {
                if (type.getSpawnGroup() == SpawnGroup.MISC || !type.isSummonable()) continue;
                Identifier id = Registries.ENTITY_TYPE.getId(type);
                if (SummonAnythingConfig.isAlwaysBlocked(id)) continue;
                list.add(id);
            }
            list.sort(Comparator.comparing(Identifier::getNamespace).thenComparing(Identifier::getPath));
            allMobs = list;
        }
        return allMobs;
    }

    private static String label(Identifier id) {
        return Registries.ENTITY_TYPE.get(id).getName().getString();
    }

    @Override
    protected void init() {
        super.init();
        search = new TextFieldWidget(this.textRenderer, this.x + LIST_X, this.y + 18, LIST_W, 12, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search mobs or mods..."));
        search.setChangedListener(s -> refilter());
        this.addDrawableChild(search);

        create = ButtonWidget.builder(Text.literal("Create"), b -> {
            if (selected != null) {
                ClientPlayNetworking.send(new SelectMobPayload(selected));
            }
        }).dimensions(this.x + 124, this.y + 58, 44, 20).build();
        this.addDrawableChild(create);
        refilter();
    }

    private void refilter() {
        filtered.clear();
        String q = search == null ? "" : search.getText().trim().toLowerCase();
        for (Identifier id : mobs()) {
            if (q.isEmpty() || id.toString().toLowerCase().contains(q) || label(id).toLowerCase().contains(q)) {
                filtered.add(id);
            }
        }
        scroll = 0;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x0 = this.x, y0 = this.y;
        // panel
        context.fill(x0, y0, x0 + backgroundWidth, y0 + backgroundHeight, 0xFF1A1030);
        context.fill(x0 + 2, y0 + 2, x0 + backgroundWidth - 2, y0 + backgroundHeight - 2, 0xFF2B1B4A);
        context.fill(x0, y0, x0 + backgroundWidth, y0 + 1, 0xFF9B6BFF);
        context.fill(x0, y0 + backgroundHeight - 1, x0 + backgroundWidth, y0 + backgroundHeight, 0xFF9B6BFF);
        context.fill(x0, y0, x0 + 1, y0 + backgroundHeight, 0xFF9B6BFF);
        context.fill(x0 + backgroundWidth - 1, y0, x0 + backgroundWidth, y0 + backgroundHeight, 0xFF9B6BFF);

        // list
        context.fill(x0 + LIST_X - 1, y0 + LIST_Y - 1, x0 + LIST_X + LIST_W + 1, y0 + LIST_Y + ROWS * ROW_H + 1, 0xFF9B6BFF);
        context.fill(x0 + LIST_X, y0 + LIST_Y, x0 + LIST_X + LIST_W, y0 + LIST_Y + ROWS * ROW_H, 0xFF120A22);
        for (int i = 0; i < ROWS; i++) {
            int index = scroll + i;
            if (index >= filtered.size()) break;
            Identifier id = filtered.get(index);
            int ry = y0 + LIST_Y + i * ROW_H;
            boolean hover = mouseX >= x0 + LIST_X && mouseX < x0 + LIST_X + LIST_W && mouseY >= ry && mouseY < ry + ROW_H;
            if (id.equals(selected)) {
                context.fill(x0 + LIST_X, ry, x0 + LIST_X + LIST_W, ry + ROW_H, 0xFF5B34A8);
            } else if (hover) {
                context.fill(x0 + LIST_X, ry, x0 + LIST_X + LIST_W, ry + ROW_H, 0xFF3A2470);
            }
            String text = textRenderer.trimToWidth(label(id) + " (" + id.getNamespace() + ")", LIST_W - 4);
            context.drawText(textRenderer, text, x0 + LIST_X + 2, ry + 2, 0xFFE8DCFF, false);
        }
        if (filtered.isEmpty()) {
            context.drawText(textRenderer, "No mobs found", x0 + LIST_X + 4, y0 + LIST_Y + 4, 0xFF9A8AB8, false);
        }

        // slot frames
        frame(context, x0 + 142, y0 + 36);
        frame(context, x0 + 142, y0 + 88);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) frame(context, x0 + 8 + col * 18, y0 + 140 + row * 18);
        }
        for (int col = 0; col < 9; col++) frame(context, x0 + 8 + col * 18, y0 + 198);

        // arrow between tome and output
        context.drawText(textRenderer, "v", x0 + 147, y0 + 66, 0xFFB894FF, false);

        // selected mob
        String sel = selected == null ? "Pick a mob from the list" : "Selected: " + label(selected);
        context.drawText(textRenderer, textRenderer.trimToWidth(sel, backgroundWidth - 16), x0 + 8, y0 + 132, 0xFFFFE6A8, false);
    }

    private void frame(DrawContext context, int sx, int sy) {
        context.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF9B6BFF);
        context.fill(sx, sy, sx + 16, sy + 16, 0xFF120A22);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, this.title, 8, 6, 0xFFE8DCFF, false);
        context.drawText(textRenderer, "Tome", 138, 25, 0xFF9A8AB8, false);
        context.drawText(textRenderer, "Book", 140, 77, 0xFF9A8AB8, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        create.active = selected != null
                && this.handler.getSlot(0).hasStack()
                && !this.handler.getSlot(1).hasStack();
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (mouseX >= this.x + LIST_X && mouseX < this.x + LIST_X + LIST_W
                && mouseY >= this.y + LIST_Y && mouseY < this.y + LIST_Y + ROWS * ROW_H) {
            int index = scroll + (int) ((mouseY - (this.y + LIST_Y)) / ROW_H);
            if (index >= 0 && index < filtered.size()) {
                selected = filtered.get(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int max = Math.max(0, filtered.size() - ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(vertical)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // While typing in the search box, letters (like E) must not close the screen. 256 = Escape.
        if (keyCode != 256 && search != null && search.isFocused()) {
            search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
