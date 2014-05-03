package com.jabyftw.sgames;

import com.jabyftw.sgames.commands.SGExecutor;
import com.jabyftw.sgames.commands.SGMExecutor;
import com.jabyftw.sgames.commands.SGSExecutor;
import com.jabyftw.sgames.item.Kit;
import com.jabyftw.sgames.item.SponsorKit;
import com.jabyftw.sgames.item.SupplyCrate;
import com.jabyftw.sgames.item.Tier;
import com.jabyftw.sgames.managers.Configuration;
import com.jabyftw.sgames.managers.EventListener;
import com.jabyftw.sgames.managers.Lobby;
import com.jabyftw.sgames.managers.Voting;
import com.jabyftw.sgames.util.Permissions;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.logging.Level;

/**
 * @author Rafael
 */
public class SurvivalGames extends JavaPlugin {

    private final Random random = new Random();
    public Configuration config;
    public Permissions permissions;
    public Voting voting;
    public EventListener listener;

    public LinkedHashMap<String, Kit> gameKits = new LinkedHashMap<String, Kit>();
    public LinkedHashMap<String, SponsorKit> sponsorKits = new LinkedHashMap<String, SponsorKit>();
    public LinkedHashMap<String, Tier> chestTiers = new LinkedHashMap<String, Tier>();
    public LinkedHashMap<String, Lobby> lobbies = new LinkedHashMap<String, Lobby>();
    public HashMap<Player, Lobby> players = new HashMap<Player, Lobby>();
    public HashMap<Player, String> muttated = new HashMap<Player, String>();
    public HashMap<Lobby, ArrayList<Sign>> dynamicSigns = new HashMap<Lobby, ArrayList<Sign>>();
    public ArrayList<Player> invisiblePlayers = new ArrayList<Player>();
    public ArrayList<String> haveMuttated = new ArrayList<String>(), stuckPlayers = new ArrayList<String>();
    public ArrayList<SupplyCrate> supplyCrates = new ArrayList<SupplyCrate>();

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        permissions = new Permissions();
        startup();
        getLogger().log(Level.INFO, "Enabled in {0}ms", (System.currentTimeMillis() - start));
    }

    @Override
    public void onDisable() {
        if(config.useVoting) {
            voting.removePlayers();
        }
        config.saveDynamicSigns();
        for(Lobby lob : lobbies.values()) {
            lob.endMatch(true, false);
        }
        if(config.useMySQL) {
            config.sql.closeConn();
        }
        config.clearVariables();
        getServer().getScheduler().cancelTasks(this);
    }

    public void startup() {
        config = new Configuration(this);
        config.startConfiguration();
        listener = new EventListener(this);
        getServer().getPluginCommand("sg").setExecutor(new SGExecutor(this));
        getServer().getPluginCommand("sgm").setExecutor(new SGMExecutor(this));
        getServer().getPluginCommand("sgs").setExecutor(new SGSExecutor(this));
    }

    public boolean onReload() {
        onDisable();
        startup();
        return true;
    }

    public String getStateName(State state) {
        switch(state) {
            case WAITING:
                return config.waitingString;
            case PREGAME:
                return config.pregameString;
            case PLAYING:
                return config.playingString;
            case DEATHMATCH:
                return config.deathmatchString;
            default:
                return config.disabledString;
        }
    }

    public ArrayList<Player> getPlayersFromLobby(Lobby lob) {
        ArrayList<Player> list = new ArrayList<Player>();
        for(Map.Entry<Player, Lobby> set : players.entrySet()) {
            if(set.getValue().equals(lob)) {
                list.add(set.getKey());
            }
        }
        return list;
    }

    public String getLang(String path) {
        return config.langPrefix + getNoPrefixLang(path);
    }

    public String getNoPrefixLang(String path) {
        return config.langY.getConfig().getString("lang." + path).replaceAll("&", "§");
    }

    public double getRandom(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    public void cleanPlayer(Player p, boolean flying, boolean removeScoreboard) {
        p.setGameMode(GameMode.SURVIVAL);
        p.setMaxHealth(20);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setExhaustion(0);
        p.setAllowFlight(flying);
        for(PotionEffect potioneffect : p.getActivePotionEffects()) {
            p.removePotionEffect(potioneffect.getType());
        }
        p.resetPlayerTime();
        p.resetPlayerWeather();
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        if(removeScoreboard) {
            p.setScoreboard(getServer().getScoreboardManager().getMainScoreboard());
        }
        //noinspection deprecation
        p.updateInventory();
    }

    public Lobby getLobby(String nameOrId) {
        try {
            Lobby lobby = lobbies.get(nameOrId);
            if(lobby != null) {
                return lobby;
            }
        } catch(NumberFormatException ignored) {
        }
        for(Lobby lobby : lobbies.values()) {
            if(lobby.getPath().equalsIgnoreCase(nameOrId)) {
                return lobby;
            } else if(ChatColor.stripColor(lobby.getName()).equalsIgnoreCase(nameOrId)) {
                return lobby;
            }
        }
        return null;
    }

    public void makeVisible(Player player) {
        invisiblePlayers.remove(player);
        for(Player online : getServer().getOnlinePlayers()) {
            if(online.equals(player)) {
                continue;
            }
            if(!online.canSee(player)) {
                online.showPlayer(player);
            }
        }
    }

    public void makeInvisible(Player player) {
        invisiblePlayers.add(player);
        for(Player online : getServer().getOnlinePlayers()) {
            if(online.equals(player)) {
                continue;
            }
            if(online.canSee(player)) {
                online.hidePlayer(player);
            }
        }
    }

    public enum State {

        WAITING, PREGAME, PLAYING, DEATHMATCH, DISABLED
    }
}
