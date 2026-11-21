package de.texturemixer;

import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Ein laufendes (oder gerade beendetes) Giveaway bzw. eine Auktion. */
public final class EventState {
    public enum Type { GIVEAWAY, AUCTION }

    public final Type type;
    public final ItemStack item;
    public final String keyword;       // Giveaway: Beitritts-Wort
    public final long endAt;           // Millis
    public final long minBid;          // Auktion
    public final LinkedHashSet<String> participants = new LinkedHashSet<>();
    public final List<String> bidLog = new ArrayList<>();
    public final Set<String> pastWinners = new HashSet<>();
    public long highest = 0;
    public String highestBidder = null;
    public String winner = null;
    public boolean closed = false;
    public long closedAt = 0;

    public EventState(Type type, ItemStack item, String keyword, long endAt, long minBid) {
        this.type = type;
        this.item = item;
        this.keyword = keyword;
        this.endAt = endAt;
        this.minBid = minBid;
    }
}
