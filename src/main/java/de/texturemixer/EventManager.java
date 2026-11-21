package de.texturemixer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Logik fuer Giveaways und Auktionen (rein clientseitig, liest den Chat mit). */
public final class EventManager {
    public static volatile EventState current;
    public static boolean announce = true;

    private static final long RESULT_SHOW_MS = 30_000;
    private static final Pattern BID = Pattern.compile(
            "(?i)(?:^|[\\s:\\]>])!?bid\\s+([0-9][0-9.,]*)\\s*([kmb]?)\\s*$");

    private EventManager() {}

    // ---------- Start / Ende ----------

    public static String start(EventState.Type type, int minutes, String keyword, long minBid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return "Nicht im Spiel.";
        if (current != null && !current.closed) return "Es läuft schon ein Event. Erst /giveaway cancel bzw. /auction cancel.";
        var stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) return "Halte das Item, das du verlosen/versteigern willst, in der Hand.";

        long endAt = System.currentTimeMillis() + minutes * 60_000L;
        EventState e = new EventState(type, stack.copy(), keyword, endAt, minBid);
        current = e;

        String item = itemName(e);
        String me = mc.getUser().getName();
        if (type == EventState.Type.GIVEAWAY) {
            say("[Giveaway] " + item + " wird verlost! Schreib mir '" + keyword + "' per /msg " + me
                    + " um mitzumachen. Endet in " + minutes + " Min.");
        } else {
            say("[Auktion] " + item + " | Mindestgebot: " + fmt(minBid) + " | Biete mit /msg " + me
                    + " bid <Betrag> (auch 10k / 1.5m). Endet in " + minutes + " Min.");
        }
        return null;
    }

    public static String cancel() {
        if (current == null) return "Kein Event aktiv.";
        current = null;
        say("[Event] Wurde abgebrochen.");
        return null;
    }

    public static String endNow() {
        if (current == null || current.closed) return "Kein laufendes Event.";
        finish();
        return null;
    }

    public static String reroll() {
        EventState e = current;
        if (e == null || !e.closed || e.type != EventState.Type.GIVEAWAY) return "Nur nach einem beendeten Giveaway möglich.";
        List<String> pool = new ArrayList<>(e.participants);
        pool.removeAll(e.pastWinners);
        if (pool.isEmpty()) return "Keine weiteren Teilnehmer.";
        e.winner = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        e.pastWinners.add(e.winner);
        e.closedAt = System.currentTimeMillis();
        say("[Giveaway] Neuer Gewinner von " + itemName(e) + ": " + e.winner + "!");
        return null;
    }

    private static void finish() {
        EventState e = current;
        if (e == null || e.closed) return;
        e.closed = true;
        e.closedAt = System.currentTimeMillis();
        if (e.type == EventState.Type.GIVEAWAY) {
            if (e.participants.isEmpty()) {
                say("[Giveaway] Beendet - leider hat niemand mitgemacht.");
            } else {
                List<String> l = new ArrayList<>(e.participants);
                e.winner = l.get(ThreadLocalRandom.current().nextInt(l.size()));
                e.pastWinners.add(e.winner);
                say("[Giveaway] Gewinner von " + itemName(e) + ": " + e.winner + "! (" + l.size() + " Teilnehmer)");
            }
        } else {
            if (e.highestBidder == null) {
                say("[Auktion] Beendet - kein Gebot für " + itemName(e) + ".");
            } else {
                e.winner = e.highestBidder;
                say("[Auktion] " + itemName(e) + " geht an " + e.winner + " für " + fmt(e.highest) + "!");
            }
        }
    }

    public static void tick(Minecraft mc) {
        EventState e = current;
        if (e == null || mc.player == null) return;
        long now = System.currentTimeMillis();
        if (!e.closed && now >= e.endAt) finish();
        else if (e.closed && now - e.closedAt > RESULT_SHOW_MS) current = null;
    }

    // ---------- Chat auswerten ----------

    /** Wird mit jeder empfangenen Chat-/Systemnachricht aufgerufen. */
    public static void onText(String text) {
        EventState e = current;
        Minecraft mc = Minecraft.getInstance();
        if (e == null || e.closed || mc.player == null || mc.getConnection() == null || text == null) return;

        String self = mc.getUser().getName();
        String sender = null;
        int sEnd = -1, sStart = Integer.MAX_VALUE;
        for (PlayerInfo pi : mc.getConnection().getOnlinePlayers()) {
            String n = pi.getProfile().name(); // API-RISIKO: evtl. getName()
            Matcher m = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(n) + "(?![A-Za-z0-9_])").matcher(text);
            if (m.find() && m.start() < sStart) {
                sStart = m.start();
                sEnd = m.end();
                sender = n;
            }
        }
        if (sender == null || sender.equalsIgnoreCase(self)) return;
        String rest = text.substring(sEnd).strip();

        if (e.type == EventState.Type.GIVEAWAY) {
            String low = rest.toLowerCase(Locale.ROOT);
            String kw = e.keyword.toLowerCase(Locale.ROOT);
            if (low.endsWith(kw)) {
                int before = low.length() - kw.length();
                boolean ok = before == 0 || !Character.isLetterOrDigit(low.charAt(before - 1));
                if (ok && e.participants.add(sender)) {
                    local("[Giveaway] " + sender + " ist dabei (" + e.participants.size() + ")");
                    if (announce) mc.player.connection.sendCommand("msg " + sender + " Du bist beim Giveaway dabei!");
                }
            }
        } else {
            Matcher m = BID.matcher(rest);
            if (!m.find()) return;
            long amount = parseAmount(m.group(1), m.group(2));
            if (amount < e.minBid || amount <= e.highest) {
                if (announce && amount >= 0) {
                    String why = amount < e.minBid ? "unter dem Mindestgebot " + fmt(e.minBid)
                            : "zu niedrig, aktuelles Höchstgebot: " + fmt(e.highest);
                    mc.player.connection.sendCommand("msg " + sender + " Gebot " + why);
                }
                return;
            }
            e.highest = amount;
            e.highestBidder = sender;
            e.bidLog.add(sender + ": " + fmt(amount));
            local("[Auktion] Neues Höchstgebot: " + fmt(amount) + " von " + sender);
            say("[Auktion] Neues Höchstgebot: " + fmt(amount) + " von " + sender);
        }
    }

    // ---------- Helfer ----------

    public static long parseAmount(String num, String suffix) {
        try {
            String n = num.replace(",", "");
            double v = Double.parseDouble(n);
            switch (suffix.toLowerCase(Locale.ROOT)) {
                case "k" -> v *= 1_000;
                case "m" -> v *= 1_000_000;
                case "b" -> v *= 1_000_000_000;
                default -> { }
            }
            return Math.round(v);
        } catch (Exception ex) {
            return -1;
        }
    }

    /** Parst "500", "10k", "1.5m" fuer das Mindestgebot. */
    public static long parseMin(String s) {
        Matcher m = Pattern.compile("(?i)^([0-9][0-9.,]*)([kmb]?)$").matcher(s.trim());
        return m.matches() ? parseAmount(m.group(1), m.group(2)) : -1;
    }

    public static String fmt(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    public static String itemName(EventState e) {
        int c = e.item.getCount();
        return (c > 1 ? c + "x " : "") + e.item.getHoverName().getString();
    }

    private static void say(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (announce && mc.player != null) mc.player.connection.sendChat(msg);
    }

    private static void local(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(msg), false);
    }
}
