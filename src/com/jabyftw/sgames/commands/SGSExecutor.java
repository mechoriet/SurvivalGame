package com.jabyftw.sgames.commands;

import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.managers.Lobby;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Rafael
 */
public class SGSExecutor implements CommandExecutor {

    private final SurvivalGames pl;
    private final HashMap<Player, GameSetup> gameSetups = new HashMap<Player, GameSetup>();

    public SGSExecutor(SurvivalGames pl) {
        this.pl = pl;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            if(sender.hasPermission(pl.permissions.setup_permission)) {
                if(args.length > 0) {
                    if(args[0].equalsIgnoreCase("create")) {
                        if(args.length > 1) {
                            gameSetups.put(player, new GameSetup(pl, args[1]));
                            sender.sendMessage(pl.getLang("setup.createdSetup"));
                            return true;
                        } else {
                            return false;
                        }
                    } else if(args[0].equalsIgnoreCase("edit")) {
                        if(args.length > 1) {
                            Lobby lobby = pl.getLobby(args[1]);
                            if(lobby != null) {
                                removeLobby(lobby);
                                gameSetups.put(player, new GameSetup(pl, lobby));
                                sender.sendMessage(pl.getLang("setup.editedSetup"));
                                return true;
                            } else {
                                sender.sendMessage(pl.getLang("matchNotFound"));
                                return true;
                            }
                        } else {
                            return false;
                        }
                    } else if(args[0].equalsIgnoreCase("VotingLocation")) {
                        pl.config.lobbiesY.getConfig().set("config.voting.lobbyLocation", pl.config.locationToString(player.getLocation()));
                        pl.config.lobbiesY.saveConfig();
                        player.sendMessage(pl.getLang("setup.successful.location"));
                        return true;
                    } else if(args[0].equalsIgnoreCase("remove")) {
                        if(args.length > 1) {
                            Lobby lobby = pl.getLobby(args[1]);
                            if(lobby != null) {
                                removeLobby(lobby);
                                if(pl.config.removeLobbyPath(lobby.getPath())) {
                                    sender.sendMessage(pl.getLang("setup.arenaDeleted"));
                                    return true;
                                } else {
                                    sender.sendMessage(pl.getLang("setup.failedToDelete"));
                                    return true;
                                }
                            } else {
                                sender.sendMessage(pl.getLang("matchNotFound"));
                                return true;
                            }
                        } else {
                            return false;
                        }
                    } else if(gameSetups.containsKey(player)) {
                        try {
                            if(args[0].equalsIgnoreCase("MaxPlayers") && args.length > 1) {
                                gameSetups.get(player).setMaxPlayer(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("MinPlayers") && args.length > 1) {
                                gameSetups.get(player).setMinPlayer(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("GraceTime") && args.length > 1) {
                                gameSetups.get(player).setGraceTime(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("WaitTime") && args.length > 1) {
                                gameSetups.get(player).setWaitTime(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("ChestRefresh") && args.length > 1) {
                                gameSetups.get(player).setChestRefresh(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("LightningThreshold") && args.length > 1) {
                                gameSetups.get(player).setLightningThreshold(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("MaxDuration") && args.length > 1) {
                                gameSetups.get(player).setMaxDuration(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("PossibleJoinsWithEnough") && args.length > 1) {
                                gameSetups.get(player).setPossibleJoinsWithEnough(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("PossibleJoinsWithoutEnough") && args.length > 1) {
                                gameSetups.get(player).setPossibleJoinsWithoutEnough(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("PossibleJoinsTryDelay") && args.length > 1) {
                                gameSetups.get(player).setPossibleJoinsTryDelay(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("DisplayName") && args.length > 1) {
                                gameSetups.get(player).setDisplayName(args[1]);
                                player.sendMessage(pl.getLang("setup.successful.string"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("VotingDescription") && args.length > 1) {
                                gameSetups.get(player).setVotingDescription(multiArrayAfter(args, 1));
                                player.sendMessage(pl.getLang("setup.successful.multiArgsString"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("AddAllowedCommands") && args.length > 1) {
                                gameSetups.get(player).addAllowedCommands(multiArrayAfter(args, 1));
                                player.sendMessage(pl.getLang("setup.successful.multiArgsString"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("AddDescription") && args.length > 1) {
                                gameSetups.get(player).addDescription(multiArrayAfter(args, 1));
                                player.sendMessage(pl.getLang("setup.successful.multiArgsString"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("AddBlockedCrafting")) {
                                gameSetups.get(player).addBlockedCrafting(player.getItemInHand().getType());
                                player.sendMessage(pl.getLang("setup.successful.material"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("AddAllowedToPlace")) {
                                gameSetups.get(player).addAllowedPlace(player.getItemInHand().getType());
                                player.sendMessage(pl.getLang("setup.successful.material"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("AddAllowedToBreak")) {
                                gameSetups.get(player).addAllowedBreak(player.getItemInHand().getType());
                                player.sendMessage(pl.getLang("setup.successful.material"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("FirstCorner")) {
                                gameSetups.get(player).setCorner1(player.getLocation());
                                player.sendMessage(pl.getLang("setup.successful.location"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("SecondCorner")) {
                                gameSetups.get(player).setCorner2(player.getLocation());
                                player.sendMessage(pl.getLang("setup.successful.location"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("SpectatorSpawn")) {
                                gameSetups.get(player).setSpectator(player.getLocation());
                                player.sendMessage(pl.getLang("setup.successful.location"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("SafeLocation")) {
                                gameSetups.get(player).setSafeLocation(player.getLocation());
                                player.sendMessage(pl.getLang("setup.successful.location"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("ExitLocation")) {
                                gameSetups.get(player).setExitLocation(player.getLocation());
                                player.sendMessage(pl.getLang("setup.successful.location"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("AddChestLocation")) {
                                if(args.length > 1) {
                                    if(!gameSetups.get(player).addChestLocation(player.getLocation(), args[1])) {
                                        player.sendMessage(pl.getLang("setup.error.invalidChestLocationOrTierId"));
                                    } else {
                                        player.sendMessage(pl.getLang("setup.successful.location"));
                                    }
                                    return true;
                                } else {
                                    if(!gameSetups.get(player).addChestLocation(player.getLocation())) {
                                        player.sendMessage(pl.getLang("setup.error.invalidChestLocation"));
                                    } else {
                                        player.sendMessage(pl.getLang("setup.successful.location"));
                                    }
                                    return true;
                                }
                            } else if(args[0].equalsIgnoreCase("AddSpawnLocation")) {
                                gameSetups.get(player).addSpawnLocation(player.getLocation());
                                player.sendMessage(pl.getLang("setup.successful.location"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("KickVips") && args.length > 1) {
                                gameSetups.get(player).setKickVips(args[1].toLowerCase().startsWith("t"));
                                player.sendMessage(pl.getLang("setup.successful.boolean"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("SearchForChests") && args.length > 1) {
                                gameSetups.get(player).setSearchForChests(args[1].toLowerCase().startsWith("t"));
                                player.sendMessage(pl.getLang("setup.successful.boolean"));
                                return true;
                            /* DEATHMATCH */
                            } else if(args[0].equalsIgnoreCase("AddDeathmatchSpawn")) {
                                gameSetups.get(player).addDeathmatchSpawnLocation(player.getLocation());
                                player.sendMessage(pl.getLang("setup.successful.location"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("DeathmatchCenter")) {
                                gameSetups.get(player).setDeathmatchCenter(player.getLocation());
                                player.sendMessage(pl.getLang("setup.successful.location"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("DeathmatchDuration") && args.length > 1) {
                                gameSetups.get(player).setDeathmatchDuration(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("DeathmatchDistance") && args.length > 1) {
                                gameSetups.get(player).setDeathmatchDistance(Integer.parseInt(args[1]));
                                player.sendMessage(pl.getLang("setup.successful.integer"));
                                return true;
                            /* CLEAR */
                            } else if(args[0].equalsIgnoreCase("ClearAllowedBreak")) {
                                gameSetups.get(player).clearAllowedBreak();
                                player.sendMessage(pl.getLang("setup.successful.materialCleared"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("ClearAllowedPlace")) {
                                gameSetups.get(player).clearAllowedPlace();
                                player.sendMessage(pl.getLang("setup.successful.materialCleared"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("ClearBlockedCrafting")) {
                                gameSetups.get(player).clearBlockedCrafting();
                                player.sendMessage(pl.getLang("setup.successful.materialCleared"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("ClearAllowedCommands")) {
                                gameSetups.get(player).clearAllowedCommands();
                                player.sendMessage(pl.getLang("setup.successful.stringCleared"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("ClearDescription")) {
                                gameSetups.get(player).clearDescription();
                                player.sendMessage(pl.getLang("setup.successful.stringCleared"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("ClearSpawns")) {
                                gameSetups.get(player).clearSpawnLocations();
                                player.sendMessage(pl.getLang("setup.successful.locationCleared"));
                                return true;
                            } else if(args[0].equalsIgnoreCase("ClearDeathmatchSpawns")) {
                                gameSetups.get(player).clearDeathmatchSpawnLocations();
                                player.sendMessage(pl.getLang("setup.successful.locationCleared"));
                                return true;
                            } else {
                                ArrayList<String> missed = gameSetups.get(player).getMissedConfig();
                                if(missed.size() > 0) {
                                    player.sendMessage(pl.getLang("setup.failedToConfigure").replaceAll("%missed", missed.toString()));
                                    return true;
                                } else {
                                    if(pl.config.saveConfig(gameSetups.get(player))) {
                                        if(!gameSetups.get(player).isEdited()) {
                                            player.sendMessage(pl.getLang("setup.arenaCreated"));
                                        }
                                        if(pl.config.loadLobby(gameSetups.get(player).getPath(), true)) {
                                            gameSetups.remove(player);
                                            player.sendMessage(pl.getLang("setup.arenaLoaded"));
                                            return true;
                                        } else {
                                            player.sendMessage(pl.getLang("setup.failedToLoad"));
                                            return true;
                                        }
                                    } else {
                                        player.sendMessage(pl.getLang("setup.failedToSave"));
                                        return true;
                                    }
                                }
                            }
                        } catch(NumberFormatException ignored) {
                            player.sendMessage(pl.getLang("setup.error.invalidInteger"));
                            return true;
                        } catch(NullPointerException ignored) {
                            player.sendMessage(pl.getLang("setup.error.invalidItemInHand"));
                            return true;
                        }
                    } else {
                        sender.sendMessage(pl.getLang("setup.setupIsntCreated"));
                        return true;
                    }
                } else {
                    return false;
                }
            } else {
                sender.sendMessage(pl.getLang("noPermission"));
                return true;
            }
        } else {
            sender.sendMessage(pl.getLang("command.cantBeUsedOnConsole"));
            return true;
        }
    }

    private void removeLobby(Lobby lobby) {
        lobby.endMatch(true, false);
        lobby.setCurrentState(SurvivalGames.State.DISABLED);
        pl.lobbies.remove(lobby.getPath());
    }

    // @Source: deathmarine - https://github.com/deathmarine/Ultrabans/blob/master/src/com/modcrafting/ultrabans/util/Formatting.java#L47
    private String multiArrayAfter(String[] args, int after) {
        StringBuilder builder = new StringBuilder();
        if(args.length >= 1) {
            for(int i = after; i < args.length; i++) {
                builder.append(args[i]);
                builder.append(" ");
            }
            if(builder.length() > 1) {
                builder.deleteCharAt(builder.length() - 1);
                return builder.toString();
            }
        }
        return "";
    }
}
