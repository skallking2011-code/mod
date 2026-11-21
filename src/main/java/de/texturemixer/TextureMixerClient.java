package de.texturemixer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class TextureMixerClient implements ClientModInitializer {
    private static volatile boolean openRequested;

    @Override
    public void onInitializeClient() {
        // /texturemixer  (oder /tm) oeffnet das Menue
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var node = dispatcher.register(ClientCommands.literal("texturemixer").executes(ctx -> {
                openRequested = true;
                return 1;
            }));
            dispatcher.register(ClientCommands.literal("tm").redirect(node));
            EventCommands.register(dispatcher);
            PresetCommands.register(dispatcher);
        });

        // Giveaways / Auktionen: HUD + Chat mitlesen
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("texturemixer", "events"), EventHud::extract);
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (!overlay) EventManager.onText(msg.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((msg, signed, sender, params, ts) -> {
            String t = msg.getString();
            if (sender != null && !t.contains(sender.name())) t = sender.name() + ": " + t; // API-RISIKO: name()
            EventManager.onText(t);
        });

        // Screen erst im naechsten Tick oeffnen, sonst schliesst der Chat es gleich wieder
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            EventManager.tick(mc);
            if (openRequested) {
                openRequested = false;
                mc.setScreenAndShow(new MixerScreen(null));
            }
        });
    }
}
