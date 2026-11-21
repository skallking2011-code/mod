package de.texturemixer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** /tmpreset save|load|delete <name>  und  /tmpreset list */
public final class PresetCommands {
    private PresetCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(literal("tmpreset")
                .then(literal("save").then(argument("name", StringArgumentType.word()).executes(c -> {
                    String name = StringArgumentType.getString(c, "name");
                    Map<String, Map<String, String>> all = new TreeMap<>(MixerConfig.loadPresets());
                    all.put(name, new HashMap<>(MixerConfig.load()));
                    MixerConfig.savePresets(all);
                    return msg(c, "§aPreset '" + name + "' gespeichert (aktuelle, angewendete Auswahl).");
                })))
                .then(literal("load").then(argument("name", StringArgumentType.word()).executes(c -> {
                    String name = StringArgumentType.getString(c, "name");
                    Map<String, String> sel = MixerConfig.loadPresets().get(name);
                    if (sel == null) return msg(c, "§cPreset nicht gefunden: " + name);
                    try {
                        MixerConfig.save(sel);
                        int n = PackTools.build(sel);
                        PackActivator.enableAndReload();
                        return msg(c, "§aPreset '" + name + "' geladen (" + n + " Dateien).");
                    } catch (Exception e) {
                        return msg(c, "§cFehler: " + e.getMessage());
                    }
                })))
                .then(literal("delete").then(argument("name", StringArgumentType.word()).executes(c -> {
                    String name = StringArgumentType.getString(c, "name");
                    Map<String, Map<String, String>> all = new TreeMap<>(MixerConfig.loadPresets());
                    if (all.remove(name) == null) return msg(c, "§cPreset nicht gefunden: " + name);
                    MixerConfig.savePresets(all);
                    return msg(c, "§aPreset '" + name + "' gelöscht.");
                })))
                .then(literal("list").executes(c -> {
                    var all = MixerConfig.loadPresets();
                    return msg(c, all.isEmpty() ? "Keine Presets." : "Presets: " + String.join(", ", all.keySet()));
                })));
    }

    private static int msg(CommandContext<FabricClientCommandSource> c, String text) {
        c.getSource().sendFeedback(Component.literal(text));
        return 1;
    }
}
