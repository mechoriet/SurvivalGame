package com.jabyftw.sgames.commands;

import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.managers.Lobby;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;

public class GameSetup {

    private final SurvivalGames pl;
    private final String path;
    private final ArrayList<String> allowedCommands = new ArrayList<String>(), description = new ArrayList<String>(), blockCrafting = new ArrayList<String>(),
            allowPlace = new ArrayList<String>(), allowBreak = new ArrayList<String>(), chestLocation = new ArrayList<String>(), spawnLocation = new ArrayList<String>();
    private final ArrayList<String> deathmatchLocation = new ArrayList<String>(); // DEATHMATCH
    private int minPlayer = 0, maxPlayer = 0, lightningThreshold = 0, chestRefresh = 0, maxDuration = 0, graceTime = 45, waitTime = 30,
            withEnough = 2, withoutEnough = 3, possibleJoinsDelay = 30;
    private String votingDescription = "", displayName = "",
            corner1 = "", corner2 = "", spectator = "", safeLocation = "", exitLocation = ""; // Locations
    private boolean kickVips = false, searchForChests = true;
    private int deathmatchDuration = 0, deathmatchDistance = 0; // DEATHMATCH
    private String deathmatchCenter = ""; // DEATHMATCH
    private final boolean edited;

    public GameSetup(SurvivalGames pl, String path) {
        this.pl = pl;
        this.path = path.toLowerCase().replaceAll(" ", "_");
        edited = false;
    }

    public GameSetup(SurvivalGames pl, Lobby lobby) {
        this.pl = pl;
        this.path = lobby.getPath();
        this.allowedCommands.addAll(lobby.getAllowedCommands());
        this.description.addAll(lobby.getLore());
        minPlayer = lobby.getMinPlayers();
        maxPlayer = lobby.getMaxPlayers();
        lightningThreshold = lobby.getLightningThreshold();
        chestRefresh = lobby.getChestRefresh();
        waitTime = lobby.getWaitTime();
        graceTime = lobby.getGraceTime();
        withEnough = lobby.getPossibleJoinsWithEnoughPlayers();
        withoutEnough = lobby.getPossibleJoinsWithoutEnoughPlayers();
        possibleJoinsDelay = lobby.getPossibleJoinsDelay();
        maxDuration = lobby.getMaxDuration();
        votingDescription = lobby.getVotingDescription();
        displayName = lobby.getName();
        corner1 = pl.config.locationToString(lobby.getArena().getCorner1());
        corner2 = pl.config.locationToString(lobby.getArena().getCorner2());
        spectator = pl.config.locationToString(lobby.getArena().getSpectatorSpawn());
        safeLocation = pl.config.locationToString(lobby.getArena().getSafeLocation());
        kickVips = lobby.getKickVips();
        exitLocation = pl.config.locationToString(lobby.getArena().getExitLocation());
        searchForChests = lobby.getArena().searchChests();
        deathmatchCenter = pl.config.locationToString(lobby.getArena().getDeathmatchCenter());
        deathmatchDistance = lobby.getDeathmatchDistance();
        deathmatchDuration = lobby.getDeathmatchDuration();
        for(Material material : lobby.getArena().getBlockedCrafting()) {
            this.blockCrafting.add(material.name());
        }
        for(Material material : lobby.getArena().getAllowedPlace()) {
            this.allowPlace.add(material.name());
        }
        for(Material material : lobby.getArena().getAllowedBreak()) {
            this.allowBreak.add(material.name());
        }
        for(Map.Entry<Location, String> entry : lobby.getArena().getChestLocation()) {
            if(entry.getValue() != null && entry.getKey() != null && entry.getKey().getWorld() != null) {
                addChestLocation(entry.getKey(), entry.getValue());
            } else {
                pl.getLogger().log(Level.WARNING, "Failed to import a chest location.");
            }
        }
        for(Location location : lobby.getArena().getSpawns()) {
            addSpawnLocation(location);
        }
        for(Location location : lobby.getArena().getDeathmatchSpawns()) {
            addDeathmatchSpawnLocation(location);
        }
        edited = true;
    }

    public String getPath() {
        return path;
    }

