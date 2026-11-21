package de.texturemixer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Speichert: Kategorie-Name -> Dateiname des gewaehlten Packs. */
public final class MixerConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("texturemixer.json");
    private static final Path PRESETS = FabricLoader.getInstance().getConfigDir().resolve("texturemixer_presets.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MixerConfig() {}

    public static Map<String, String> load() {
        try {
            if (Files.exists(FILE)) {
                Map<String, String> m = GSON.fromJson(Files.readString(FILE),
                        new TypeToken<Map<String, String>>() {}.getType());
                if (m != null) return m;
            }
        } catch (Exception e) {
            System.err.println("[TextureMixer] Config konnte nicht gelesen werden: " + e);
        }
        return new HashMap<>();
    }

    public static void save(Map<String, String> map) {
        try {
            Files.writeString(FILE, GSON.toJson(map));
        } catch (Exception e) {
            System.err.println("[TextureMixer] Config konnte nicht gespeichert werden: " + e);
        }
    }

    /** Presets: Name -> (Kategorie -> Pack). */
    public static Map<String, Map<String, String>> loadPresets() {
        try {
            if (Files.exists(PRESETS)) {
                Map<String, Map<String, String>> m = GSON.fromJson(Files.readString(PRESETS),
                        new TypeToken<Map<String, Map<String, String>>>() {}.getType());
                if (m != null) return m;
            }
        } catch (Exception e) {
            System.err.println("[TextureMixer] Presets nicht lesbar: " + e);
        }
        return new java.util.TreeMap<>();
    }

    public static void savePresets(Map<String, Map<String, String>> m) {
        try {
            Files.writeString(PRESETS, GSON.toJson(m));
        } catch (Exception e) {
            System.err.println("[TextureMixer] Presets nicht speicherbar: " + e);
        }
    }
}
