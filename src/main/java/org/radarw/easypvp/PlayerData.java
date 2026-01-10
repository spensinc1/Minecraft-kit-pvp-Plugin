package org.radarw.easypvp;

import java.util.UUID;

public class PlayerData {
    public UUID uuid;
    public int kills;
    public int deaths;
    public int money;
    public int killStreak;

    public PlayerData(UUID uuid, int kills, int deaths, int money, int killStreak) {
        this.uuid = uuid;
        this.kills = kills;
        this.deaths = deaths;
        this.money = money;
        this.killStreak = killStreak;
    }
}