    public ArrayList<String> getMissedConfig() {
        ArrayList<String> error = new ArrayList<String>();
        if(spawnLocation.size() < minPlayer) {
            error.add(pl.getNoPrefixLang("setup.error.notEnoughSpawns"));
        }
        if(minPlayer == 0) {
            error.add(pl.getNoPrefixLang("setup.error.minPlayerNotSet"));
        }
        if(maxPlayer == 0) {
            error.add(pl.getNoPrefixLang("setup.error.maxPlayerNotSet"));
        }
        if(maxDuration == 0) {
            error.add(pl.getNoPrefixLang("setup.error.maxDurationNotSet"));
        }
        if(corner1.length() == 0) {
            error.add(pl.getNoPrefixLang("setup.error.corner1NotSet"));
        }
        if(corner2.length() == 0) {
            error.add(pl.getNoPrefixLang("setup.error.corner2NotSet"));
        }
        if(spectator.length() == 0) {
            error.add(pl.getNoPrefixLang("setup.error.spectatorNotSet"));
        }
        if(safeLocation.length() == 0) {
            error.add(pl.getNoPrefixLang("setup.error.safeLocationNotSet"));
        }
        if(exitLocation.length() == 0) {
            error.add(pl.getNoPrefixLang("setup.error.exitLocationNotSet"));
        }
        if(displayName.length() == 0) {
            error.add(pl.getNoPrefixLang("setup.error.displayNameNotSet"));
        }
        if(chestLocation.size() > 0 && chestRefresh == 0) {
            error.add(pl.getNoPrefixLang("setup.error.chestDelayNotSet"));
        }
        if(pl.config.useVoting && votingDescription.length() == 0) {
            error.add(pl.getNoPrefixLang("setup.error.votingDescriptionNotSet"));
        }
        if((deathmatchDuration != 0 || deathmatchDistance != 0 || deathmatchCenter.length() > 0 || deathmatchLocation.size() > 0) && !isDeathmatchEnabled()) {
            if(deathmatchDuration == 0) {
                error.add(pl.getNoPrefixLang("setup.error.deathmatchDurationNotSet"));
            }
            if(deathmatchDistance == 0) {
                error.add(pl.getNoPrefixLang("setup.error.deathmatchDistanceRadiusNotSet"));
            }
            if(deathmatchCenter.length() == 0) {
                error.add(pl.getNoPrefixLang("setup.error.deathmatchCenterNotSet"));
            }
            if(deathmatchLocation.size() == 0) {
                error.add(pl.getNoPrefixLang("setup.error.deathmatchSpawnNotSet"));
            }
        }
        return error;
    }

    public boolean isDeathmatchEnabled() {
        return (deathmatchDuration != 0 && deathmatchDistance != 0 && deathmatchCenter.length() > 0 && deathmatchLocation.size() > 0);
    }

    public ArrayList<String> getAllowedCommands() {
        return allowedCommands;
    }

    public ArrayList<String> getDescription() {
        return description;
    }

    public ArrayList<String> getBlockCrafting() {
        return blockCrafting;
    }

    public ArrayList<String> getAllowPlace() {
        return allowPlace;
    }

    public ArrayList<String> getAllowBreak() {
        return allowBreak;
    }

    public ArrayList<String> getChestLocation() {
        return chestLocation;
    }

    public ArrayList<String> getSpawnLocation() {
        return spawnLocation;
    }

    public ArrayList<String> getDeathmatchLocation() {
        return deathmatchLocation;
    }

    public void addAllowedCommands(String command) {
        allowedCommands.add(command.replaceAll("/", ""));
    }

    public void addDescription(String description) {
        this.description.add(description);
    }

    public void addBlockedCrafting(Material material) {
        blockCrafting.add(material.name());
    }

    public void addAllowedPlace(Material material) {
        allowPlace.add(material.name());
    }

    public void addAllowedBreak(Material material) {
        allowBreak.add(material.name());
    }

    public boolean addChestLocation(Location chestloc) {
        if(chestloc.getBlock().getState() instanceof Chest || chestloc.getBlock().getState() instanceof DoubleChest) {
            chestLocation.add(pl.config.defaultTier.getPath() + "/" + pl.config.locationToString(chestloc));
            return true;
        } else {
            chestloc = chestloc.clone().subtract(0, 1, 0);
            if(chestloc.getBlock().getState() instanceof Chest || chestloc.getBlock().getState() instanceof DoubleChest) {
                chestLocation.add(pl.config.defaultTier.getPath() + "/" + pl.config.locationToString(chestloc));
                return true;
            }
        }
        return false;
    }

    public boolean addChestLocation(Location chestloc, String tier) {
        if(pl.chestTiers.containsKey(tier)) {
            if(chestloc.getBlock().getState() instanceof Chest || chestloc.getBlock().getState() instanceof DoubleChest) {
                chestLocation.add(tier + "/" + pl.config.locationToString(chestloc));
                return true;
            } else {
                chestloc = chestloc.clone().subtract(0, 1, 0);
                if(chestloc.getBlock().getState() instanceof Chest || chestloc.getBlock().getState() instanceof DoubleChest) {
                    chestLocation.add(tier + "/" + pl.config.locationToString(chestloc));
                    return true;
                }
            }
        }
        return false;
    }

