package de.texturemixer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class PresetsScreen extends Screen {
    private EditBox nameBox;
    private String status = "";

    public PresetsScreen() {
        super(Component.literal("Presets"));
    }

    @Override
    protected void init() {
        Tabs.add(this.width, 1, this::addRenderableWidget);
        int cx = this.width / 2;
        Map<String, Map<String, String>> all = MixerConfig.loadPresets();
        int maxRows = Math.max(1, (this.height - 120) / 24);
        int y = 40, i = 0;
        for (String name : all.keySet()) {
            if (i++ >= maxRows) break;
            final String n = name;
            Button label = Button.builder(Component.literal(n), b -> {}).bounds(cx - 150, y, 150, 20).build();
            label.active = false;
            addRenderableWidget(label);
            addRenderableWidget(Button.builder(Component.literal("Laden"), b -> load(n))
                    .bounds(cx + 5, y, 70, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Löschen"), b -> delete(n))
                    .bounds(cx + 80, y, 70, 20).build());
            y += 24;
        }

        nameBox = new EditBox(this.font, cx - 150, this.height - 60, 150, 20, Component.literal("Name"));
        nameBox.setValue("");
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.literal("Auswahl speichern"), b -> save())
                .bounds(cx + 5, this.height - 60, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Schließen"), b -> onClose())
                .bounds(cx - 50, this.height - 28, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        super.extractRenderState(g, mx, my, d);
        if (MixerConfig.loadPresets().isEmpty())
            g.text(this.font, "Noch keine Presets. Wende im Tab Texturen eine Auswahl an, dann hier speichern.",
                    this.width / 2 - 150, 44, 0xFFB0B8C0, false);
        g.text(this.font, "Name für neues Preset:", this.width / 2 - 150, this.height - 72, 0xFFFFFFFF, false);
        if (!status.isEmpty()) g.text(this.font, status, this.width / 2 - 150, this.height - 40, 0xFF9BE08A, true);
    }

    private void save() {
        String name = nameBox.getValue().trim().replaceAll("[^A-Za-z0-9_-]", "_");
        if (name.isEmpty()) { status = "Bitte einen Namen eingeben."; return; }
        Map<String, Map<String, String>> all = new TreeMap<>(MixerConfig.loadPresets());
        all.put(name, new HashMap<>(MixerConfig.load()));
        MixerConfig.savePresets(all);
        status = "Preset '" + name + "' gespeichert.";
        rebuildWidgets();
    }

    private void load(String name) {
        Map<String, String> sel = MixerConfig.loadPresets().get(name);
        if (sel == null) { status = "Preset nicht gefunden."; return; }
        try {
            MixerConfig.save(sel);
            int n = PackTools.build(sel);
            PackActivator.enableAndReload();
            status = "Preset '" + name + "' geladen (" + n + " Dateien).";
        } catch (Exception e) {
            status = "Fehler: " + e.getMessage();
        }
    }

    private void delete(String name) {
        Map<String, Map<String, String>> all = new TreeMap<>(MixerConfig.loadPresets());
        all.remove(name);
        MixerConfig.savePresets(all);
        status = "Preset '" + name + "' gelöscht.";
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(null);
    }
}
