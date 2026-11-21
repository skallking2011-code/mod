package de.texturemixer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Gemeinsame Tab-Leiste oben: Texturen | Presets | Giveaway | Auktion. */
final class Tabs {
    static final String[] NAMES = {"Texturen", "Presets", "Giveaway", "Auktion"};

    private Tabs() {}

    static Screen create(int i) {
        return switch (i) {
            case 0 -> new MixerScreen(null);
            case 1 -> new PresetsScreen();
            case 2 -> new EventScreen(false);
            default -> new EventScreen(true);
        };
    }

    static void add(int width, int active, Consumer<Button> add) {
        int w = Math.min(100, (width - 16) / NAMES.length);
        int total = w * NAMES.length + (NAMES.length - 1) * 2;
        int x0 = (width - total) / 2;
        for (int i = 0; i < NAMES.length; i++) {
            final int target = i;
            Button b = Button.builder(Component.literal(NAMES[i]),
                    bt -> Minecraft.getInstance().setScreenAndShow(create(target)))
                    .bounds(x0 + i * (w + 2), 4, w, 18).build();
            b.active = i != active;
            add.accept(b);
        }
    }
}
