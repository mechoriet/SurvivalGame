package com.jabyftw.sgames.util;

import com.jabyftw.sgames.Jogador;
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

    private final ArrayList<ScoreboardValue> scorelist = new ArrayList<ScoreboardValue>();

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
        for(ScoreboardValue scoreboardValue : scorelist) {
            scoreboardValue.resetScore();
        }
        scorelist.clear();
        for(int i = 0; i < lines.length; i++) {
            scorelist.add(new ScoreboardValue(fixDuplicates(!lines[i].equalsIgnoreCase("blank") ? lines[i] : " "), i + 1));
        }
        this.title = titleStr;
        objective.setDisplayName(replacer.replaceString(this.title).length() < 16 ? replacer.replaceString(this.title) : "title too big");
    }

    private String fixDuplicates(String text) {
        for(ScoreboardValue scoreboardValue : scorelist) {
            if(text.equalsIgnoreCase(scoreboardValue.getStringOriginal())) {
                text += " ";
            }
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
                if((lastSb == null || !lastSb.equals(jogador.getPlayer().getScoreboard())) && resetScoreboard) {
                    jogador.getPlayer().setScoreboard(scoreboard);
                }
                lastSb = jogador.getPlayer().getScoreboard();
                for(ScoreboardValue scoreboardValue : scorelist) {
                    scoreboardValue.update();
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
        for(ScoreboardValue scoreboardValue : scorelist) {
            scoreboardValue.resetScore();
        }
        scorelist.clear();
        objective.unregister();
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

    @SuppressWarnings("deprecation")
    private class ScoreboardValue {

        private final String stringOriginal;
        private final int line;

        private String stringAtual;
        private Score scoreAtual = null;

        public ScoreboardValue(String string, int line) {
            this.stringOriginal = string;
            this.line = line;
            stringAtual = replacer.replaceString(stringOriginal);
        }

        public String getStringOriginal() {
            return stringOriginal;
        }

        public void update() {
            String stringNova = replacer.replaceString(stringOriginal);
            if(!stringAtual.equalsIgnoreCase(stringNova)) {
                resetScore();
                scoreAtual = objective.getScore(Bukkit.getOfflinePlayer(stringNova));
                scoreAtual.setScore(line);
            }
        }

        public void resetScore() {
            if(scoreAtual != null) {
                scoreboard.resetScores(scoreAtual.getPlayer());
                scoreAtual = null;
            }
        }
    }
}
