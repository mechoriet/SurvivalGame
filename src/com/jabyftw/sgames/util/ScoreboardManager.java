package com.jabyftw.sgames.util;

import com.jabyftw.sgames.Jogador;
import com.jabyftw.sgames.SurvivalGames;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Based on http://forums.bukkit.org/threads/util-simplescoreboard-make-pretty-scoreboards-with-ease.263041/
 * http://pastebin.com/nWXpwz7e
 */
public class ScoreboardManager implements Listener {

    private final SurvivalGames pl;
    private final Jogador jogador;
    private final int delay;
    private final Replacer replacer;
    private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    private final Objective objective = scoreboard.registerNewObjective("survivalgames", "dummy");

    private final HashMap<String, Integer> line = new HashMap<String, Integer>();
    private final ArrayList<Score> scorelist = new ArrayList<Score>();

    private BukkitTask runnable = null;
    private String title;
    private boolean resetScoreboard;

    public ScoreboardManager(SurvivalGames pl, Jogador jogador, int delay, Replacer replacer) {
        this.pl = pl;
        this.jogador = jogador;
        this.delay = delay;
        this.replacer = replacer;
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        startRunnable();
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }

    public void setLines(String titleStr, String[] lines) {
        line.clear();
        for(int i = 0; i < lines.length; i++) {
            line.put(fixDuplicates(!lines[i].equalsIgnoreCase("blank") ? lines[i] : " "), i + 1);
        }
        this.title = titleStr;
        objective.setDisplayName(replacer.replaceString(this.title).length() < 16 ? replacer.replaceString(this.title) : "title too big");
    }

    private String fixDuplicates(String text) {
        while(line.containsKey(text)) {
            text += "§r";
        }
        if(text.length() > 16) {
            text = text.substring(0, 15);
        }
        return text;
    }

    public void startRunnable() {
        stopRunnable(false);
        resetScoreboard = true;
        runnable = new BukkitRunnable() {

            Scoreboard lastSb = null;

            @Override
            public void run() {
                if(!lastSb.equals(jogador.getPlayer().getScoreboard()) && resetScoreboard) {
                    jogador.getPlayer().setScoreboard(scoreboard);
                }
                lastSb = jogador.getPlayer().getScoreboard();
                for(Map.Entry<String, Integer> entry : line.entrySet()) {
                    for(Score oldscore : scorelist) {
                        if(oldscore != null) {
                            scoreboard.resetScores(oldscore.getPlayer());
                        }
                    }
                    scorelist.clear();
                    OfflinePlayer player = Bukkit.getOfflinePlayer(replacer.replaceString(entry.getKey()));
                    Score newscore = objective.getScore(player);
                    newscore.setScore(entry.getValue());
                    /*Score newscore = objective.getScore(replacer.replaceString(entry.getKey()));
                    newscore.setScore(entry.getValue());*/
                    scorelist.add(newscore);
                }
                objective.setDisplayName(replacer.replaceString(title).length() < 16 ? replacer.replaceString(title) : "title too big");
            }
        }.runTaskTimer(pl, 1, delay);
    }

    public void stopRunnable(boolean removeScoreboard) {
        resetScoreboard = false;
        if(runnable != null) {
            runnable.cancel();
            runnable = null;
        }
        if(removeScoreboard) {
            jogador.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        checkExit(e.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        checkExit(e.getPlayer());
    }

    private void checkExit(Player quitter) {
        if(quitter.equals(jogador.getPlayer())) {
            stopRunnable(true);
            HandlerList.unregisterAll(this);
        }
    }

    public interface Replacer {
        public String replaceString(String message);
    }
}
