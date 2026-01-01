package org.radarw.easypvp;

import java.util.UUID;

public class PlayerData {
    public UUID uuid;
    public int kills;
    public int deaths;
    public int money;

    public PlayerData(UUID uuid, int kills, int deaths, int money) {
        this.uuid = uuid;
        this.kills = kills;
        this.deaths = deaths;
        this.money = money;
    }
}