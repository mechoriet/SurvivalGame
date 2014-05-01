package com.jabyftw.sgames.util;

import com.jabyftw.sgames.SurvivalGames;
import org.bukkit.Bukkit;

import java.text.DecimalFormat;

/**
 * @author Rafael
 */
public class RankingEntry {

    private final SurvivalGames pl;
    private final String name;
    private int kills, deaths, wins;

    public RankingEntry(SurvivalGames pl, String name, int[] values) {
        this.pl = pl;
        this.name = name;
        this.kills = values[0];
        this.deaths = values[1];
        this.wins = values[2];
    }

    public String getPlayerName() {
        return name;
    }

    public void addKillToCounter() {
        kills++;
    }

    public void addDeathToCounter() {
        deaths++;
    }

    public void addWinToCounter() {
        wins++;
    }

    public String getPoints() {
        if(pl.config.usePlayerPoints) {
            return Integer.toString(pl.config.playerp.getAPI().look(Bukkit.getOfflinePlayer(name).getUniqueId()));
        }
        return "0";
    }

    public void saveStats() {
        if(pl.config.useMySQL) {
            pl.config.sql.updateOrInsertPlayer(name, kills, deaths, wins);
        }
    }

    public String getKillDeathRatio() {
        int death = deaths;
        if(death < 1) {
            death = 1;
        }
        return new DecimalFormat("0.00").format(kills * 1D / death * 1D);
    }

    public String getWinLosRatio() {
        int death = deaths; // deaths = loses
        if(death < 1) {
            death = 1;
        }
        return new DecimalFormat("0.00").format(wins * 1D / death * 1D);
    }
}
