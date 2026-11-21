package de.texturemixer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class EventCommands {
    private EventCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(literal("giveaway")
                .then(literal("start").then(argument("minuten", IntegerArgumentType.integer(1, 720))
                        .then(argument("keyword", StringArgumentType.word()).executes(c -> {
                            String err = EventManager.start(EventState.Type.GIVEAWAY,
                                    IntegerArgumentType.getInteger(c, "minuten"),
                                    StringArgumentType.getString(c, "keyword"), 0);
                            return reply(c, err, "Giveaway gestartet.");
                        })))
                )
                .then(literal("end").executes(c -> reply(c, EventManager.endNow(), "Giveaway beendet.")))
                .then(literal("cancel").executes(c -> reply(c, EventManager.cancel(), "Abgebrochen.")))
                .then(literal("reroll").executes(c -> reply(c, EventManager.reroll(), "Neu gelost.")))
                .then(literal("list").executes(c -> {
                    EventState e = EventManager.current;
                    if (e == null) return reply(c, "Kein Event aktiv.", null);
                    String names = e.type == EventState.Type.GIVEAWAY
                            ? String.join(", ", e.participants)
                            : String.join(" | ", e.bidLog);
                    return reply(c, null, (e.type == EventState.Type.GIVEAWAY
                            ? "Teilnehmer (" + e.participants.size() + "): "
                            : "Gebote: ") + (names.isEmpty() ? "-" : names));
                })));

        d.register(literal("auction")
                .then(literal("start").then(argument("minuten", IntegerArgumentType.integer(1, 720))
                        .then(argument("mindestgebot", StringArgumentType.word()).executes(c -> {
                            long min = EventManager.parseMin(StringArgumentType.getString(c, "mindestgebot"));
                            if (min < 0) return reply(c, "Mindestgebot ungültig (z.B. 500, 10k, 1.5m).", null);
                            String err = EventManager.start(EventState.Type.AUCTION,
                                    IntegerArgumentType.getInteger(c, "minuten"), "", min);
                            return reply(c, err, "Auktion gestartet.");
                        })))
                )
                .then(literal("end").executes(c -> reply(c, EventManager.endNow(), "Auktion beendet.")))
                .then(literal("cancel").executes(c -> reply(c, EventManager.cancel(), "Abgebrochen."))));

        d.register(literal("eventannounce").executes(c -> {
            EventManager.announce = !EventManager.announce;
            return reply(c, null, "Chat-Ansagen: " + (EventManager.announce ? "AN" : "AUS"));
        }));
    }

    private static int reply(CommandContext<FabricClientCommandSource> c, String error, String ok) {
        if (error != null) {
            c.getSource().sendFeedback(Component.literal("§c" + error));
            return 0;
        }
        if (ok != null) c.getSource().sendFeedback(Component.literal("§a" + ok));
        return 1;
    }
}
