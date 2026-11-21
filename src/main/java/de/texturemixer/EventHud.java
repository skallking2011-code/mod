package de.texturemixer;

import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** "Hologramm"-Panel oben rechts: schwebendes, vergroessertes Item + Live-Infos. */
public final class EventHud {
    private static final int CYAN = 0xFF7FEFFF, WHITE = 0xFFFFFFFF, GRAY = 0xFFB0B8C0, GREEN = 0xFF7DFF8A, GOLD = 0xFFFFD84A;

    public static boolean visible = true;

    private EventHud() {}

    private record Line(String text, int color) {}

    public static void extract(GuiGraphicsExtractor g, DeltaTracker dt) {
        EventState e = EventManager.current;
        Minecraft mc = Minecraft.getInstance();
        if (!visible || e == null || mc.player == null) return;

        List<Line> head = new ArrayList<>();
        List<Line> body = new ArrayList<>();
        boolean giveaway = e.type == EventState.Type.GIVEAWAY;
        head.add(new Line(giveaway ? "GIVEAWAY" : "AUKTION", CYAN));
        head.add(new Line(EventManager.itemName(e), WHITE));

        if (e.closed) {
            head.add(new Line("BEENDET", GOLD));
            body.add(new Line(e.winner == null ? "Kein Gewinner" : "Gewinner: " + e.winner, e.winner == null ? GRAY : GREEN));
            if (!giveaway && e.winner != null) body.add(new Line("Preis: " + EventManager.fmt(e.highest), GOLD));
        } else {
            long left = Math.max(0, e.endAt - System.currentTimeMillis()) / 1000;
            head.add(new Line(String.format("Endet in %d:%02d", left / 60, left % 60), GOLD));
            if (giveaway) {
                body.add(new Line("Schreib mir: " + e.keyword, GRAY));
            } else {
                body.add(new Line("Mindestgebot: " + EventManager.fmt(e.minBid), GRAY));
                body.add(new Line(e.highestBidder == null ? "Noch kein Gebot"
                        : "Höchstgebot: " + EventManager.fmt(e.highest) + " (" + e.highestBidder + ")", GREEN));
            }
        }

        if (giveaway) {
            body.add(new Line("Teilnehmer: " + e.participants.size(), CYAN));
            List<String> names = new ArrayList<>(e.participants);
            int from = Math.max(0, names.size() - 8);
            for (int i = from; i < names.size(); i++) body.add(new Line(" - " + names.get(i), WHITE));
            if (from > 0) body.add(new Line("  +" + from + " weitere", GRAY));
        } else if (!e.bidLog.isEmpty()) {
            body.add(new Line("Gebote:", CYAN));
            int from = Math.max(0, e.bidLog.size() - 5);
            for (int i = e.bidLog.size() - 1; i >= from; i--) body.add(new Line(" " + e.bidLog.get(i), WHITE));
        }

        var font = mc.font;
        int w = 200;
        int x = mc.getWindow().getGuiScaledWidth() - w - 8;
        int y = 8;
        int h = 10 + 54 + 6 + body.size() * 10 + 6;

        g.fill(x, y, x + w, y + h, 0xB0061018);
        g.outline(x, y, w, h, 0xFF4FD8FF);
        g.fill(x, y + 62, x + w, y + 63, 0x804FD8FF); // Trennlinie

        // schwebendes, vergroessertes Item (Hologramm-Effekt)
        int bob = (int) Math.round(Math.sin(Util.getMillis() / 350.0) * 2.0);
        var pose = g.pose();                       // API-RISIKO: Matrix3x2fStack
        pose.pushMatrix();
        pose.translate((float) (x + 8), (float) (y + 8 + bob));
        pose.scale(3f, 3f);
        g.item(e.item, 0, 0);                      // API-RISIKO: Methodenname
        pose.popMatrix();

        int ty = y + 10;
        for (Line l : head) {
            g.text(font, l.text(), x + 66, ty, l.color(), true);
            ty += 12;
        }
        int by = y + 68;
        for (Line l : body) {
            g.text(font, l.text(), x + 8, by, l.color(), false);
            by += 10;
        }
    }
}
