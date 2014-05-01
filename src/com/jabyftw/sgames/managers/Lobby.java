package com.jabyftw.sgames.managers;

import com.jabyftw.sgames.Jogador;
import com.jabyftw.sgames.Spectator;
import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.SurvivalGames.State;
import com.jabyftw.sgames.item.Kit;
import com.jabyftw.sgames.item.SponsorKit;
import com.jabyftw.sgames.util.IconMenu;
import com.jabyftw.sgames.util.IconMenu.IconMenuEventHandler;
import com.jabyftw.sgames.util.RankingEntry;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;

/**
 * @author Rafael
 */
@SuppressWarnings("deprecation")
public class Lobby {

    private final SurvivalGames pl;
    private final Arena arenaManager;
    private final String path, displayName, votingDescription;
    private final boolean kickNonvips;
    private final ArrayList<String> additionalLore = new ArrayList<String>(), allowedCommands = new ArrayList<String>();
    private final int minPlayers, maxPlayers, matchDuration, deathmatchDuration, distanceFromCenter, aliveSizeToLightning, chestDelay, graceTime, waitTime,
            possibleJoinWarnedMaior, possibleJoinWarnedMenor, possibleJoinsDelay;

    private final ArrayList<Integer> deathmatchAnnounced = new ArrayList<Integer>();
    private final HashMap<Player, Jogador> onSponsoring = new HashMap<Player, Jogador>();
    private final HashMap<Player, SponsorKit> sponsors = new HashMap<Player, SponsorKit>();
    private final LinkedHashMap<Player, Spectator> spectators = new LinkedHashMap<Player, Spectator>();
    private final LinkedHashMap<Player, Jogador> players = new LinkedHashMap<Player, Jogador>();
    private final LinkedHashMap<Player, Kit> preplayers = new LinkedHashMap<Player, Kit>();

    private IconMenu[] playerList;
    private State state = State.DISABLED;
    private boolean imortal, worth;
    private int duration;
    private BukkitTask durationRunnable, deathmatchRunnable, chestRunnable, lightningRunnable, waitingRunnable;

    public Lobby(SurvivalGames pl, String path, String name, String description, List<String> lore, List<String> commands,
                 int min, int max, int matchduration, int deathmatchduration, int distancefromcenter, int alivelightning, int chestdelay, int gracetime, int waittime,
                 int warnedmaior, int warnedmenor, int possiblejoinsdelay,
                 boolean kicknonvip, Arena arena) {
        this.pl = pl;
        this.path = path;
        this.displayName = name;
        this.votingDescription = description;
        this.additionalLore.addAll(lore);
        this.allowedCommands.addAll(commands);
        this.chestDelay = (chestdelay * 20);
        this.arenaManager = arena;
        this.aliveSizeToLightning = alivelightning;
        this.distanceFromCenter = distancefromcenter;
        this.matchDuration = matchduration;
        this.deathmatchDuration = deathmatchduration;
        this.kickNonvips = kicknonvip;
        this.minPlayers = min;
        this.maxPlayers = max;
        this.graceTime = gracetime;
        this.waitTime = waittime;
        this.possibleJoinWarnedMaior = warnedmaior;
        this.possibleJoinWarnedMenor = warnedmenor;
        this.possibleJoinsDelay = possiblejoinsdelay;
        if(arenaManager.checkVariables()) {
            state = State.WAITING;
            duration = this.matchDuration;
            startWaitingRunnable();
            pl.getLogger().log(Level.INFO, "Lobby {0} is ready for use.", path);
        }
    }

