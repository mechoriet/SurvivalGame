package com.jabyftw.sgames.util;

import com.google.common.base.Splitter;
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

import java.util.*;

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
    private final ArrayList<Team> teams = new ArrayList<Team>();
    private final ArrayList<Score> scorelist = new ArrayList<Score>();

    private BukkitTask runnable = null;

    public ScoreboardManager(SurvivalGames pl, Jogador jogador, int delay, Replacer replacer) {
        this.pl = pl;
        this.jogador = jogador;
        this.delay = delay;
        this.replacer = replacer;
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        jogador.getPlayer().setScoreboard(scoreboard);
        startRunnable();
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }

    public void setLines(String title, String[] lines) {
        line.clear();
        for(int i = 0; i < lines.length; i++) {
            line.put(fixDuplicates(!lines[i].equalsIgnoreCase("blank") ? lines[i] : " "), i + 1);
        }
        objective.setDisplayName(replacer.replaceString(title).length() < 16 ? replacer.replaceString(title) : "title too big");
    }

    private String fixDuplicates(String text) {
        if(line.containsKey(text)) {
            text += "§r";
        }
        if(text.length() > 48) {
            text = text.substring(0, 47);
        }
        return text;
    }

    private Map.Entry<Team, String> createTeam(String text) {
        String result = "";
        if(text.length() <= 16)
            return new AbstractMap.SimpleEntry<Team, String>(null, text);
        Team team = scoreboard.registerNewTeam("text-" + scoreboard.getTeams().size());
        Iterator<String> iterator = Splitter.fixedLength(16).split(text).iterator();
        team.setPrefix(iterator.next());
        result = iterator.next();
        if(text.length() > 32)
            team.setSuffix(iterator.next());
        teams.add(team);
        return new AbstractMap.SimpleEntry<Team, String>(team, result);
    }

    public void startRunnable() {
        stopRunnable(false);
        runnable = new BukkitRunnable() {

            @Override
            public void run() {
                for(Map.Entry<String, Integer> entry : line.entrySet()) {
                    for(Score oldscore : scorelist) {
                        if(oldscore != null) {
                            scoreboard.resetScores(oldscore.getPlayer());
                        }
                    }
                    scorelist.clear();
                    Map.Entry<Team, String> team = createTeam(replacer.replaceString(entry.getKey()));
                    OfflinePlayer player = Bukkit.getOfflinePlayer(team.getValue());
                    if(team.getKey() != null) {
                        team.getKey().addPlayer(player);
                    }
                    Score newscore = objective.getScore(player);
                    newscore.setScore(entry.getValue());
                    scorelist.add(newscore);
                }
            }
        }.runTaskTimer(pl, 0, delay);
    }

    public void stopRunnable(boolean removeScoreboard) {
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
            for(Team team : teams) {
                team.unregister();
            }
        }
    }

    public interface Replacer {
        public String replaceString(String message);
    }
}
