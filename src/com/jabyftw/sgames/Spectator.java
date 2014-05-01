package com.jabyftw.sgames;

import com.jabyftw.sgames.managers.Lobby;
import org.bukkit.entity.Player;

import java.util.ArrayList;

/**
 * @author Rafael
 */
public class Spectator {

    private final Player player;
    private final Lobby lobby;

    private int value = 0;

    public Spectator(Player p, Lobby lobby) {
        this.player = p;
        this.lobby = lobby;
    }

    public Player getPlayer() {
        return player;
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean canComeBack() {
        return false;
    }

    public Player getPast(boolean changeValue) {
        ArrayList<Jogador> players = lobby.getAliveJogador();
        int i = value;
        i -= 1;
        if(i < 0) {
            i = players.size() - 1;
        }
        if(changeValue) {
            value = i;
        }
        return players.get(i).getPlayer();
    }

    public Player getNext(boolean changeValue) {
        ArrayList<Jogador> players = lobby.getAliveJogador();
        int i = value;
        i += 1;
        if(i >= players.size()) {
            i = 0;
        }
        if(changeValue) {
            value = i;
        }
        return players.get(i).getPlayer();
    }
}