    public void startMatch() {
        if(state == State.WAITING && preplayers.size() > 0) {
            state = State.PREGAME;
            endWaitingRunnable();
            arenaManager.removeEntities();
            prepareAllPlayers();
            updatePlayersScoreboards();
            broadcastMessage(pl.getLang("startedInXSeconds").replaceAll("%time", Integer.toString(waitTime)));
            new BukkitRunnable() {

                @Override
                public void run() {
                    state = State.PLAYING;
                    updatePlayerList();
                    startDurationRunnable();
                    startChestRunnable();
                    startLightningRunnable();
                    startAllPlayers();
                    broadcastMessage(pl.getLang("gameStarted"));
                    worth = getAliveJogador().size() >= minPlayers;
                    if(!worth) {
                        broadcastMessage(pl.getLang("gameIsntWorth"));
                    }
                    updatePlayersScoreboards();
                    broadcastMessage(pl.getLang("immortalityInXSeconds").replaceAll("%time", Integer.toString(graceTime)));
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            imortal = false;
                            broadcastMessage(pl.getLang("immortalityIsOver"));
                        }
                    }.runTaskLater(pl, graceTime * 20);
                }
            }.runTaskLater(pl, waitTime * 20);
        }
    }

    public void startDeathmatch() {
        if(arenaManager.useDeathmatch() && state == State.PLAYING) {
            state = State.DEATHMATCH;
            preparePlayersDeathmatch();
            endChestRunnable();
            endLightningRunnable();
            broadcastMessage(pl.getLang("deathmatchIn30Seconds"));
            new BukkitRunnable() {

                @Override
                public void run() {
                    startLightningRunnable();
                    startDeathmatchRunnable();
                    startPlayersDeathmatch();
                    broadcastMessage(pl.getLang("deathmatchStarted"));
                }
            }.runTaskLater(pl, 20 * 30);
        }
    }

    public void endMatch(boolean stopping, boolean hasWinner) {
        if(state.equals(State.PLAYING) || state.equals(State.DEATHMATCH)) {
            state = State.DISABLED;
            arenaManager.removeEntities();
            arenaManager.removeFallenCrates();
            arenaManager.clearInventories();
            arenaManager.restoreBrokenAndPlacedBlocks();
            endDeathmatchRunnable();
            endDurationRunnable();
            endChestRunnable();
            endLightningRunnable();
            if(getTheOnlyPlayer() != null && !stopping && hasWinner) {
                Jogador jogador = getTheOnlyPlayer();
                if(worth) {
                    jogador.getRanking().addWinToCounter();
                    jogador.addWinReward();
                }
                jogador.setPlaying(false);
                broadcastMessage(pl.getLang("gameIsOverWinner").replaceAll("%winner", jogador.getPlayer().getDisplayName()));
            } else {
                broadcastMessage(pl.getLang("gameIsOver"));
            }
            removeAllPlayersStopping();
            playerList = null;
            preplayers.clear();
            players.clear();
            onSponsoring.clear();
            spectators.clear();
            sponsors.clear();
            if(!stopping) {
                arenaManager.resetSpawnUsage();
                state = State.WAITING;
                updatePlayersScoreboards();
                duration = matchDuration;
                startWaitingRunnable();
            }
            if(pl.config.useVoting) {
                pl.voting.restartVoting(stopping);
            }
        }
    }

    public boolean getImortality() {
        return imortal;
    }

    public State getCurrentState() {
        return state;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public String getDurationRemaining() {
        if(state == State.PLAYING || state == State.DEATHMATCH) {
            return Integer.toString(duration);
        } else {
            return pl.getLang("command.durationDidntStarted");
        }
    }

    public ArrayList<String> getLore() {
        return additionalLore;
    }

    public ArrayList<String> getAllowedCommands() {
        return allowedCommands;
    }

    public void broadcastMessage(String message) {
        for(Player p : getLobbyPlayers()) {
            p.sendMessage(message);
        }
        pl.getLogger().log(Level.INFO, path + ": " + message);
    }

    public ArrayList<Player> getLobbyPlayers() {
        return pl.getPlayersFromLobby(this);
    }

    public ArrayList<Jogador> getAliveJogador() {
        ArrayList<Jogador> alive = new ArrayList<Jogador>();
        for(Jogador j : players.values()) {
            if(j.isPlaying()) {
                alive.add(j);
            }
        }
        return alive;
    }

    public void startDurationRunnable() {
        durationRunnable = new BukkitRunnable() {

            @Override
            public void run() {
                if(getCurrentState() == State.PLAYING || getCurrentState() == State.DEATHMATCH) {
                    duration--;
                    if(arenaManager.useDeathmatch() && state == State.PLAYING) {
                        for(int x = 5; x >= 1; x--) {
                            if(duration <= (deathmatchDuration + x) && !deathmatchAnnounced.contains(x)) {
                                broadcastMessage(pl.getLang("deathmatchWillStartIn").replaceAll("%minutes", Integer.toString(x)));
                                deathmatchAnnounced.add(x);
                            }
                        }
                        if(duration <= deathmatchDuration) {
                            startDeathmatch();
                        }
                    }
                    if(duration <= 0) {
                        endMatch(false, false);
                    }
                }
            }
        }.runTaskTimer(pl, 1, 20 * 60);
    }

    public void startLightningRunnable() {
        if(aliveSizeToLightning > 0) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    if(getCurrentState() == State.PLAYING || getCurrentState() == State.DEATHMATCH) {
                        if(getAliveJogador().size() <= aliveSizeToLightning) {
                            for(Jogador j : getAliveJogador()) {
                                j.getPlayer().getWorld().strikeLightningEffect(j.getPlayer().getLocation());
                            }
                        }
                    }
                }
            }.runTaskTimer(pl, 20 * 10, 20 * 10);
        }
    }

    public void startDeathmatchRunnable() {
        if(arenaManager.useDeathmatch()) {
            deathmatchRunnable = new BukkitRunnable() {

                @Override
                public void run() {
                    if(getCurrentState() == State.DEATHMATCH) {
                        for(Jogador j : getAliveJogador()) {
                            Player p = j.getPlayer();
                            if(p.getLocation().distanceSquared(arenaManager.getDeathmatchCenter()) >= (distanceFromCenter * distanceFromCenter)) {
                                double damage = (p.getMaxHealth() * 0.2D);
                                if((p.getHealth() - damage) <= 0) {
                                    removePlayer(p, true);
                                } else {
                                    p.damage(damage);
                                    p.sendMessage(pl.getLang("tooFarFromCenter"));
                                }
                            }
                        }
                    }
                }
            }.runTaskTimer(pl, 20 * 5, 20 * 5);
        }
    }

    public void startChestRunnable() {
        if(chestDelay > 0) {
            chestRunnable = new BukkitRunnable() {

                @Override
                public void run() {
                    if(state == State.PLAYING && arenaManager.refreshChests()) {
                        broadcastMessage(pl.getLang("chestRefilled"));
                    }
                }
            }.runTaskTimer(pl, 0, chestDelay);
        }
    }

    private void startWaitingRunnable() {
        imortal = true;
        waitingRunnable = new BukkitRunnable() {
            int warnedMaior = 0;
            int warnedMenor = 0;

            @Override
            public void run() {
                if(state == State.WAITING && preplayers.size() > 0) {
                    if(preplayers.size() >= minPlayers) {
                        warnedMenor = 0;
                        if(preplayers.size() < maxPlayers) {
                            if(warnedMaior >= possibleJoinWarnedMaior) {
                                startMatch();
                            } else {
                                broadcastMessage(pl.getLang("waitingPossibleJoins").replaceAll("%tries", Integer.toString(warnedMaior + 1)).replaceAll("%maxtries", Integer.toString(possibleJoinWarnedMaior)));
                                warnedMaior++;
                            }
                        } else {
                            startMatch();
                        }
                    } else {
                        warnedMaior = 0;
                        if(warnedMenor >= possibleJoinWarnedMenor) {
                            startMatch();
                        } else {
                            broadcastMessage(pl.getLang("notEnoughPlayers").replaceAll("%needed", Integer.toString(minPlayers - preplayers.size())).replaceAll("%tries", Integer.toString(possibleJoinWarnedMenor - warnedMenor + 1)));
                            broadcastMessage(pl.getLang("gameWontBeWorth"));
                            warnedMenor++;
                        }
                    }
                } else {
                    warnedMenor = 0;
                    warnedMaior = 0;
                }
            }
        }.runTaskTimer(pl, 0, 20 * possibleJoinsDelay);
    }

    public void endChestRunnable() {
        if(chestRunnable != null) {
            chestRunnable.cancel();
            chestRunnable = null;
        }
    }

    public void endLightningRunnable() {
        if(lightningRunnable != null) {
            lightningRunnable.cancel();
            lightningRunnable = null;
        }
    }

    public void endDurationRunnable() {
        if(durationRunnable != null) {
            durationRunnable.cancel();
            durationRunnable = null;
        }
        duration = matchDuration;
    }

    public void endDeathmatchRunnable() {
        if(deathmatchRunnable != null) {
            deathmatchRunnable.cancel();
            deathmatchRunnable = null;
        }
        deathmatchAnnounced.clear();
    }

    public void endWaitingRunnable() {
        if(waitingRunnable != null) {
            waitingRunnable.cancel();
            waitingRunnable = null;
        }
    }

    private void startAllPlayers() {
        for(Jogador j : getAliveJogador()) {
            Player p = j.getPlayer();
            if(pl.stuckPlayers.contains(p.getName())) {
                pl.stuckPlayers.remove(p.getName());
            }
            givePlayerPermissions(j.getPlayer(), j.getKit());
            j.getKit().usePotionEffects(j);
            p.setPlayerTime(1, true);
        }
    }

    private void updatePlayersScoreboards() {
        for(Jogador jogador : players.values()) {
            jogador.getScoreboardManager().setLines(pl.config.getScoreboardTitle(state), pl.config.getScoreboardLines(state));
        }
    }

    private void prepareAllPlayers() {
        for(Map.Entry<Player, Kit> set : preplayers.entrySet()) {
            RankingEntry ranking = new RankingEntry(pl, set.getKey().getName(), pl.config.getStats(set.getKey().getName()));
            Jogador jog = new Jogador(pl, set.getKey(), this, ranking, set.getValue());
            jog.setPlaying(true);
            players.put(set.getKey(), jog);
            set.getKey().teleport(arenaManager.getSpawnLocation());
            pl.cleanPlayer(set.getKey(), false);
            equipWithKitItems(set.getKey(), set.getValue());
            pl.stuckPlayers.add(set.getKey().getName());
        }
        preplayers.clear();
    }

    private void preparePlayersDeathmatch() {
        if(state == State.DEATHMATCH) {
            for(Jogador j : getAliveJogador()) {
                Player p = j.getPlayer();
                p.teleport(arenaManager.getDeathmatchSpawnLocation());
                pl.stuckPlayers.add(p.getName());
            }
        }
    }

    private void startPlayersDeathmatch() {
        if(state == State.DEATHMATCH) {
            for(Jogador j : getAliveJogador()) {
                Player p = j.getPlayer();
                if(pl.stuckPlayers.contains(p.getName())) {
                    pl.stuckPlayers.remove(p.getName());
                }
            }
        }
    }

    private Jogador getTheOnlyPlayer() {
        List<Jogador> list = new ArrayList<Jogador>(getAliveNonMuttatorPlayers());
        if(list.size() == 1) {
            return list.get(0);
        }
        return null;
    }

    private void removeAllPlayersStopping() {
        for(Player p : getLobbyPlayers()) {
            removePlayer(p, false);
        }
    }

    public void addPlayer(Player p, Kit kit) {
        if(p.hasPermission(pl.permissions.player_join)) {
            if(state == State.WAITING) {
                if(isFull(p.hasPermission(pl.permissions.player_joinfulllobby))) {
                    if(pl.players.containsKey(p)) {
                        p.sendMessage(pl.getLang("alreadyOnLobby"));
                    } else {
                        if(arenaManager.haveSuficientSpawns(preplayers.size() + 1)) {
                            preplayers.put(p, kit);
                            pl.players.put(p, this);
                            p.teleport(arenaManager.getSafeLocation());
                            p.sendMessage(pl.getLang("youJoinedTheLobby"));
                        } else {
                            p.sendMessage(pl.getLang("noSpawnLeft"));
                        }
                    }
                } else {
                    p.sendMessage(pl.getLang("lobbyIsFull"));
                }
            } else {
                p.sendMessage(pl.getLang("gameAlreadyStarted"));
            }
        } else {
            p.sendMessage(pl.getLang("noPermission"));
        }
    }

    public void removePlayer(Player p, boolean checkwin) {
        if(pl.players.containsKey(p) && pl.players.get(p).equals(this)) {
            pl.players.remove(p);
        }
        if(players.containsKey(p)) {
            Jogador j = players.get(p);
            j.getScoreboardManager().stopRunnable(true);
            if(j.isPlaying()) {
                dropInventory(p);
                j.setPlaying(false);
                if(worth) {
                    j.getRanking().addDeathToCounter();
                }
            }
            pl.cleanPlayer(p, false);
            players.remove(p);
            if(checkwin) {
                checkWin();
            }
            updatePlayerList();
            pl.makeVisible(p);
            p.teleport(arenaManager.getExitLocation());
        } else if(spectators.containsKey(p)) {
            pl.makeVisible(p);
            pl.cleanPlayer(p, false);
            spectators.remove(p);
            p.teleport(arenaManager.getExitLocation());
        }
        sponsors.remove(p);
        onSponsoring.remove(p);
        preplayers.remove(p);
        pl.muttated.remove(p);
        pl.config.prejoin.remove(p.getName());
        pl.stuckPlayers.remove(p.getName());
    }

    private void equipWithKitItems(Player player, Kit kit) {
        if(kit.canUseKit(player)) {
            player.getInventory().clear();
            for(ItemStack itemstack : kit.getItems()) {
                if(pl.config.autoequipHelmet.contains(itemstack.getType()) && player.getInventory().getHelmet() == null) {
                    player.getInventory().setHelmet(itemstack);
                } else if(pl.config.autoequipChestplate.contains(itemstack.getType()) && player.getInventory().getChestplate() == null) {
                    player.getInventory().setChestplate(itemstack);
                } else if(pl.config.autoequipLeggings.contains(itemstack.getType()) && player.getInventory().getLeggings() == null) {
                    player.getInventory().setLeggings(itemstack);
                } else if(pl.config.autoequipBoots.contains(itemstack.getType()) && player.getInventory().getBoots() == null) {
                    player.getInventory().setBoots(itemstack);
                } else {
                    player.getInventory().addItem(itemstack);
                }
            }
            player.sendMessage(pl.getLang("youAreUsingKit").replaceAll("%name", kit.getName()));
        }
    }

    private void givePlayerPermissions(Player player, Kit kit) {
        if(pl.config.useVault) {
            for(String permission : kit.getPermissionsToGive()) {
                pl.config.perm.playerAdd(player.getWorld(), player.getName(), permission);
            }
        }
    }

    private void removePlayerPermissions(Jogador jogador) {
        if(pl.config.useVault) {
            for(String permission : jogador.getKit().getPermissionsToGive()) {
                if(pl.config.perm.has(jogador.getPlayer(), permission)) {
                    pl.config.perm.playerRemove(jogador.getPlayer().getWorld(), jogador.getPlayer().getName(), permission);
                }
            }
        }
    }

    private void dropInventory(Player p) {
        for(ItemStack itemstack : p.getInventory().getContents()) {
            if(itemstack != null && !itemstack.getType().equals(Material.AIR)) {
                arenaManager.addToRemove(p.getWorld().dropItemNaturally(p.getLocation(), itemstack));
            }
        }
        for(ItemStack itemstack : p.getInventory().getArmorContents()) {
            if(itemstack != null && !itemstack.getType().equals(Material.AIR)) {
                arenaManager.addToRemove(p.getWorld().dropItemNaturally(p.getLocation(), itemstack));
            }
        }
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
    }

    private void checkWin() {
        if(getAliveNonMuttatorPlayers().size() <= 1) {
            endMatch(false, true);
        }
    }

    public ArrayList<Jogador> getAliveNonMuttatorPlayers() {
        ArrayList<Jogador> list = new ArrayList<Jogador>();
        for(Jogador jogador : getAliveJogador()) {
            if(!pl.muttated.containsKey(jogador.getPlayer())) {
                list.add(jogador);
            }
        }
        return list;
    }

    private boolean isFull(boolean canJoinFull) {
        if(preplayers.size() >= maxPlayers && canJoinFull) { // if is not full and player can join full
            if(kickNonvips) { // if should kick non vip
                Iterator<Player> iterator = preplayers.keySet().iterator();
                while(preplayers.size() >= maxPlayers && iterator.hasNext() && state == State.WAITING) { // while is full and preplayers contains player
                    Player player = iterator.next();
                    if(!player.hasPermission(pl.permissions.player_joinfulllobby)) { // if player is not a vip
                        removePlayer(player, false);
                        player.sendMessage(pl.getLang("youWereRemovedFromTheLobby"));
                    }
                }
            } else { // shouldn't kick anyone, allow join
                return true;
            }
        }
        return preplayers.size() < maxPlayers; // else just return a normal "is full"
    }

    public boolean isPlayerAlive(Player p) {
        for(Map.Entry<Player, Jogador> set : players.entrySet()) {
            if(set.getKey().equals(p) && set.getValue().isPlaying()) {
                return true;
            }
        }
        return false;
    }

    public Iterable<Map.Entry<Player, SponsorKit>> getSponsors() {
        return sponsors.entrySet();
    }

    private void updatePlayerList() {
        if(state == State.PLAYING || state == State.DEATHMATCH) {
            int playerCounter = this.getAliveJogador().size();
            int pageNumber = 0;
            while(playerCounter > 0) {
                while(playerCounter <= (54 - 3)) {
                    pageNumber++;
                    playerCounter -= pl.config.getPageSize(playerCounter, 3);
                    if(playerCounter <= 0) {
                        break;
                    }
                }
            }
            playerList = new IconMenu[pageNumber];
            playerCounter = this.getAliveJogador().size();
            for(int page = 0; page < pageNumber; page++) {
                int size = pl.config.getPageSize(playerCounter, 3);
                playerList[page] = new IconMenu(pl, pl.config.guiY.getConfig().getString("config.playerListInventoryTitle").replaceAll("&", "§"), size, page, new IconMenuEventHandler() {

                    @Override
                    public void onInventoryClick(IconMenu.InventoryMenuClickEvent e) {
                        switch(e.getSlot()) {
                            case 0:
                                if(e.getPage() + 1 <= playerList.length - 1) {
                                    e.getPlayer().closeInventory();
                                    playerList[e.getPage() + 1].openInventory(e.getPlayer());
                                }
                                e.setClosing(false);
                                break;
                            case 1:
                                if(e.getPage() - 1 >= 0) {
                                    e.getPlayer().closeInventory();
                                    playerList[e.getPage() - 1].openInventory(e.getPlayer());
                                }
                                e.setClosing(false);
                                break;
                            case 2:
                                e.setClosing(true);
                                break;
                            default:
                                if(e.getItem() != null && e.getItem().getItemMeta().hasDisplayName()) {
                                    Player sponsored = pl.getServer().getPlayerExact(e.getItem().getItemMeta().getDisplayName());
                                    if(sponsored != null && pl.players.containsKey(sponsored) && players.containsKey(sponsored) && players.get(sponsored).isPlaying()) {
                                        onSponsoring.put(e.getPlayer(), players.get(sponsored));
                                        e.setClosing(false);
                                        e.getPlayer().closeInventory();
                                        try {
                                            pl.config.sponsorMenu[0].openInventory(e.getPlayer());
                                        } catch(ArrayIndexOutOfBoundsException ignored) {
                                            e.getPlayer().sendMessage(pl.getLang("command.GUINotGenerated"));
                                        }
                                        break;
                                    }
                                    e.setClosing(true);
                                    e.getPlayer().sendMessage(pl.getLang("playerNotFound"));
                                }
                                break;
                        }
                    }

                    @Override
                    public void onInventoryClose(IconMenu.InventoryMenuCloseEvent e) {
                    }
                });
                playerCounter -= size;
            }
            generatePlayerListOptions();
        }
    }

    public IconMenu getPlayerList() {
        return playerList[0];
    }

    public Player getSponsorTo(Player p) {
        if(onSponsoring.containsKey(p) && onSponsoring.get(p).isPlaying() && players.containsValue(onSponsoring.get(p))) {
            return onSponsoring.get(p).getPlayer();
        }
        return null;
    }

    public void removeSponsoring(Player buyier) {
        if(onSponsoring.containsKey(buyier)) {
            onSponsoring.remove(buyier);
            buyier.sendMessage(pl.getLang("sponsoringCancelled"));
        }
    }

    private void generatePlayerListOptions() {
        ArrayList<Jogador> jogadores = new ArrayList<Jogador>(this.getAliveJogador());
        Iterator<Jogador> iterator = jogadores.iterator();
        for(IconMenu iconMenu : playerList) {
            if(iconMenu.getPage() + 1 <= playerList.length - 1) {
                iconMenu.setOption(0, pl.config.nextPage);
            } else {
                iconMenu.setOption(0, pl.config.disabledPage);
            }
            if(iconMenu.getPage() - 1 >= 0) {
                iconMenu.setOption(1, pl.config.backPage);
            } else {
                iconMenu.setOption(1, pl.config.disabledPage);
            }
            iconMenu.setOption(2, pl.config.closePage);
            int atual = 3;
            while(iterator.hasNext()) {
                if(atual < 54) {
                    Jogador jogador = iterator.next();
                    String[] list = {"material:skull_item", "skullname:" + jogador.getPlayer().getName(), "damage:3", "quantity:1"};
                    ItemStack shown = pl.config.resolveItemStack(Arrays.asList(list));
                    ItemMeta meta = shown.getItemMeta();
                    meta.setDisplayName(jogador.getPlayer().getName());
                    shown.setItemMeta(meta);
                    iconMenu.setOption(atual, shown);
                    iterator.remove();
                    atual++;
                } else {
                    break;
                }
            }
        }
    }

    public boolean isPlayerSpectator(Player player) {
        return spectators.containsKey(player) || (players.containsKey(player) && !players.get(player).isPlaying());
    }

    public Arena getArena() {
        return arenaManager;
    }

    public void addDeath(Player player) {
        if(players.containsKey(player)) {
            Jogador jogador = players.get(player);
            if(worth) {
                jogador.getRanking().addDeathToCounter();
            }
            jogador.getRanking().saveStats();
            dropInventory(player);
            jogador.setPlaying(false);
            removePlayerPermissions(jogador);
            updatePlayerList();
            addSpectator(jogador);
            broadcastMessage(pl.getLang("playerDeath").replaceAll("%displayname", player.getDisplayName()).replaceAll("%alive", Integer.toString(getAliveNonMuttatorPlayers().size())));
        } else {
            removePlayer(player, true);
            player.sendMessage("Occurred an error while dying");
        }
        checkWin();
    }

    private void addSpectator(Jogador jogador) {
        setSpectator(jogador.getPlayer());
        spectators.put(jogador.getPlayer(), jogador);
    }

    public void addDeath(Player damaged, Player damager) {
        if(pl.muttated.containsKey(damaged)) {
            if(pl.muttated.get(damaged).equals(damager.getName()) && players.containsKey(damager)) {
                players.get(damager).addFailedMuttationBonus();
            }
            pl.muttated.remove(damaged);
        }
        if(players.containsKey(damaged)) {
            players.get(damaged).setKiller(damager);
            addDeath(damaged);
        }
        if(players.containsKey(damager)) {
            if(pl.muttated.containsValue(damager.getName())) {
                Iterator<Map.Entry<Player, String>> iterator = pl.muttated.entrySet().iterator();
                //noinspection WhileLoopReplaceableByForEach
                while(iterator.hasNext()) {
                    Map.Entry<Player, String> set = iterator.next();
                    if(set.getValue().equals(damager.getName())) {
                        set.getKey().sendMessage(pl.getLang("muttationSucceededTargetKilled"));
                        players.get(set.getKey()).addMuttationReward();
                        removePlayer(set.getKey(), true);
                    }
                }
            }
            if(worth) {
                players.get(damager).addKillReward();
                players.get(damager).getRanking().addKillToCounter();
            }
            players.get(damager).getKit().usePotionEffectsOnKill(damager);
        }
    }

    public String getPath() {
        return path;
    }

    public Spectator getSpectatorFromPlayer(Player player) {
        return spectators.get(player);
    }

    public String getAliveNonMutattorSize() {
        if(state == State.PLAYING || state == State.DEATHMATCH) {
            return Integer.toString(getAliveNonMuttatorPlayers().size());
        } else {
            return Integer.toString(preplayers.size());
        }
    }

    public boolean addSponsor(Player player, SponsorKit sponsor, Player sponsored) {
        if(players.containsKey(sponsored) && players.get(sponsored).isPlaying()) {
            sponsors.put(player, sponsor);
            players.get(sponsored).addSponsor(player.getName());
            return true;
        }
        return false;
    }

    public void addSpectator(Player player) {
        if(player.hasPermission(pl.permissions.player_spectate)) {
            if(state == State.PLAYING || state == State.DEATHMATCH || state == State.PREGAME) {
                setSpectator(player);
                spectators.put(player, new Spectator(player, this));
                pl.players.put(player, this);
                player.sendMessage(pl.getLang("youreASpectator"));
            } else {
                player.sendMessage(pl.getLang("matchNotReady"));
            }
        } else {
            player.sendMessage(pl.getLang("noPermission"));
        }
    }

    public void setSpectator(Player player) {
        pl.cleanPlayer(player, true);
        for(ItemStack is : pl.config.spectatorInventory) {
            player.getInventory().addItem(is);
        }
        player.teleport(arenaManager.getSpectatorSpawn());
        player.updateInventory();
        pl.makeInvisible(player);
    }

    public String getVotingDescription() {
        return votingDescription;
    }

    public boolean muttatePlayer(Jogador jogador, Player killer) {
        if(pl.config.useMuttation && isPlayerAlive(killer) && getCurrentState() == State.PLAYING && !pl.haveMuttated.contains(jogador.getPlayer().getName().toLowerCase())) {
            if(pl.config.usePlayerPoints) {
                if(pl.config.playerp.getAPI().take(jogador.getPlayer().getUniqueId(), pl.config.muttationCost)) {
                    muttate(jogador);
                    pl.muttated.put(jogador.getPlayer(), killer.getName());
                    return true;
                }
            } else if(pl.config.useVault) {
                if(pl.config.econ.withdrawPlayer(jogador.getPlayer().getName(), pl.config.muttationCost).transactionSuccess()) {
                    muttate(jogador);
                    pl.muttated.put(jogador.getPlayer(), killer.getName());
                    return true;
                }
            }
        }
        return false;
    }

    private void muttate(Jogador jogador) {
        if(spectators.containsKey(jogador.getPlayer())) {
            spectators.remove(jogador.getPlayer());
        }
        jogador.setPlaying(true);
        jogador.setAlreadyMuttated(true);
        pl.cleanPlayer(jogador.getPlayer(), false);
        pl.makeVisible(jogador.getPlayer());
        equipWithKitItems(jogador.getPlayer(), pl.config.muttationKit);
        jogador.getPlayer().teleport(arenaManager.getSpawnLocation());
        jogador.getPlayer().updateInventory();
        pl.haveMuttated.add(jogador.getPlayer().getName().toLowerCase());
    }

    public int getLightningThreshold() {
        return aliveSizeToLightning;
    }

    public int getChestRefresh() {
        return chestDelay;
    }

    public int getMaxDuration() {
        return matchDuration;
    }

    public boolean getKickVips() {
        return kickNonvips;
    }

    public int getDeathmatchDistance() {
        return distanceFromCenter;
    }

    public int getDeathmatchDuration() {
        return deathmatchDuration;
    }

    public void setCurrentState(State currentState) {
        this.state = currentState;
        updatePlayersScoreboards();
    }

    public String getName() {
        return displayName;
    }

    public int getGraceTime() {
        return graceTime;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public int getPossibleJoinsWithEnoughPlayers() {
        return possibleJoinWarnedMaior;
    }

    public int getPossibleJoinsWithoutEnoughPlayers() {
        return possibleJoinWarnedMenor;
    }

    public int getPossibleJoinsDelay() {
        return possibleJoinsDelay;
    }
}
