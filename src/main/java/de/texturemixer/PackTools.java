package de.texturemixer;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class PackTools {
    public static final String OUTPUT_NAME = "TextureMixer";
    private static final String PREFIX = "assets/minecraft/";

    // min/max_format bewusst weit gefasst, damit das Pack nicht als "inkompatibel" markiert wird.
    private static final String MCMETA =
            "{\"pack\":{\"description\":\"Texture Mixer (automatisch erzeugt)\",\"min_format\":1,\"max_format\":1000}}";

    private PackTools() {}

    public static Path packDir() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
    }

    private interface Visitor { void visit(String rel, Path file) throws IOException; }

    /** Alle Packs (Ordner oder .zip), ausser unserem eigenen Output. */
    public static List<Path> listPacks() throws IOException {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(packDir())) return out;
        try (var s = Files.list(packDir())) {
            for (Path p : (Iterable<Path>) s.sorted()::iterator) {
                String n = p.getFileName().toString();
                if (n.equals(OUTPUT_NAME)) continue;
                if (Files.isDirectory(p) || n.toLowerCase(Locale.ROOT).endsWith(".zip")) out.add(p);
            }
        }
        return out;
    }

    /** Welche Packs enthalten etwas fuer welche Kategorie? (Dateinamen der Packs) */
    public static Map<Category, List<String>> scan() {
        Map<Category, List<String>> result = new EnumMap<>(Category.class);
        for (Category c : Category.values()) result.put(c, new ArrayList<>());
        List<Path> packs;
        try {
            packs = listPacks();
        } catch (IOException e) {
            System.err.println("[TextureMixer] Pack-Liste fehlgeschlagen: " + e);
            return result;
        }
        for (Path pack : packs) {
            Set<Category> found = EnumSet.noneOf(Category.class);
            try {
                walkPack(pack, (rel, f) -> {
                    if (!rel.startsWith(PREFIX)) return;
                    String r = rel.substring(PREFIX.length());
                    for (Category c : Category.values()) if (c.matches(r)) found.add(c);
                });
            } catch (Exception e) {
                System.err.println("[TextureMixer] Pack uebersprungen " + pack.getFileName() + ": " + e);
                continue;
            }
            for (Category c : found) result.get(c).add(pack.getFileName().toString());
        }
        return result;
    }

    /** Baut resourcepacks/TextureMixer neu. Rueckgabe: Anzahl kopierter Dateien. */
    public static int build(Map<String, String> selection) throws IOException {
        Path out = packDir().resolve(OUTPUT_NAME);
        deleteRecursive(out);
        Files.createDirectories(out);
        Files.writeString(out.resolve("pack.mcmeta"), MCMETA);
        int[] count = {0};
        for (Category c : Category.values()) {
            String name = selection.get(c.name());
            if (name == null) continue;
            Path pack = packDir().resolve(name);
            if (!Files.exists(pack)) continue;
            walkPack(pack, (rel, f) -> {
                if (rel.startsWith(PREFIX) && c.matches(rel.substring(PREFIX.length()))) {
                    Path dst = out.resolve(rel);
                    Files.createDirectories(dst.getParent());
                    Files.copy(f, dst, StandardCopyOption.REPLACE_EXISTING);
                    count[0]++;
                }
            });
        }
        return count[0];
    }

    private static void walkPack(Path pack, Visitor v) throws IOException {
        if (Files.isDirectory(pack)) {
            walkRoot(pack, v);
        } else {
            try (FileSystem fs = FileSystems.newFileSystem(pack)) {
                walkRoot(fs.getPath("/"), v);
            }
        }
    }

    private static Path findRoot(Path root) throws IOException {
        if (Files.isDirectory(root.resolve("assets"))) return root;
        // Manche Zips haben noch einen Unterordner
        try (var s = Files.list(root)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (Files.isDirectory(p) && Files.isDirectory(p.resolve("assets"))) return p;
            }
        }
        return root;
    }

    private static void walkRoot(Path root0, Visitor v) throws IOException {
        Path root = findRoot(root0);
        Path assets = root.resolve("assets");
        if (!Files.isDirectory(assets)) return;
        List<Path> files;
        try (var s = Files.walk(assets)) {
            files = s.filter(Files::isRegularFile).toList();
        }
        for (Path f : files) {
            String rel = root.relativize(f).toString().replace('\\', '/');
            v.visit(rel, f);
        }
    }

    private static void deleteRecursive(Path p) throws IOException {
        if (!Files.exists(p)) return;
        try (var s = Files.walk(p)) {
            for (Path f : (Iterable<Path>) s.sorted(Comparator.reverseOrder())::iterator) Files.delete(f);
        }
    }

    /** Liest die Vorschau-Textur (PNG-Bytes) einer Kategorie aus einem Pack, oder null. */
    public static byte[] readPreview(Category c, String packName) {
        Path pack = packDir().resolve(packName);
        try {
            if (Files.isDirectory(pack)) return readPreviewFrom(pack, c);
            try (FileSystem fs = FileSystems.newFileSystem(pack)) {
                return readPreviewFrom(fs.getPath("/"), c);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readPreviewFrom(Path root0, Category c) throws IOException {
        Path root = findRoot(root0);
        for (String cand : c.previews) {
            Path p = root.resolve("assets/minecraft/" + cand);
            if (Files.isRegularFile(p)) return Files.readAllBytes(p);
        }
        return null;
    }
}