    public void addSpawnLocation(Location loc) {
        spawnLocation.add(pl.config.locationToString(loc));
    }

    public void addDeathmatchSpawnLocation(Location loc) {
        deathmatchLocation.add(pl.config.locationToString(loc));
    }

    public int getMinPlayer() {
        return minPlayer;
    }

    public void setMinPlayer(int minPlayer) {
        this.minPlayer = minPlayer;
    }

    public int getMaxPlayer() {
        return maxPlayer;
    }

    public void setMaxPlayer(int maxPlayer) {
        this.maxPlayer = maxPlayer;
    }

    public int getLightningThreshold() {
        return lightningThreshold;
    }

    public void setLightningThreshold(int lightningThreshold) {
        this.lightningThreshold = lightningThreshold;
    }

    public int getChestRefresh() {
        return chestRefresh;
    }

    public void setChestRefresh(int chestRefresh) {
        this.chestRefresh = chestRefresh;
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }

    public String getVotingDescription() {
        return votingDescription;
    }

    public void setVotingDescription(String votingDescription) {
        this.votingDescription = votingDescription.replaceAll("&", "§");
    }

    public String getCorner1() {
        return corner1;
    }

    public void setCorner1(Location corner1) {
        this.corner1 = pl.config.locationToString(corner1);
    }

    public String getCorner2() {
        return corner2;
    }

    public void setCorner2(Location corner2) {
        this.corner2 = pl.config.locationToString(corner2);
    }

    public String getSpectator() {
        return spectator;
    }

    public void setSpectator(Location spectator) {
        this.spectator = pl.config.locationToString(spectator);
    }

    public boolean isKickVips() {
        return kickVips;
    }

    public void setKickVips(boolean kickVips) {
        this.kickVips = kickVips;
    }

    public int getDeathmatchDuration() {
        return deathmatchDuration;
    }

    public void setDeathmatchDuration(int deathmatchDuration) {
        this.deathmatchDuration = deathmatchDuration;
    }

    public int getDeathmatchDistance() {
        return deathmatchDistance;
    }

    public void setDeathmatchDistance(int deathmatchDistance) {
        this.deathmatchDistance = deathmatchDistance;
    }

    public String getDeathmatchCenter() {
        return deathmatchCenter;
    }

    public void setDeathmatchCenter(Location center) {
        this.deathmatchCenter = pl.config.locationToString(center);
    }

    public void clearAllowedCommands() {
        allowedCommands.clear();
    }

    public void clearDescription() {
        description.clear();
    }

    public void clearBlockedCrafting() {
        blockCrafting.clear();
    }

    public void clearAllowedPlace() {
        allowPlace.clear();
    }

    public void clearAllowedBreak() {
        allowBreak.clear();
    }

    public void clearSpawnLocations() {
        spawnLocation.clear();
    }

    public void clearDeathmatchSpawnLocations() {
        deathmatchLocation.clear();
    }

    public boolean isSearchForChests() {
        return searchForChests;
    }

    public void setSearchForChests(boolean searchForChests) {
        this.searchForChests = searchForChests;
    }

    public boolean isEdited() {
        return edited;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName.replaceAll("&", "§");
    }

    public String getSafeLocation() {
        return safeLocation;
    }

    public String getExitLocation() {
        return exitLocation;
    }

    public void setSafeLocation(Location location) {
        this.safeLocation = pl.config.locationToString(location);
    }

    public void setExitLocation(Location location) {
        this.exitLocation = pl.config.locationToString(location);
    }

    public void setWaitTime(int time) {
        this.waitTime = time;
    }

    public void setGraceTime(int grace) {
        this.graceTime = grace;
    }

    public int getGraceTime() {
        return graceTime;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public int getPossibleJoinsWithEnough() {
        return withEnough;
    }

    public int getPossibleJoinsWithoutEnough() {
        return withoutEnough;
    }

    public int getPossibleJoinsTryDelay() {
        return possibleJoinsDelay;
    }

    public void setPossibleJoinsWithEnough(int i) {
        this.withEnough = i;
    }

    public void setPossibleJoinsWithoutEnough(int i) {
        this.withoutEnough = i;
    }

    public void setPossibleJoinsTryDelay(int i) {
        this.possibleJoinsDelay = i;
    }
}
