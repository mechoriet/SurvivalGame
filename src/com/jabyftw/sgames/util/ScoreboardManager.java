package com.jabyftw.sgames.util;

import com.jabyftw.sgames.SurvivalGames;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Based on http://forums.bukkit.org/threads/util-simplescoreboard-make-pretty-scoreboards-with-ease.263041/
 * http://pastebin.com/nWXpwz7e
 */
public class ScoreboardManager implements Listener {

    private final SurvivalGames pl;
    private final Player player;
    private final int delay;
    private final Replacer replacer;
    private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    private final Objective objective = scoreboard.registerNewObjective("survivalgames", "dummy");

    private final HashMap<String, Integer> line = new HashMap<String, Integer>();
    private final ArrayList<Score> scorelist = new ArrayList<Score>();

    private BukkitTask runnable = null;

    public ScoreboardManager(SurvivalGames pl, Player player, int delay, Replacer replacer) {
        this.pl = pl;
        this.player = player;
        this.delay = delay;
        this.replacer = replacer;
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreboard);
        startRunnable();
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }

    public void setLines(String title, String[] lines) {
        line.clear();
        for(int i = 0; i < lines.length; i++) {
            line.put(!lines[i].equalsIgnoreCase("blank") && lines[i].length() < 48 ? lines[i] : " ", i + 1);
        }
        objective.setDisplayName(replacer.replaceString(title).length() < 16 ? replacer.replaceString(title) : "title too big");
    }

    public void startRunnable() {
        stopRunnable(false);
        runnable = new BukkitRunnable() {

            @Override
            public void run() {
                for(Map.Entry<String, Integer> entry : line.entrySet()) {
                    for(Score oldscore : scorelist) {
                        if(oldscore != null) {
                            oldscore.setScore(0);
                        }
                    }
                    scorelist.clear();
                    Score newscore = objective.getScore(replacer.replaceString(entry.getKey()));
                    newscore.setScore(entry.getValue());
                    scorelist.add(entry.getValue(), newscore);
                }
            }
        }.runTaskTimer(pl, 0, delay);
    }

    private void stopRunnable(boolean removeScoreboard) {
        if(runnable != null) {
            runnable.cancel();
            runnable = null;
        }
        if(removeScoreboard) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
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
        if(quitter.equals(player)) {
            stopRunnable(true);
            HandlerList.unregisterAll(this);
        }
    }

    public interface Replacer {
        public String replaceString(String message);
    }
}
