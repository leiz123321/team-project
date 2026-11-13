package entity;

import java.util.List;
import java.util.Objects;

/**
 * The strength of a five-card poker hand: its category, and the ranks that
 * decide between two hands of the same category, most important first.
 */
public final class HandValue implements Comparable<HandValue> {
    private final HandCategory category;
    private final List<Rank> tiebreakers;

    /**
     * Creates a hand value.
     *
     * @param category the category of the hand
     * @param tiebreakers the ranks that break ties, most important first
     * @throws IllegalArgumentException if an argument is null
     */
    public HandValue(HandCategory category, List<Rank> tiebreakers) {
        if (category == null || tiebreakers == null) {
            throw new IllegalArgumentException("category and tiebreakers must not be null");
        }
        this.category = category;
        this.tiebreakers = List.copyOf(tiebreakers);
    }

    public HandCategory getCategory() {
        return this.category;
    }

    /**
     * Returns the tie-breaking ranks, most important first, as a list that
     * cannot be changed.
     */
    public List<Rank> getTiebreakers() {
        return this.tiebreakers;
    }

    /**
     * Compares by category first, then by the tie-breaking ranks in order.
     * A result of 0 means the two hands split the pot.
     */
    @Override
    public int compareTo(HandValue other) {
        int byCategory = this.category.compareTo(other.category);
        if (byCategory != 0) {
            return byCategory;
        }
        int shared = Math.min(this.tiebreakers.size(), other.tiebreakers.size());
        for (int i = 0; i < shared; i++) {
            int byRank = this.tiebreakers.get(i).compareTo(other.tiebreakers.get(i));
            if (byRank != 0) {
                return byRank;
            }
        }
        return Integer.compare(this.tiebreakers.size(), other.tiebreakers.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HandValue)) {
            return false;
        }
        HandValue other = (HandValue) o;
        return this.category == other.category && this.tiebreakers.equals(other.tiebreakers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.category, this.tiebreakers);
    }

    @Override
    public String toString() {
        return this.category + " " + this.tiebreakers;
    }
}
