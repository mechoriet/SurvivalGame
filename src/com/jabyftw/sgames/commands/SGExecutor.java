package com.jabyftw.sgames.commands;

import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.SurvivalGames.State;
import com.jabyftw.sgames.managers.Lobby;
import com.jabyftw.sgames.util.RankingEntry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * @author Rafael
 */
public class SGExecutor implements CommandExecutor {

    private final SurvivalGames pl;

    public SGExecutor(SurvivalGames pl) {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length > 0) {
            if(args[0].equalsIgnoreCase("list")) {
                sender.sendMessage(pl.getLang("command.listTitle"));
                if(!pl.config.useVoting) {
                    for(Map.Entry<String, Lobby> set : pl.lobbies.entrySet()) {
                        sender.sendMessage(pl.getLang("command.listEntry")
                                .replaceAll("%path", set.getKey())
                                .replaceAll("%displayname", set.getValue().getName())
                                .replaceAll("%state", pl.getStateName(set.getValue().getCurrentState()))
                                .replaceAll("%alive", set.getValue().getAliveNonMutattorSize())
                                .replaceAll("%time", set.getValue().getDurationRemaining())
                                .replaceAll("%min", Integer.toString(set.getValue().getMinPlayers()))
                                .replaceAll("%max", Integer.toString(set.getValue().getMaxPlayers())));
                    }
                    return true;
                } else {
                    for(Map.Entry<String, Lobby> set : pl.lobbies.entrySet()) {
                        sender.sendMessage(pl.getLang("command.votingListEntry")
                                .replaceAll("%path", set.getKey())
                                .replaceAll("%displayname", set.getValue().getName())
                                .replaceAll("%description", set.getValue().getVotingDescription()));
                    }
                    return true;
                }
            } else if(args[0].equalsIgnoreCase("join")) {
                if(sender instanceof Player) {
                    if(sender.hasPermission(pl.permissions.player_join)) {
                        try {
                            if(pl.config.useVoting) {
                                pl.config.kitPages[0].openInventory((Player) sender);
                                return true;
                            } else {
                                pl.config.joinPages[0].openInventory((Player) sender);
                                return true;
                            }
                        } catch(ArrayIndexOutOfBoundsException ignored) {
                            sender.sendMessage(pl.getLang("command.GUINotGenerated"));
                            return true;
                        }
                    } else {
                        sender.sendMessage(pl.getLang("noPermission"));
                        return true;
                    }
                } else {
                    sender.sendMessage(pl.getLang("command.cantBeUsedOnConsole"));
                    return true;
                }
            } else if(args[0].equalsIgnoreCase("leave")) {
                if(sender instanceof Player) {
                    Player player = (Player) sender;
                    if(!pl.config.useVoting) {
                        if(pl.players.containsKey(player)) {
                            player.teleport(pl.players.get(player).getArena().getExitLocation());
                            pl.players.get(player).removePlayer(player, true);
                            sender.sendMessage(pl.getLang("command.youLeftTheLobby"));
                            return true;
                        } else {
                            sender.sendMessage(pl.getLang("command.notPlaying"));
                            return true;
                        }
                    } else {
                        pl.voting.removePlayer(player);
                        return true;
                    }
                } else {
                    sender.sendMessage(pl.getLang("command.cantBeUsedOnConsole"));
                    return true;
                }
            } else if(args[0].equalsIgnoreCase("sponsor")) {
                if(pl.config.useSponsor) {
                    if(sender.hasPermission(pl.permissions.player_sponsor)) {
                        if(sender instanceof Player) {
                            Player player = (Player) sender;
                            if(pl.players.containsKey(player) && pl.players.get(player).getCurrentState() == State.PLAYING) {
                                try {
                                    pl.players.get(player).getPlayerList().openInventory(player);
                                    return true;
                                } catch(ArrayIndexOutOfBoundsException ignored) {
                                    sender.sendMessage(pl.getLang("command.GUINotGenerated"));
                                    return true;
                                }
                            } else {
                                sender.sendMessage(pl.getLang("command.notPlaying"));
                                return true;
                            }
                        } else {
                            sender.sendMessage(pl.getLang("command.cantBeUsedOnConsole"));
                            return true;
                        }
                    } else {
                        sender.sendMessage(pl.getLang("noPermission"));
                        return true;
                    }
                } else {
                    sender.sendMessage(pl.getLang("command.sponsorIsDisabled"));
                    return true;
                }
            } else if(args[0].equalsIgnoreCase("spectate")) {
                if(sender.hasPermission(pl.permissions.player_spectate)) {
                    if(sender instanceof Player) {
                        Player player = (Player) sender;
                        if(!pl.config.useVoting) {
                            if(args.length > 1) {
                                Lobby lobby = pl.getLobby(args[1]);
                                if(lobby != null) {
                                    lobby.addSpectator(player);
                                    return true;
                                } else {
                                    sender.sendMessage(pl.getLang("matchNotFound"));
                                    return true;
                                }
                            } else {
                                sender.sendMessage(pl.getLang("command.spectateHelp"));
                                return true;
                            }
                        } else {
                            if(pl.voting.getAtual() != null) {
                                if(pl.voting.getAtual().getCurrentState().equals(State.PLAYING) || pl.voting.getAtual().getCurrentState().equals(State.DEATHMATCH) || pl.voting.getAtual().getCurrentState().equals(State.PREGAME)) {
                                    pl.voting.getAtual().addSpectator(player);
                                } else {
                                    pl.voting.addPlayer(player, true);
                                }
                            }
                            return true;
                        }
                    } else {
                        sender.sendMessage(pl.getLang("command.cantBeUsedOnConsole"));
                        return true;
                    }
                } else {
                    sender.sendMessage(pl.getLang("noPermission"));
                    return true;
                }
            } else if(args[0].equalsIgnoreCase("ranking")) {
                if(sender.hasPermission(pl.permissions.player_ranking)) {
                    if(pl.config.useMySQL) {
                        sender.sendMessage(pl.getLang("command.rankingList"));
                        for(RankingEntry re : pl.config.sql.getRankingEntries()) {
                            sender.sendMessage(pl.getLang("command.rankingEntry").replaceAll("%name", re.getPlayerName()).replaceAll("%kdr", re.getKillDeathRatio()).replaceAll("%wlr", re.getWinLosRatio()).replaceAll("%points", re.getPoints()));
                        }
                        return true;
                    } else {
                        sender.sendMessage(pl.getLang("command.rankingDisabled"));
                        return true;
                    }
                } else {
                    sender.sendMessage(pl.getLang("noPermission"));
                    return true;
                }
            } else if(args[0].equalsIgnoreCase("vote")) {
                if(sender.hasPermission(pl.permissions.player_vote)) {
                    if(sender instanceof Player) {
                        if(pl.config.useVoting) {
                            if(args.length > 1) {
                                if(pl.voting.addVote((Player) sender, args[1])) {
                                    sender.sendMessage(pl.getLang("command.voted"));
                                    return true;
                                } else {
                                    sender.sendMessage(pl.getLang("command.cantVoteAgain"));
                                    return true;
                                }
                            } else {
                                sender.sendMessage(pl.getLang("command.votingHelp"));
                                return true;
                            }
                        } else {
                            sender.sendMessage(pl.getLang("command.votingDisabled"));
                            return true;
                        }
                    } else {
                        sender.sendMessage(pl.getLang("command.cantBeUsedOnConsole"));
                        return true;
                    }
                } else {
                    sender.sendMessage(pl.getLang("noPermission"));
                    return true;
                }
            } else if(args[0].equalsIgnoreCase("stats")) {
                if(sender.hasPermission(pl.permissions.player_stats)) {
                    if(pl.config.useMySQL) {
                        if(sender instanceof Player) {
                            RankingEntry re = new RankingEntry(pl, sender.getName(), pl.config.sql.getStats(sender.getName()));
                            sender.sendMessage(pl.getLang("command.rankingEntry").replaceAll("%name", re.getPlayerName()).replaceAll("%kdr", re.getKillDeathRatio()).replaceAll("%wlr", re.getWinLosRatio()).replaceAll("%points", re.getPoints()));
                            return true;
                        } else {
                            sender.sendMessage(pl.getLang("command.cantBeUsedOnConsole"));
                            return true;
                        }
                    } else {
                        sender.sendMessage(pl.getLang("command.rankingDisabled"));
                        return true;
                    }
                } else {
                    sender.sendMessage(pl.getLang("noPermission"));
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
