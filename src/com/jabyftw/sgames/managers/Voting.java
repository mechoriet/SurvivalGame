package com.jabyftw.sgames.managers;

import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.item.Kit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Rafael
 */
public class Voting {

    private final SurvivalGames pl;
    private final boolean restartAfterEnd;
    private final int defaultTimeout;
    private final Location lobbyLocation;
    private final LinkedHashMap<Lobby, Integer> votes = new LinkedHashMap<Lobby, Integer>();
    private final LinkedHashMap<Player, VotingJogador> players = new LinkedHashMap<Player, VotingJogador>();

    private Lobby atual = null;
    private int timeout;
    private BukkitTask timeoutRunnable;

    public Voting(SurvivalGames pl, int defaulttimeout, boolean restart, Location lobby) {
        this.pl = pl;
        this.defaultTimeout = defaulttimeout;
        this.restartAfterEnd = restart;
        this.lobbyLocation = lobby;
        timeout = defaultTimeout;
        startTimeoutRunnable();
    }

    public VotingJogador getJogador(Player player) {
        if(players.get(player) == null) {
            addPlayer(player, false);
        }
        return players.get(player);
    }

    public void addPlayer(Player player, boolean spectator) {
        if(!players.containsKey(player)) {
            players.put(player, new VotingJogador(player, spectator));
            player.teleport(lobbyLocation);
        }
        if(atual != null) {
            if(atual.getCurrentState().equals(SurvivalGames.State.WAITING)) {
                if(!spectator) {
                    pl.config.kitPages[0].openInventory(player);
                } else {
                    player.sendMessage(pl.getLang("waitUntilMatchStarts"));
                }
            } else if(atual.getCurrentState().equals(SurvivalGames.State.PREGAME) || atual.getCurrentState().equals(SurvivalGames.State.PLAYING) || atual.getCurrentState().equals(SurvivalGames.State.DEATHMATCH)) {
                atual.addSpectator(player);
            }
            return;
        }
        player.sendMessage(pl.getLang("youJoinedTheVotingLobby"));
    }

    public boolean addVote(Player player, String args) {
        Lobby lobby = pl.getLobby(args);
        if(lobby != null && !getJogador(player).hasVoted()) {
            if(votes.containsKey(lobby)) {
                votes.put(lobby, votes.get(lobby) + 1);
            } else {
                votes.put(lobby, 1);
            }
            getJogador(player).setHasVoted(true);
            return true;
        }
        return false;
    }

    private void startTimeoutRunnable() {
        timeoutRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if(votes.size() > 0 && players.size() > 0 && atual == null) {
                    timeout--;
                    if(timeout <= 0) {
                        Lobby mostVoted = getMostVoted();
                        if(mostVoted != null) {
                            startMatch(mostVoted);
                            stopTimeoutRunnable();
                        } else {
                            timeout = defaultTimeout;
                        }
                    }
                } else {
                    timeout = defaultTimeout;
                }
            }
        }.runTaskTimer(pl, 20, 20);
    }

    private void startMatch(Lobby mostVoted) {
        atual = mostVoted;
        for(Map.Entry<Player, VotingJogador> entry : players.entrySet()) {
            if(!entry.getValue().isSpectating) {
                if(entry.getValue().getKit() != null) {
                    mostVoted.addPlayer(entry.getKey(), entry.getValue().getKit());
                } else {
                    pl.config.kitPages[0].openInventory(entry.getKey());
                }
            }
        }
        mostVoted.startMatch();
        for(Map.Entry<Player, VotingJogador> entry : players.entrySet()) {
            if(entry.getValue().isSpectating) {
                atual.addSpectator(entry.getKey());
            }
        }
    }

    public void restartVoting(boolean stopping) {
        atual = null;
        votes.clear();
        for(VotingJogador jogador : players.values()) {
            jogador.setHasVoted(false);
            jogador.setKit(null);
            jogador.getPlayer().teleport(lobbyLocation);
            jogador.getPlayer().sendMessage(pl.getLang("youJoinedTheVotingLobby"));
        }
        if(restartAfterEnd) {
            pl.getServer().shutdown();
            return;
        }
        if(!stopping) {
            removePlayers();
            startTimeoutRunnable();
        }
    }

    public void removePlayers() {
        if(atual != null) {
            atual.endMatch(true, true);
            atual = null;
        }
        players.clear();
    }

    public void removePlayer(Player player) {
        if(atual != null) {
            atual.removePlayer(player, true);
        }
        players.remove(player);
    }

    private Lobby getMostVoted() {
        Lobby mostVoted = null;
        int voteCount = 0;
        for(Map.Entry<Lobby, Integer> set : votes.entrySet()) {
            if(set.getValue() > voteCount) {
                voteCount = set.getValue();
                mostVoted = set.getKey();
            }
        }
        return mostVoted;
    }

    private void stopTimeoutRunnable() {
        timeoutRunnable.cancel();
    }

    public Lobby getAtual() {
        return atual;
    }

    private class VotingJogador {

        private final Player player;

        private Kit kit = null;
        private boolean hasVoted = false, isSpectating = false;

        public VotingJogador(Player player, boolean spectating) {
            this.player = player;
            isSpectating = spectating;
        }

        public Kit getKit() {
            return kit;
        }

        public void setKit(Kit kit) {
            this.kit = kit;
        }

        public boolean hasVoted() {
            return hasVoted;
        }

        public void setHasVoted(boolean hasVoted) {
            this.hasVoted = hasVoted;
        }

        public Player getPlayer() {
            return player;
        }
    }
}
