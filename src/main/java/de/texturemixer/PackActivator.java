package de.texturemixer;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;

public final class PackActivator {
    private PackActivator() {}

    /** Aktiviert das TextureMixer-Pack ganz oben und laedt die Ressourcen neu. */
    public static void enableAndReload() {
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();
        repo.reload();

        String id = "file/" + PackTools.OUTPUT_NAME;
        List<String> before = new ArrayList<>(repo.getSelectedIds());
        List<String> ids = new ArrayList<>(before);
        ids.remove(id);
        ids.add(id); // letzter Eintrag = hoechste Prioritaet

        repo.setSelected(ids);
        mc.options.updateResourcePacks(repo);
        if (ids.equals(before)) {
            // Auswahl unveraendert -> updateResourcePacks laedt nicht neu
            mc.reloadResourcePacks();
        }
    }
}
