package com.rajneeti.entity.enums;

/**
 * Types of gameplay actions a player can claim during their turn.
 */
public enum MatchActionType {

    INCOME(false, 1),
    FOREIGN_AID(false, 2),
    TAX(true, 3),
    STEAL(true, null),
    EXCHANGE(false, null),
    ASSASSINATE(true, null),
    COUP(false, null);

    private final boolean challengeable;
    private final Integer gainCoins;

    MatchActionType(boolean challengeable, Integer gainCoins) {
        this.challengeable = challengeable;
        this.gainCoins = gainCoins;
    }

    public boolean isChallengeable() {
        return challengeable;
    }

    public Integer getGainCoins() {
        return gainCoins;
    }
}