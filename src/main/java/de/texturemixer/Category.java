package de.texturemixer;

import java.util.regex.Pattern;

/**
 * Kategorien. Alle Regexe gelten relativ zu "assets/minecraft/" im Texturepack.
 * "preview" = Textur(en) (durch '|' getrennt, erste vorhandene gewinnt), die als Kachel gezeigt wird.
 * Eigene Kategorien: einfach hier eine Zeile ergaenzen.
 */
public enum Category {
    CRYSTALS("Crystals", "textures/item/end_crystal.png",
            "textures/entity/end_crystal/.*",
            "textures/item/end_crystal\\.png(\\.mcmeta)?",
            "models/item/end_crystal\\.json",
            "items/end_crystal\\.json"),

    ANCHORS("Anchors", "textures/block/respawn_anchor_side0.png|textures/block/respawn_anchor_top.png",
            "textures/block/respawn_anchor.*\\.png(\\.mcmeta)?",
            "models/block/respawn_anchor.*\\.json",
            "blockstates/respawn_anchor\\.json",
            "items/respawn_anchor\\.json"),

    OBSIDIAN("Obsidian", "textures/block/obsidian.png",
            "textures/block/(crying_)?obsidian\\.png(\\.mcmeta)?",
            "models/block/(crying_)?obsidian.*\\.json",
            "blockstates/(crying_)?obsidian\\.json",
            "items/(crying_)?obsidian\\.json"),

    GLOWSTONE("Glowstone", "textures/block/glowstone.png",
            "textures/block/glowstone\\.png(\\.mcmeta)?",
            "models/block/glowstone.*\\.json",
            "blockstates/glowstone\\.json",
            "items/glowstone\\.json"),

    ARMOR("Rüstung",
            "textures/item/netherite_chestplate.png|textures/item/diamond_chestplate.png|textures/item/iron_chestplate.png",
            "textures/entity/equipment/humanoid(_leggings)?/.*",
            "models/equipment/.*",
            "textures/item/" + armorItem() + "(_overlay)?\\.png(\\.mcmeta)?",
            "(models/item|items)/" + armorItem() + "\\.json"),

    TOOLS("Tools",
            "textures/item/netherite_sword.png|textures/item/diamond_sword.png|textures/item/iron_sword.png",
            "textures/item/" + toolItem() + "\\.png(\\.mcmeta)?",
            "(models/item|items)/" + toolItem() + "\\.json"),

    PEARLS("Perlen", "textures/item/ender_pearl.png",
            "textures/item/ender_pearl\\.png(\\.mcmeta)?",
            "(models/item|items)/ender_pearl\\.json"),

    GAPS("Gaps", "textures/item/golden_apple.png",
            "textures/item/golden_apple\\.png(\\.mcmeta)?",
            "(models/item|items)/golden_apple\\.json"),

    EGAPS("E-Gaps", "textures/item/enchanted_golden_apple.png|textures/item/golden_apple.png",
            "textures/item/enchanted_golden_apple\\.png(\\.mcmeta)?",
            "(models/item|items)/enchanted_golden_apple\\.json"),

    TOTEMS("Totems", "textures/item/totem_of_undying.png",
            "textures/item/totem_of_undying\\.png(\\.mcmeta)?",
            "(models/item|items)/totem_of_undying\\.json"),

    // Inventar-GUI: Hintergrund des Inventars, Hotbar, Slots. Vorschau = oberer linker Teil der 256x256-Datei.
    INVENTORY("Inventory", "textures/gui/container/inventory.png",
            new float[]{0f, 0f, 176f / 256f, 166f / 256f},
            "textures/gui/container/inventory\\.png(\\.mcmeta)?",
            "textures/gui/sprites/hud/hotbar.*",
            "textures/gui/sprites/container/slot.*",
            "textures/gui/sprites/container/inventory/.*");

    private static String armorItem() {
        return "(leather|chainmail|iron|golden|diamond|netherite|copper|turtle)_(helmet|chestplate|leggings|boots)";
    }

    private static String toolItem() {
        return "(((wooden|stone|copper|iron|golden|diamond|netherite)_(sword|axe|pickaxe|shovel|hoe|spear))"
                + "|mace|trident|(bow|crossbow)(_pulling_[0-2]|_arrow|_firework)?)";
    }

    public final String label;
    public final String[] previews;
    /** {u, v, w, h} als Anteil 0..1 der Bildgroesse, oder null = ganzes Bild */
    public final float[] region;
    private final Pattern[] patterns;

    Category(String label, String preview, String... regexes) {
        this(label, preview, (float[]) null, regexes);
    }

    Category(String label, String preview, float[] region, String... regexes) {
        this.label = label;
        this.previews = preview.split("\\|");
        this.region = region;
        this.patterns = new Pattern[regexes.length];
        for (int i = 0; i < regexes.length; i++) patterns[i] = Pattern.compile(regexes[i]);
    }

    /** @param rel Pfad relativ zu assets/minecraft/ mit '/' */
    public boolean matches(String rel) {
        for (Pattern p : patterns) if (p.matcher(rel).matches()) return true;
        return false;
    }
}
