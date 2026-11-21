package de.texturemixer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * Links Kategorien, rechts Kacheln: erste Kachel = Standard (nichts ueberschreiben),
 * danach pro Pack eine Kachel mit der ECHTEN Textur aus dem Pack. Roter Rahmen = gewaehlt.
 */
public class MixerScreen extends Screen {
    private static final int SIDE_W = 120, TILE = 36, GAP = 4;
    private static final int RED = 0xFFE0506A, WHITE = 0xFFFFFFFF, GRAY = 0xFFAAAAAA;

    private record Preview(Identifier id, int w, int h) {}
    private record Tile(int x, int y, int opt) {}

    private final Screen parent;
    private final Category[] cats = Category.values();
    private final Map<Category, List<String>> options = new EnumMap<>(Category.class);
    private final Map<Category, Integer> index = new EnumMap<>(Category.class);
    private final Map<String, Preview> previews = new HashMap<>();
    private final List<Identifier> registered = new ArrayList<>();
    private final List<Tile> tiles = new ArrayList<>();
    private Category current = Category.values()[0];
    private int page = 0;
    private int gridX = SIDE_W + 25;
    private String status = "";

    public MixerScreen(Screen parent) {
        super(Component.literal("Texture Mixer"));
        this.parent = parent;
        Map<Category, List<String>> scanned = PackTools.scan();
        Map<String, String> saved = MixerConfig.load();
        for (Category c : cats) {
            List<String> opts = new ArrayList<>();
            opts.add(null); // Standard
            opts.addAll(scanned.get(c));
            options.put(c, opts);
            index.put(c, Math.max(opts.indexOf(saved.get(c.name())), 0));
        }
    }

    @Override
    protected void init() {
        tiles.clear();
        Tabs.add(this.width, 0, this::addRenderableWidget);

        // Kategorien links
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            addRenderableWidget(Button.builder(Component.literal(c.label), b -> {
                current = c;
                page = 0;
                rebuildWidgets();
            }).bounds(10, 30 + i * 22, SIDE_W, 20).build());
        }

        // Kacheln rechts
        int gridY = 40;
        int cols = Math.max(1, (this.width - gridX - 10) / (TILE + GAP));
        int rows = Math.max(1, (this.height - gridY - 76) / (TILE + GAP));
        int perPage = cols * rows;
        List<String> opts = options.get(current);
        int start = page * perPage;
        Category cat = current;

        for (int k = start; k < Math.min(opts.size(), start + perPage); k++) {
            int pos = k - start;
            int x = gridX + (pos % cols) * (TILE + GAP);
            int y = gridY + (pos / cols) * (TILE + GAP);
            final int opt = k;
            String name = opts.get(k) == null ? "Standard (nichts überschreiben)" : opts.get(k);
            addRenderableWidget(Button.builder(Component.empty(), b -> index.put(cat, opt))
                    .bounds(x, y, TILE, TILE)
                    .tooltip(Tooltip.create(Component.literal(name)))
                    .build());
            tiles.add(new Tile(x, y, k));
        }

        // Seiten-Buttons
        if (opts.size() > perPage) {
            int maxPage = (opts.size() - 1) / perPage;
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                if (page > 0) { page--; rebuildWidgets(); }
            }).bounds(gridX, this.height - 56, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                if (page < maxPage) { page++; rebuildWidgets(); }
            }).bounds(gridX + 24, this.height - 56, 20, 20).build());
        }

        // Unten
        addRenderableWidget(Button.builder(Component.literal("Anwenden"), b -> apply())
                .bounds(this.width / 2 - 105, this.height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
                .bounds(this.width / 2 + 5, this.height - 28, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, current.label, gridX, 28, WHITE, true);

        int ci = Arrays.asList(cats).indexOf(current);
        graphics.outline(9, 29 + ci * 22, SIDE_W + 2, 22, RED);

        List<String> opts = options.get(current);
        int selected = index.get(current);
        for (Tile t : tiles) {
            String pack = opts.get(t.opt());
            if (pack == null) {
                graphics.text(this.font, "Std", t.x() + 10, t.y() + 14, GRAY, false);
            } else {
                Preview p = preview(current, pack);
                if (p != null) drawPreview(graphics, p, t.x(), t.y());
                else graphics.text(this.font, initials(pack), t.x() + 10, t.y() + 14, GRAY, false);
            }
            if (t.opt() == selected) graphics.outline(t.x() - 1, t.y() - 1, TILE + 2, TILE + 2, RED);
        }

        String sel = opts.get(selected);
        graphics.text(this.font, current.label + ": " + (sel == null ? "Standard" : sel),
                gridX, this.height - 40, WHITE, true);
        if (!status.isEmpty()) graphics.text(this.font, status, 10, this.height - 40 + 0, 0xFF9BE08A, true);
    }

    private void drawPreview(GuiGraphicsExtractor g, Preview p, int x, int y) {
        int u = 0, v = 0, rw = p.w(), rh = p.h();
        float[] r = current.region;
        if (r != null) {
            u = (int) (r[0] * p.w());
            v = (int) (r[1] * p.h());
            rw = Math.max(1, (int) (r[2] * p.w()));
            rh = Math.max(1, (int) (r[3] * p.h()));
        } else if (p.h() > p.w() && p.h() % p.w() == 0) {
            rh = p.w(); // animierte Textur: nur erstes Frame
        }
        g.blit(RenderPipelines.GUI_TEXTURED, p.id(), x + 2, y + 2, (float) u, (float) v,
                TILE - 4, TILE - 4, rw, rh, p.w(), p.h());
    }

    private static String initials(String pack) {
        String s = pack.replaceAll("(?i)\\.zip$", "");
        return s.length() <= 2 ? s : s.substring(0, 2);
    }

    /** Laedt die Vorschau einmalig und registriert sie als Textur. */
    private Preview preview(Category c, String pack) {
        String key = c.name() + "|" + pack;
        if (previews.containsKey(key)) return previews.get(key);
        Preview result = null;
        byte[] data = PackTools.readPreview(c, pack);
        if (data != null) {
            try {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(data));
                Identifier id = Identifier.fromNamespaceAndPath("texturemixer",
                        "preview/p" + Integer.toHexString(key.hashCode()));
                Minecraft.getInstance().getTextureManager()
                        .register(id, new DynamicTexture(() -> "texturemixer preview", img)); // API-RISIKO: Konstruktor
                registered.add(id);
                result = new Preview(id, img.getWidth(), img.getHeight());
            } catch (Exception e) {
                System.err.println("[TextureMixer] Vorschau fehlgeschlagen " + key + ": " + e);
            }
        }
        previews.put(key, result);
        return result;
    }

    private void apply() {
        Map<String, String> selection = new HashMap<>();
        for (Category c : cats) {
            String name = options.get(c).get(index.get(c));
            if (name != null) selection.put(c.name(), name);
        }
        MixerConfig.save(selection);
        try {
            int n = PackTools.build(selection);
            PackActivator.enableAndReload();
            status = n + " Dateien übernommen, Pack neu geladen.";
        } catch (Exception e) {
            e.printStackTrace();
            status = "Fehler: " + e.getMessage();
        }
    }

    @Override
    public void onClose() {
        for (Identifier id : registered) Minecraft.getInstance().getTextureManager().release(id);
        registered.clear();
        Minecraft.getInstance().setScreenAndShow(parent);
    }
}
