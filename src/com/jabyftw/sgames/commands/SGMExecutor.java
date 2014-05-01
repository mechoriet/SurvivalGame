package com.jabyftw.sgames.commands;

import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.SurvivalGames.State;
import com.jabyftw.sgames.managers.Lobby;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Rafael
 */
public class SGMExecutor implements CommandExecutor {

    private final SurvivalGames pl;

    public SGMExecutor(SurvivalGames pl) {
        this.pl = pl;
    }

    // /sgm (stop/start/removeplayer)
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length > 0) {
            if(args[0].equalsIgnoreCase("stop")) {
                if(sender.hasPermission(pl.permissions.management_stop)) {
                    if(args.length > 1 && !pl.config.useVoting) {
                        Lobby lobby = pl.getLobby(args[1]);
                        if(lobby != null) {
                            if(lobby.getCurrentState() == State.PLAYING || lobby.getCurrentState() == State.DEATHMATCH) {
                                lobby.endMatch(false, false);
                                sender.sendMessage(pl.getLang("command.gameEnded"));
                                return true;
                            } else {
                                sender.sendMessage(pl.getLang("matchNotReady"));
                                return true;
                            }
                        } else {
                            sender.sendMessage(pl.getLang("matchNotFound"));
                            return true;
                        }
                    } else {
                        if(sender instanceof Player) {
                            Player player = (Player) sender;
                            if(pl.players.containsKey(player)) {
                                if(pl.players.get(player).getCurrentState() == State.PLAYING || pl.players.get(player).getCurrentState() == State.DEATHMATCH) {
                                    pl.players.get(player).endMatch(false, false);
                                    sender.sendMessage(pl.getLang("command.gameEnded"));
                                    return true;
                                } else {
                                    sender.sendMessage(pl.getLang("matchNotReady"));
                                    return true;
                                }
                            } else {
                                sender.sendMessage(pl.getLang("command.sgmHelp"));
                                return true;
                            }
                        } else {
                            sender.sendMessage(pl.getLang("command.cantBeUsedOnConsole"));
                            return true;
                        }
                    }
                } else {
                    sender.sendMessage(pl.getLang("noPermission"));
                    return true;
                }
            } else if(args[0].equalsIgnoreCase("start")) {
                if(sender.hasPermission(pl.permissions.management_start)) {
                    if(args.length > 1 && !pl.config.useVoting) {
                        Lobby lobby = pl.getLobby(args[1]);
                        if(lobby != null) {
                            if(lobby.getCurrentState() == State.WAITING) {
                                lobby.startMatch();
                                sender.sendMessage(pl.getLang("command.gameStarted"));
                                return true;
                            } else {
                                sender.sendMessage(pl.getLang("matchNotReady"));
                                return true;
                            }
                        } else {
                            sender.sendMessage(pl.getLang("matchNotFound"));
                            return true;
                        }
                    } else {
                        if(sender instanceof Player) {
                            Player player = (Player) sender;
                            if(pl.players.containsKey(player)) {
                                if(pl.players.get(player).getCurrentState() == State.WAITING) {
                                    pl.players.get(player).startMatch();
                                    sender.sendMessage(pl.getLang("command.gameStarted"));
                                    return true;
                                } else {
                                    sender.sendMessage(pl.getLang("matchNotReady"));
                                    return true;
                                }
                            } else {
                                sender.sendMessage(pl.getLang("command.sgmHelp"));
                                return true;
                            }
                        } else {
                            sender.sendMessage(pl.getLang("command.cantBeUsedOnConsole"));
                            return true;
                        }
                    }
                } else {
                    sender.sendMessage(pl.getLang("noPermission"));
                    return true;
                }
            } else if(args[0].equalsIgnoreCase("kick")) {
                if(sender.hasPermission(pl.permissions.management_kick)) {
                    if(args.length > 1) {
                        Player player = pl.getServer().getPlayerExact(args[1]);
                        if(player != null) {
                            if(pl.players.containsKey(player)) {
                                pl.players.get(player).removePlayer(player, true);
                                sender.sendMessage(pl.getLang("command.playerKicked"));
                                return true;
                            } else {
                                sender.sendMessage(pl.getLang("command.playerIsntPlaying"));
                                return true;
                            }
                        } else {
                            sender.sendMessage(pl.getLang("playerNotFound"));
                            return true;
                        }
                    } else {
                        sender.sendMessage(pl.getLang("command.kickHelp"));
                        return true;
                    }
                } else {
                    sender.sendMessage(pl.getLang("noPermission"));
                    return true;
                }
            } else if(args[0].equalsIgnoreCase("reload")) {
                if(sender.hasPermission(pl.permissions.management_kick)) {
                    long start = System.currentTimeMillis();
                    pl.onReload();
                    sender.sendMessage(pl.getLang("reloadedInXms").replaceAll("%time", Long.toString(System.currentTimeMillis() - start)));
                    return true;
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
