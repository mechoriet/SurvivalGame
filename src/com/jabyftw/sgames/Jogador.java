package com.jabyftw.sgames;

import com.jabyftw.sgames.SurvivalGames.State;
import com.jabyftw.sgames.item.Kit;
import com.jabyftw.sgames.item.SponsorKit;
import com.jabyftw.sgames.managers.Lobby;
import com.jabyftw.sgames.util.RankingEntry;
import com.jabyftw.sgames.util.ScoreboardManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author Rafael
 */
public class Jogador extends Spectator {

    private final SurvivalGames pl;
    private final RankingEntry rankingEntry;
    private final Kit kit;
    private final ScoreboardManager scoreboardManager;

    private final ArrayList<String> sponsors = new ArrayList<String>();
    private boolean isPlaying = false, alreadyMuttated = false;
    private String killer = null;

    public Jogador(SurvivalGames pl, Player p, Lobby lobby, RankingEntry ranking, Kit kit) {
        super(p, lobby);
        this.pl = pl;
        this.rankingEntry = ranking;
        this.kit = kit;
        scoreboardManager = new ScoreboardManager(pl, this, pl.config.scoreboardDelay, pl.config.getReplacer(lobby, this));
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public Kit getKit() {
        return kit;
    }

    @Override
    public boolean canComeBack() {
        return (killer != null && lobby.getCurrentState() == State.PLAYING);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
        if(isPlaying) {
            scoreboardManager.startRunnable();
        } else {
            scoreboardManager.stopRunnable(true);
        }
    }

    public String getKiller() {
        return killer;
    }

    public RankingEntry getRanking() {
        return rankingEntry;
    }

    public void setKiller(Player killer) {
        this.killer = killer.getName();
    }

    public void addKillReward() {
        giveReward(pl.config.valuePerKill);
    }

    public void addWinReward() {
        giveReward(pl.config.valuePerWin);
    }

    public void addSponsor(String name) {
        this.sponsors.add(name);
    }

    private void giveReward(int value) {
        if(pl.config.usePlayerPoints) {
            pl.config.playerp.getAPI().give(player.getUniqueId(), value);
            for(Map.Entry<Player, SponsorKit> set : lobby.getSponsors()) {
                if(sponsors.contains(set.getKey().getName())) {
                    pl.config.playerp.getAPI().give(set.getKey().getUniqueId(), (int) (set.getValue().getPercentage() * value));
                }
            }
        } else if(pl.config.useVault) {
            pl.config.econ.depositPlayer(player.getName(), value);
            for(Map.Entry<Player, SponsorKit> set : lobby.getSponsors()) {
                if(sponsors.contains(set.getKey().getName())) {
                    pl.config.econ.depositPlayer(set.getKey().getName(), (set.getValue().getPercentage() * value));
                }
            }
        }
    }

    public void setAlreadyMuttated(boolean alreadyMuttated) {
        this.alreadyMuttated = alreadyMuttated;
    }

    public boolean isAlreadyMuttated() {
        return alreadyMuttated && !pl.haveMuttated.contains(player.getName().toLowerCase());
    }

    public void addFailedMuttationBonus() {
        giveReward(pl.config.muttationBonus);
    }

    public void addMuttationReward() {
        giveReward(pl.config.muttationReward);
    }
}
