package de.texturemixer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Einstellungen + Live-Status fuer Giveaway (auction=false) bzw. Auktion (auction=true). */
public class EventScreen extends Screen {
    private static String lastMin = "10", lastKeyword = "join", lastBid = "1000";

    private final boolean auction;
    private EditBox minutesBox, secondBox;
    private String status = "";

    public EventScreen(boolean auction) {
        super(Component.literal(auction ? "Auktion" : "Giveaway"));
        this.auction = auction;
    }

    @Override
    protected void init() {
        Tabs.add(this.width, auction ? 3 : 2, this::addRenderableWidget);
        int cx = this.width / 2;

        minutesBox = new EditBox(this.font, cx - 100, 62, 200, 20, Component.literal("Minuten"));
        minutesBox.setValue(lastMin);
        addRenderableWidget(minutesBox);

        secondBox = new EditBox(this.font, cx - 100, 100, 200, 20, Component.literal(auction ? "Mindestgebot" : "Keyword"));
        secondBox.setValue(auction ? lastBid : lastKeyword);
        addRenderableWidget(secondBox);

        addRenderableWidget(Button.builder(Component.literal("Starten"), b -> start())
                .bounds(cx - 100, 128, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Jetzt beenden"), b -> status = orOk(EventManager.endNow(), "Beendet."))
                .bounds(cx + 2, 128, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Abbrechen"), b -> status = orOk(EventManager.cancel(), "Abgebrochen."))
                .bounds(cx - 100, 152, 98, 20).build());
        if (!auction) {
            addRenderableWidget(Button.builder(Component.literal("Neu losen"), b -> status = orOk(EventManager.reroll(), "Neu gelost."))
                    .bounds(cx + 2, 152, 98, 20).build());
        }

        addRenderableWidget(Button.builder(label("Hologramm", EventHud.visible), b -> {
            EventHud.visible = !EventHud.visible;
            b.setMessage(label("Hologramm", EventHud.visible));
        }).bounds(cx - 100, 176, 98, 20).build());
        addRenderableWidget(Button.builder(label("Ansagen", EventManager.announce), b -> {
            EventManager.announce = !EventManager.announce;
            b.setMessage(label("Ansagen", EventManager.announce));
        }).bounds(cx + 2, 176, 98, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Schließen"), b -> onClose())
                .bounds(cx - 50, this.height - 28, 100, 20).build());
    }

    private static Component label(String name, boolean on) {
        return Component.literal(name + ": " + (on ? "AN" : "AUS"));
    }

    private static String orOk(String err, String ok) {
        return err != null ? err : ok;
    }

    private void start() {
        int min;
        try {
            min = Math.max(1, Math.min(720, Integer.parseInt(minutesBox.getValue().trim())));
        } catch (NumberFormatException e) {
            status = "Dauer ungültig.";
            return;
        }
        lastMin = String.valueOf(min);
        String err;
        if (auction) {
            long bid = EventManager.parseMin(secondBox.getValue());
            if (bid < 0) { status = "Mindestgebot ungültig (z.B. 500, 10k, 1.5m)."; return; }
            lastBid = secondBox.getValue().trim();
            err = EventManager.start(EventState.Type.AUCTION, min, "", bid);
        } else {
            String kw = secondBox.getValue().trim().split("\\s+")[0];
            if (kw.isEmpty()) { status = "Bitte ein Keyword eingeben."; return; }
            lastKeyword = kw;
            err = EventManager.start(EventState.Type.GIVEAWAY, min, kw, 0);
        }
        status = err != null ? err : "Gestartet.";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        super.extractRenderState(g, mx, my, d);
        int cx = this.width / 2;
        var mc = Minecraft.getInstance();

        String held = mc.player == null || mc.player.getMainHandItem().isEmpty()
                ? "(nichts in der Hand!)" : mc.player.getMainHandItem().getHoverName().getString();
        g.text(this.font, (auction ? "Auktionsitem: " : "Verlosung: ") + held, cx - 100, 34, 0xFFFFD84A, true);
        g.text(this.font, "Dauer (Minuten)", cx - 100, 51, 0xFFFFFFFF, false);
        g.text(this.font, auction ? "Mindestgebot (500, 10k, 1.5m)" : "Keyword (Spieler schreibt dir das per /msg)",
                cx - 100, 89, 0xFFFFFFFF, false);

        if (!status.isEmpty()) g.text(this.font, status, cx - 100, 202, 0xFF9BE08A, true);

        // Live-Status
        EventState e = EventManager.current;
        boolean match = e != null && (e.type == EventState.Type.AUCTION) == auction;
        int y = 218;
        if (!match) {
            g.text(this.font, "Kein aktives Event.", cx - 100, y, 0xFFB0B8C0, false);
            return;
        }
        if (e.closed) {
            g.text(this.font, e.winner == null ? "Beendet - kein Gewinner" : "Gewinner: " + e.winner, cx - 100, y, 0xFF7DFF8A, true);
            return;
        }
        long left = Math.max(0, e.endAt - System.currentTimeMillis()) / 1000;
        g.text(this.font, String.format("Läuft noch %d:%02d", left / 60, left % 60), cx - 100, y, 0xFF7FEFFF, true);
        y += 12;
        if (auction) {
            g.text(this.font, e.highestBidder == null ? "Noch kein Gebot"
                    : "Höchstgebot: " + EventManager.fmt(e.highest) + " (" + e.highestBidder + ")", cx - 100, y, 0xFFFFFFFF, false);
        } else {
            List<String> names = new ArrayList<>(e.participants);
            g.text(this.font, "Teilnehmer: " + names.size(), cx - 100, y, 0xFFFFFFFF, false);
            y += 12;
            int from = Math.max(0, names.size() - 6);
            for (int i = from; i < names.size() && y < this.height - 40; i++, y += 10)
                g.text(this.font, " - " + names.get(i), cx - 100, y, 0xFFB0B8C0, false);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(null);
    }
}
