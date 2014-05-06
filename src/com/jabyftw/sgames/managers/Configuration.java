package com.jabyftw.sgames.managers;

import com.jabyftw.sgames.Jogador;
import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.SurvivalGames.State;
import com.jabyftw.sgames.commands.GameSetup;
import com.jabyftw.sgames.item.Kit;
import com.jabyftw.sgames.item.SponsorKit;
import com.jabyftw.sgames.item.Tier;
import com.jabyftw.sgames.util.CustomConfig;
import com.jabyftw.sgames.util.IconMenu;
import com.jabyftw.sgames.util.IconMenu.IconMenuEventHandler;
import com.jabyftw.sgames.util.MySQL;
import com.jabyftw.sgames.util.ScoreboardManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.logging.Level;

/**
 * @author Rafael
 */
public final class Configuration {

    private final SurvivalGames pl;
    private final String[] empty = {};
    public CustomConfig configY, langY, lobbiesY, itemsY, guiY, scoreboardY;

    public final LinkedHashMap<String, Lobby> prejoin = new LinkedHashMap<String, Lobby>();
    public final ArrayList<ItemStack> spectatorInventory = new ArrayList<ItemStack>();
    public final ArrayList<EntityType> entitiesToRemove = new ArrayList<EntityType>();
    public final ArrayList<String> defaultAllowedCommands = new ArrayList<String>();
    public ArrayList<Material> autoequipHelmet = new ArrayList<Material>(), autoequipChestplate = new ArrayList<Material>(), autoequipLeggings = new ArrayList<Material>(), autoequipBoots = new ArrayList<Material>();

    public Permission perm = null;
    public Economy econ = null;
    public PlayerPoints playerp = null;
    public MySQL sql = null;

    public IconMenu[] joinPages, kitPages, sponsorMenu;
    public ItemStack nextPage, backPage, disabledPage, closePage, noKitItem;
    public Kit defaultKit, muttationKit;
    public Tier defaultTier, crateTier;
    public Material spectatorPlayerSelector, spectatorPlayerSponsor, spectatorMuttation, playerSearcher, playerCrate;

    public boolean useVault, usePlayerPoints, useMySQL, useVoting, useAutojoin, useSponsor, tntAutoIgnite, useMuttation, tntUseFakeExplosion, useChat, announceCrateDeploy, playCrateFirework, blockDropItem,
            useBarAPI;
    public String waitingString, pregameString, playingString, deathmatchString, disabledString, chatFormat, crateAnnouncement, crateInventoryTitle, langPrefix;
    public int valuePerWin, valuePerKill, guiUpdateDelay, tntPower, tntDelay, maxDistance, signDelay, rankingEntries, muttationCost, muttationBonus, muttationReward, scoreboardDelay;
    public String[] signLines = new String[4];

    public Configuration(SurvivalGames pl) {
        this.pl = pl;
    }

    public void startConfiguration() {
        configY = new CustomConfig(pl, "config");
        langY = new CustomConfig(pl, "messages");
        lobbiesY = new CustomConfig(pl, "lobbies");
        itemsY = new CustomConfig(pl, "items");
        guiY = new CustomConfig(pl, "gui");
        scoreboardY = new CustomConfig(pl, "scoreboard");
        generateConfig();
        generateLang();
        generateLobbies();
        generateItems();
        generateGUIConfiguration();
        generateScoreboardConfig();
        loadConfig();
        generateJoinPages();
        generateKitPages();
        if(kitPages == null || ((kitPages.length == 0 || pl.gameKits.size() == 0) && !useVoting)) {
            pl.getLogger().log(Level.SEVERE, "You don't have any kit, you won't be able to join.");
        }
        if(useSponsor) {
            generateSponsorPages();
        }
        generateDynamicSignsTask();
    }

    private void generateConfig() {
        FileConfiguration config = configY.getConfig();
        String[] commands = {"tell"},
                entities = {"arrow", "dropped_item", "FALLING_BLOCK", "EXPERIENCE_ORB"},
                signlines = {"&6%displayname", "&c-----", "&a%alive&7/&c%max", "&c%state"},
                item1 = {"material:iron_hoe", "quantity:1", "lore:&cMove throgh player list", "displayname:&6Player Selector"},
                item2 = {"material:gold_ingot", "quantity:1", "lore:&cSponsor a player!", "displayname:&cSponsor Manager"},
                item3 = {"material:gold_sword", "quantity:1", "displayname:&4Muttate"},
                helmet = {"leather_helmet", "chainmail_helmet", "iron_helmet", "diamond_helmet", "gold_helmet"},
                chestplate = {"leather_chestplate", "chainmail_chestplate", "iron_chestplate", "diamond_chestplate", "gold_chestplate"},
                leggings = {"leather_leggings", "chainmail_leggings", "iron_leggings", "diamond_leggings", "gold_leggings"},
                boots = {"leather_boots", "chainmail_boots", "iron_boots", "diamond_boots", "gold_boots"};
        config.addDefault("config.supportVault", false);
        config.addDefault("config.supportPlayerPoints", false);
        config.addDefault("config.supportBarAPI", false);
        config.addDefault("config.generateExampleLobbiesConfiguration", false);
        config.addDefault("config.generateExampleItemConfiguration", true);
        config.addDefault("config.mysql.enabled", false);
        config.addDefault("config.mysql.username", "root");
        config.addDefault("config.mysql.password", "root");
        config.addDefault("config.mysql.url", "jdbc:mysql://localhost:3306/database");
        config.addDefault("config.mysql.rankingUpdateDelayInMinutes", 3);
        config.addDefault("config.voting.useVotingSystem", false);
        config.addDefault("config.voting.autoJoinSurvivalGames", true);
        config.addDefault("config.voting.restartAfterGameFinish", false);
        config.addDefault("config.voting.votingTimeOutTimeInSeconds", 180);
        config.addDefault("config.voting.lobbyLocation", "world;5;15;5");
        config.addDefault("config.ingame.money-or-playerpoints-ValuePerWin", 100);
        config.addDefault("config.ingame.money-or-playerpoints-ValuePerKill", 25);
        config.addDefault("config.ingame.rankingEntriesSize", 5);
        config.addDefault("config.ingame.scoreboardUpdateDelayInTicks", 10);
        config.addDefault("config.ingane.playerSearcherItem", "compass");
        config.addDefault("config.ingane.playerSearcherMaxDistanceInBlocks", 250);
        config.addDefault("config.ingane.playerCrateItem", "piston_base");
        config.addDefault("config.ingame.spectatorPlayerSelectorItem", "iron_hoe");
        config.addDefault("config.ingame.spectatorPlayerSponsorItem", "gold_ingot");
        config.addDefault("config.ingame.spectatorMuttationItem", "gold_sword");
        config.addDefault("config.ingame.dropItemsOnBlocKBreak", false);
        config.addDefault("config.ingame.changeChatFormat", true);
        config.addDefault("config.ingame.usedChatFormat", "&a[%points] &6%displayname&c:&f %message");
        config.addDefault("config.ingame.defaultAllowedCommands", Arrays.asList(commands));
        config.addDefault("config.ingame.entitiesRemovedUponMatchState", Arrays.asList(entities));
        config.addDefault("config.ingame.muttation.enabled", true);
        config.addDefault("config.ingame.muttation.money-or-playerpoints-MuttationCost", 75);
        config.addDefault("config.ingame.muttation.money-or-playerpoints-MuttationBonus", 250);
        config.addDefault("config.ingame.muttation.money-or-playerpoints-MuttationReward", 125);
        config.addDefault("config.ingame.tntAutoIgnite.enabled", true);
        config.addDefault("config.ingame.tntAutoIgnite.useFakeExplosionInsteadOfTNT", true);
        config.addDefault("config.ingame.tntAutoIgnite.fakeExplosionPower", 6);
        config.addDefault("config.ingame.tntAutoIgnite.fakeExplosionDelayInSeconds", 4);
        config.addDefault("config.ingame.player.supplyCrate.announceCrate", false);
        config.addDefault("config.ingame.player.supplyCrate.playFireworkEffectWhenFalling", true);
        config.addDefault("config.ingame.player.supplyCrate.crateAnnouncement", "&cCrate will be deployed on &6x: %x&c, &6z: %z&c.");
        config.addDefault("config.ingame.dynamicsigns.UpdateTimeInTicks", 13);
        config.addDefault("config.ingame.dynamicsigns.signLines", Arrays.asList(signlines));
        config.addDefault("config.ingame.autoEquipMaterials.Helmet", Arrays.asList(helmet));
        config.addDefault("config.ingame.autoEquipMaterials.Chestplate", Arrays.asList(chestplate));
        config.addDefault("config.ingame.autoEquipMaterials.Leggings", Arrays.asList(leggings));
        config.addDefault("config.ingame.autoEquipMaterials.Boots", Arrays.asList(boots));
        config.addDefault("config.ingame.spectator.spectatorsInventoryItems.item1", Arrays.asList(item1));
        config.addDefault("config.ingame.spectator.spectatorsInventoryItems.item2", Arrays.asList(item2));
        config.addDefault("config.ingame.spectator.spectatorsInventoryItems.item3", Arrays.asList(item3));
        configY.saveConfig();
    }

    private void generateLang() {
        FileConfiguration lang = langY.getConfig();
        lang.addDefault("lang.prefix", "&c[&6SurvivalGames&c]&a ");
        lang.addDefault("lang.state.waiting", "&aWaiting");
        lang.addDefault("lang.state.pregame", "&bPre-Game");
        lang.addDefault("lang.state.playing", "&6Playing");
        lang.addDefault("lang.state.deathmatch", "&cDeathmatch");
        lang.addDefault("lang.state.disabled", "&7Disabled");
        lang.addDefault("lang.immortalityInXSeconds", "&cImmortality will be over in &4%time seconds&c.");
        lang.addDefault("lang.immortalityIsOver", "&4Immortality is over. &cLet the fight begin!");
        lang.addDefault("lang.gameIsOver", "&cGame is over, no winners though.");
        lang.addDefault("lang.gameIsOverWinner", "&aThe game is over! &e%winner &6is the winner!");
        lang.addDefault("lang.waitingPossibleJoins", "&6Waiting possible joins before starting. &aTry %tries/%maxtries");
        lang.addDefault("lang.tooFarFromCenter", "&cToo far from deathmatch center. &4Go back!");
        lang.addDefault("lang.deathmatchWillStartIn", "&4Deathmatch &cwill start in &6%minutes minutes&c.");
        lang.addDefault("lang.chestRefilled", "&eChests were filled with items.");
        lang.addDefault("lang.notEnoughPlayers", "&cNot enough players to start. &6Needed %needed more players or %minutes minutes.");
        lang.addDefault("lang.startedInXSeconds", "&aThe game &6will start in %time seconds.");
        lang.addDefault("lang.deathmatchIn30Seconds", "&4Deathmatch &cwill start in 30 seconds.");
        lang.addDefault("lang.gameStarted", "&aGame started! &6Good luck and have fun!");
        lang.addDefault("lang.deathmatchStarted", "&4Deathmatch &chas started! &6May the best man win!");
        lang.addDefault("lang.gameIsntWorth", "&cThis game isnt going to your ranking: &4minimum players isnt reached&c.");
        lang.addDefault("lang.gameWontBeWorth", "&cIf not enough players, the game won't save on Rankings.");
        lang.addDefault("lang.youAreUsingKit", "&6You're using kit &a%name&6.");
        lang.addDefault("lang.noPermission", "&4No permission.");
        lang.addDefault("lang.youJoinedTheLobby", "&6You joined the lobby.");
        lang.addDefault("lang.youJoinedTheVotingLobby", "&6You joined the voting lobby. Vote to a map using &e/sg vote");
        lang.addDefault("lang.lobbyIsFull", "&cLobby is full. Try again later.");
        lang.addDefault("lang.gameAlreadyStarted", "&cGame already started.");
        lang.addDefault("lang.alreadyOnLobby", "&cAlready on lobby.");
        lang.addDefault("lang.noSpawnLeft", "&cNo left spawns.");
        lang.addDefault("lang.onCooldown", "&cOn cooldown, wait some time before using it again.");
        lang.addDefault("lang.matchNotFound", "&4Match not found.");
        lang.addDefault("lang.matchNotReady", "&cMatch isnt ready.");
        lang.addDefault("lang.playerNotFound", "&4Player not found.");
        lang.addDefault("lang.youreASpectator", "&6You're now a spectator.");
        lang.addDefault("lang.sponsorPurchased", "&6Sponsor kit was purchased!");
        lang.addDefault("lang.youveBeenSponsored", "&6You have been sponsored by %buyier&6.");
        lang.addDefault("lang.sponsoringCancelled", "&cYour sponsoring has been cancelled.");
        lang.addDefault("lang.cantPurchaseSponsorKit", "&cYou couldn't purchase sponsor kit.");
        lang.addDefault("lang.lobbyConfigurationReseted", "&cYour last lobby selection was lost.");
        lang.addDefault("lang.youWereRemovedFromTheLobby", "&cA VIP joined a full lobby. &4You were kicked for his place.");
        lang.addDefault("lang.dynamicSignRegistered", "&aDynamic sign registered.");
        lang.addDefault("lang.dynamicSignUnregistered", "&cDynamic sign unregistered.");
        lang.addDefault("lang.cantDropCrateHere", "&4Can't drop more supply crates here!");
        lang.addDefault("lang.failedToSponsor", "&cFailed to sponsor.");
        lang.addDefault("lang.muttationSucceeded", "&6Muttation succeeded.");
        lang.addDefault("lang.couldntMuttate", "&4Couldn't muttate.");
        lang.addDefault("lang.youCanOnlyHitYourTarget", "&cYou can only hit your target. (&6%target&c)");
        lang.addDefault("lang.muttationSucceededTargetKilled", "&6Muttation succeeded, you gained bonus points for target killed.");
        lang.addDefault("lang.playerDeath", "&c%displayname &4died. &cThere are &6%alive &cplayers alive.");
        lang.addDefault("lang.reloadedInXms", "&6Plugin reloaded in &c%timems&6.");
        lang.addDefault("lang.youNeedATLEAST2People", "&cLobby needs &4AT LEAST &c2 players to start.");
        lang.addDefault("lang.command.durationDidntStarted", "&4--");
        lang.addDefault("lang.command.listTitle", "&6- Game list -");
        lang.addDefault("lang.command.listEntry", "&cName: &6%displayname &7|&c %alive&6/&c%min&6/&c%max &7|&c %state &7|&c Time: &6%time min.");
        lang.addDefault("lang.command.votingListEntry", "&cName: &6%displayname &7| &cDescription: &6%description");
        lang.addDefault("lang.command.cantBeUsedOnConsole", "&cCan't be used on console.");
        lang.addDefault("lang.command.sponsorIsDisabled", "&cSponsor is disabled.");
        lang.addDefault("lang.command.GUINotGenerated", "&4GUI not generated.");
        lang.addDefault("lang.command.notPlaying", "&cYou arent playing.");
        lang.addDefault("lang.command.youLeftTheLobby", "&cYou left the lobby.");
        lang.addDefault("lang.command.gameEnded", "&cGame ended.");
        lang.addDefault("lang.command.gameStarted", "&6Game started.");
        lang.addDefault("lang.command.rankingList", "&6- Ranking list -");
        lang.addDefault("lang.command.rankingEntry", "&6Name: &c%name &7|&6 K/D: &c%kdr &7|&6 W/L: &c%wlr &7|&6 Points: &a%points");
        lang.addDefault("lang.command.rankingDisabled", "&cRanking is disabled.");
        lang.addDefault("lang.command.votingDisabled", "&cVoting is disabled.");
        lang.addDefault("lang.command.spectateHelp", "&cUse &6/sg list&c to see the match you want to spectate. Then &6/sg spectate (id/name)");
        lang.addDefault("lang.command.sgmHelp", "&cUsage: &6/sgm stop/start (id/name) &cor &6/sgm stop/start &cwhen on a match.");
        lang.addDefault("lang.command.votingHelp", "&cUsage: &6/sg vote (id/name)&c. You can find the ID using &6/sg list&c.");
        lang.addDefault("lang.command.voted", "&6You voted for the map.");
        lang.addDefault("lang.command.cantVoteAgain", "&6You can't vote again.");
        lang.addDefault("lang.command.kickHelp", "&cUsage: &6/sgm kick (player name)");
        lang.addDefault("lang.command.playerIsntPlaying", "&cPlayer isnt playing.");
        lang.addDefault("lang.command.playerKicked", "&cPlayer kicked.");
        lang.addDefault("lang.setup.createdSetup", "&6Setup created! You can start setting variables.");
        lang.addDefault("lang.setup.editedSetup", "&cSetup imported! &6You can start editing variables.");
        lang.addDefault("lang.setup.setupIsntCreated", "&cYou haven't created your setup &6/sgs create (name)&c.");
        lang.addDefault("lang.setup.failedToConfigure", "&4Failed to configure. &cMissing entries: \n&f%missed");
        lang.addDefault("lang.setup.arenaCreated", "&6Arena created!");
        lang.addDefault("lang.setup.arenaLoaded", "&eArena loaded! (;");
        lang.addDefault("lang.setup.failedToLoad", "&cFailed to load.");
        lang.addDefault("lang.setup.failedToSave", "&4Failed to save );");
        lang.addDefault("lang.setup.failedToDelete", "&4Failed to delete );");
        lang.addDefault("lang.setup.arenaDeleted", "&cArena deleted.");
        lang.addDefault("lang.setup.successful.string", "&6String set");
        lang.addDefault("lang.setup.successful.stringCleared", "&6String cleared");
        lang.addDefault("lang.setup.successful.multiArgsString", "&6Text set");
        lang.addDefault("lang.setup.successful.material", "&6Material set");
        lang.addDefault("lang.setup.successful.materialCleared", "&6Material cleared");
        lang.addDefault("lang.setup.successful.boolean", "&6Boolean set");
        lang.addDefault("lang.setup.successful.integer", "&6Integer set");
        lang.addDefault("lang.setup.successful.location", "&6Location set");
        lang.addDefault("lang.setup.successful.locationCleared", "&6Locations cleared");
        lang.addDefault("lang.setup.error.invalidInteger", "&cInvalid integer.");
        lang.addDefault("lang.setup.error.invalidItemInHand", "&cInvalid item in hand.");
        lang.addDefault("lang.setup.error.invalidChestLocation", "&cChest not found on this location.");
        lang.addDefault("lang.setup.error.invalidChestLocationOrTierId", "&cChest not found on this location or invalid tier.");
        lang.addDefault("lang.setup.error.notEnoughSpawns", "&cNot enough spawns&f");
        lang.addDefault("lang.setup.error.displayNameNotSet", "&cDisplay name&f");
        lang.addDefault("lang.setup.error.minPlayerNotSet", "&eMin. player quantity&f");
        lang.addDefault("lang.setup.error.maxPlayerNotSet", "&eMax. player quantity&f");
        lang.addDefault("lang.setup.error.maxDurationNotSet", "&eMax duration&f");
        lang.addDefault("lang.setup.error.corner1NotSet", "&cFirst corner&f");
        lang.addDefault("lang.setup.error.corner2NotSet", "&cSecond corner&f");
        lang.addDefault("lang.setup.error.spectatorNotSet", "&cSpectator spawn&f");
        lang.addDefault("lang.setup.error.safeLocationNotSet", "&cSafe location&f");
        lang.addDefault("lang.setup.error.exitLocationNotSet", "&cExit location&f");
        lang.addDefault("lang.setup.error.chestDelayNotSet", "&eChest delay&f");
        lang.addDefault("lang.setup.error.votingDescriptionNotSet", "&aVoting description&f");
        lang.addDefault("lang.setup.error.deathmatchDurationNotSet", "&eDeathmatch duration&f");
        lang.addDefault("lang.setup.error.deathmatchDistanceRadiusNotSet", "&eDeathmatch kill radius threshold&f");
        lang.addDefault("lang.setup.error.deathmatchCenterNotSet", "&cDeathmatch center&f");
        lang.addDefault("lang.setup.error.deathmatchSpawnNotSet", "&cDeathmatch spawns&f");
        lang.addDefault("lang.barapi.waitingPossibleJoins", "&cWaiting possible joins. &6Trying again in...");
        lang.addDefault("lang.barapi.startingInXSeconds", "&cStarting game in...");
        lang.addDefault("lang.barapi.immortalityOverInXSeconds", "&cImmortality time will be over in...");
        lang.addDefault("lang.barapi.deathmatchStartingInX", "&4Deathmatch &cstarting in...");
        lang.addDefault("lang.barapi.gameEndingIn1Minute", "&cThe game will end in...");
        langPrefix = lang.getString("lang.prefix").replaceAll("&", "§");
        langY.saveConfig();
    }

    private void generateLobbies() {
        FileConfiguration lobbies = lobbiesY.getConfig(), config = configY.getConfig();
        if(config.getBoolean("config.generateExampleLobbiesConfiguration")) {
            String[] commands = {"tell"},
                    spawns = {"1/world;5;4;5", "1/world;5;4;-5", "1/world;-5;4;5", "1/world;-5;4;-5"},
                    description = {"&cNice arena!", "&4With deathmatch"},
                    blocked = {"FLINT_AND_STEEL", "MAP"},
                    blocks = {"LEAVES", "LONG_GRASS", "DEAD_BUSH", "BROWN_MUSHROOM", "RED_MUSHROOM"};
            if(lobbies.getConfigurationSection("lobbies.arenaTEST") == null || !lobbies.contains("lobbies.arenaTEST")) {
                lobbies.createSection("lobbies.arenaTEST");
            }
            lobbies.addDefault("lobbies.arenaTEST.minPlayers", 4);
            lobbies.addDefault("lobbies.arenaTEST.maxPlayers", 16);
            lobbies.addDefault("lobbies.arenaTEST.playerCountToLightningEverybody", 3);
            lobbies.addDefault("lobbies.arenaTEST.chestRefreshDelayInSeconds", 600);
            lobbies.addDefault("lobbies.arenaTEST.maxMatchDurationInMinutes", 60);
            lobbies.addDefault("lobbies.arenaTEST.remainingDurationToStartDeathmatch", 5);
            lobbies.addDefault("lobbies.arenaTEST.deathmatchEscapeDistanceInBlocks", 100);
            lobbies.addDefault("lobbies.arenaTEST.immortalityTimeAfterMatchStartInSeconds", 45);
            lobbies.addDefault("lobbies.arenaTEST.waitTimeUntilMatchStartInSeconds", 30);
            lobbies.addDefault("lobbies.arenaTEST.numberOfTriesWithEnoughPlayers", 2);
            lobbies.addDefault("lobbies.arenaTEST.numberOfTriesWithoutEnoughPlayers", 3);
            lobbies.addDefault("lobbies.arenaTEST.delayOfEachTry", 60);
            lobbies.addDefault("lobbies.arenaTEST.votingMapDescription", "&cNice map! Vote me!");
            lobbies.addDefault("lobbies.arenaTEST.displayName", "&cNiceMap");
            lobbies.addDefault("lobbies.arenaTEST.deathmatchCenterToDistanceLocation", "world;5;4;5");
            lobbies.addDefault("lobbies.arenaTEST.kickNonVipPlayersForAVipJoin", false);
            lobbies.addDefault("lobbies.arenaTEST.searchBlocksForChests", true);
            lobbies.addDefault("lobbies.arenaTEST.enableDeathmatch", true);
            lobbies.addDefault("lobbies.arenaTEST.allowedCommands", Arrays.asList(commands));
            lobbies.addDefault("lobbies.arenaTEST.additionalDescription", Arrays.asList(description));
            lobbies.addDefault("lobbies.arenaTEST.firstCornerLocation", "world;10;4;-10");
            lobbies.addDefault("lobbies.arenaTEST.secondCornerLocation", "world;-10;4;10");
            lobbies.addDefault("lobbies.arenaTEST.spectatorSpawnLocation", "world;5;16;5");
            lobbies.addDefault("lobbies.arenaTEST.lobbyLocation", "world;15;16;15");
            lobbies.addDefault("lobbies.arenaTEST.exitLocation", "world;30;16;30");
            lobbies.addDefault("lobbies.arenaTEST.itemsBlockedFromCrafting", Arrays.asList(blocked));
            lobbies.addDefault("lobbies.arenaTEST.blocksAllowedToPlace", Arrays.asList(blocks));
            lobbies.addDefault("lobbies.arenaTEST.blocksAllowedToBreak", Arrays.asList(blocks));
            lobbies.addDefault("lobbies.arenaTEST.savedChestLocations", Arrays.asList(spawns));
            lobbies.addDefault("lobbies.arenaTEST.spawnLocations", Arrays.asList(spawns));
            lobbies.addDefault("lobbies.arenaTEST.deathmatchLocations", Arrays.asList(spawns));
            config.set("config.generateExampleLobbiesConfiguration", false);
            configY.saveConfig();
        } else {
            lobbies.addDefault("lobbies", Arrays.asList(empty));
        }
        lobbies.addDefault("dynamic-signs", Arrays.asList(empty));
        lobbiesY.saveConfig();
    }

    public void generateItems() {
        FileConfiguration items = itemsY.getConfig(), config = configY.getConfig();
        if(config.getBoolean("config.generateExampleItemConfiguration")) {
            String[] item1 = {"material:Apple", "quantity:3", "chance:0.5"},
                    shown1 = {"material:chest", "quantity:1", "lore:&cPercentage of points: &620%", "lore:&4Low quality items"},
                    potioneffect1 = {"type:invisibility", "duration:30", "amplifier:1", "chance:0.5"},
                    shown2 = {"material:chest", "quantity:1", "lore:&cKit #1, best kit evar"},
                    item2 = {"material:diamond_chestplate", "quantity:1", "unsafeenchantment:PROTECTION_ENVIRONMENTAL;666"},
                    permissions = {"survivalgames.permissionexample"};
        /*
         CHEST TIER
         */
            items.addDefault("chest-tiers.tier1.itemsPerChest", 3);
            items.addDefault("chest-tiers.tier1.randomItemAmount", true);
            items.addDefault("chest-tiers.tier1.isDefaultTier", true);
            items.addDefault("chest-tiers.tier1.isSupplyCrateTier", true);
            items.addDefault("chest-tiers.tier1.items.item1", Arrays.asList(item1));
        /*
         SPONSOR KIT
         */
            items.addDefault("sponsor.enabled", true);
            items.addDefault("sponsor-kits.sponsor1.displayName", "&cSponsor1");
            items.addDefault("sponsor-kits.sponsor1.randomItemAmount", true);
            items.addDefault("sponsor-kits.sponsor1.randomPotionEffectAmplifier", true);
            items.addDefault("sponsor-kits.sponsor1.buyierGainPercentage", 20);
            items.addDefault("sponsor-kits.sponsor1.priceToBuySponsoring", "points;30");
            items.addDefault("sponsor-kits.sponsor1.itemShownOnList", Arrays.asList(shown1));
            items.addDefault("sponsor-kits.sponsor1.items.item1", Arrays.asList(item1));
            items.addDefault("sponsor-kits.sponsor1.potionEffects.effect1", Arrays.asList(potioneffect1));
        /*
         GAME KITS
         */
            items.addDefault("game-kits.kit1.displayName", "&aKit#1");
            items.addDefault("game-kits.kit1.permissionRequiredToUse", "null");
            items.addDefault("game-kits.kit1.isDefaultKit", true);
            items.addDefault("game-kits.kit1.isMuttationKit", true);
            items.addDefault("game-kits.kit1.itemShownOnMenu", Arrays.asList(shown2));
            items.addDefault("game-kits.kit1.items.item1", Arrays.asList(item1));
            items.addDefault("game-kits.kit1.items.item2", Arrays.asList(item2));
            items.addDefault("game-kits.kit1.potionEffects.effect1", Arrays.asList(potioneffect1));
            items.addDefault("game-kits.kit1.potionEffectsOnKill", Arrays.asList(empty));
            items.addDefault("game-kits.kit1.permissionsToGiveOnStart", Arrays.asList(permissions));
            config.set("config.generateExampleItemConfiguration", false);
        } else {
            items.addDefault("sponsor.enabled", false);
            if(items.getConfigurationSection("chest-tiers") == null) {
                items.createSection("chest-tiers");
            }
            if(items.getConfigurationSection("game-kits") == null) {
                items.createSection("game-kits");
            }
            if(items.getConfigurationSection("sponsor-kits") == null) {
                items.createSection("sponsor-kits");
            }
        }
        itemsY.saveConfig();
        configY.saveConfig();
    }

    private void generateGUIConfiguration() {
        FileConfiguration gui = guiY.getConfig();
        String[] disabledPageString = {"material:wool", "damage:8", "displayname:&cPage isnt avaliable"},
                nextPageString = {"material:wool", "damage:9", "displayname:&aNext page"},
                backPageString = {"material:wool", "damage:11", "displayname:&aBack page"},
                closePageString = {"material:wool", "damage:15", "displayname:&cClose page"},
                spectatorVotingString = {"material:EYE_OF_ENDER", "displayname:&6Spectate game"},
                WAITING = {"material:wool", "lore:&aWaiting", "damage:5"},
                PREGAME = {"material:wool", "lore:&6Pre-game", "damage:4"},
                PLAYING = {"material:wool", "lore:&cPlaying", "damage:1"},
                DEATHMATCH = {"material:wool", "lore:&4Deathmatch", "damage:14"},
                DISABLED = {"material:wool", "lore:&7Disabled", "damage:7"};
        gui.addDefault("config.joinInventoryTitle", "&cChoose an arena");
        gui.addDefault("config.kitInventoryTitle", "&cChoose a kit");
        gui.addDefault("config.playerListInventoryTitle", "&cChoose a player");
        gui.addDefault("config.sponsorListInventoryTitle", "&cChoose a sponsor kit");
        gui.addDefault("config.crateInventoryTitle", "&cSupply crate");
        gui.addDefault("config.updateDelayInSeconds", 1);
        gui.addDefault("items.disabledPageItem", Arrays.asList(disabledPageString));
        gui.addDefault("items.nextPageItem", Arrays.asList(nextPageString));
        gui.addDefault("items.backPageItem", Arrays.asList(backPageString));
        gui.addDefault("items.closeInventoryItem", Arrays.asList(closePageString));
        gui.addDefault("items.spectatorVotingItem", Arrays.asList(spectatorVotingString));
        gui.addDefault("items.state." + State.WAITING.name(), Arrays.asList(WAITING));
        gui.addDefault("items.state." + State.PREGAME.name(), Arrays.asList(PREGAME));
        gui.addDefault("items.state." + State.PLAYING.name(), Arrays.asList(PLAYING));
        gui.addDefault("items.state." + State.DEATHMATCH.name(), Arrays.asList(DEATHMATCH));
        gui.addDefault("items.state." + State.DISABLED.name(), Arrays.asList(DISABLED));
        guiY.saveConfig();
    }

    private void loadConfig() {
        clearVariables();
        FileConfiguration config = configY.getConfig(), lobbies = lobbiesY.getConfig(), items = itemsY.getConfig(), gui = guiY.getConfig();
        useVault = config.getBoolean("config.supportVault");
        usePlayerPoints = config.getBoolean("config.supportPlayerPoints");
        useMySQL = config.getBoolean("config.mysql.enabled");
        useVoting = config.getBoolean("config.voting.useVotingSystem");
        useAutojoin = config.getBoolean("config.voting.autoJoinSurvivalGames");
        valuePerWin = config.getInt("config.ingame.money-or-playerpoints-ValuePerWin");
        valuePerKill = config.getInt("config.ingame.money-or-playerpoints-ValuePerKill");
        signDelay = config.getInt("config.ingame.dynamicsigns.UpdateTimeInTicks");
        maxDistance = config.getInt("config.ingane.playerSearcherMaxDistanceInBlocks");
        scoreboardDelay = config.getInt("config.ingame.scoreboardUpdateDelayInTicks");
        useBarAPI = config.getBoolean("config.supportBarAPI");
        spectatorPlayerSelector = Material.valueOf(config.getString("config.ingame.spectatorPlayerSelectorItem").toUpperCase());
        spectatorPlayerSponsor = Material.valueOf(config.getString("config.ingame.spectatorPlayerSponsorItem").toUpperCase());
        spectatorMuttation = Material.valueOf(config.getString("config.ingame.spectatorMuttationItem").toUpperCase());
        playerSearcher = Material.valueOf(config.getString("config.ingane.playerSearcherItem").toUpperCase());
        playerCrate = Material.valueOf(config.getString("config.ingane.playerCrateItem").toUpperCase());
        useChat = config.getBoolean("config.ingame.changeChatFormat");
        chatFormat = config.getString("config.ingame.usedChatFormat").replaceAll("&", "§");
        blockDropItem = config.getBoolean("config.ingame.dropItemsOnBlocKBreak");
        announceCrateDeploy = config.getBoolean("config.ingame.player.supplyCrate.announceCrate");
        crateAnnouncement = config.getString("config.ingame.player.supplyCrate.crateAnnouncement").replaceAll("&", "§");
        playCrateFirework = config.getBoolean("config.ingame.player.supplyCrate.playFireworkEffectWhenFalling");
        useMuttation = config.getBoolean("config.ingame.muttation.enabled");
        muttationCost = config.getInt("config.ingame.muttation.money-or-playerpoints-MuttationCost");
        muttationBonus = config.getInt("config.ingame.muttation.money-or-playerpoints-MuttationBonus");
        muttationReward = config.getInt("config.ingame.muttation.money-or-playerpoints-MuttationReward");
        tntAutoIgnite = config.getBoolean("config.ingame.tntAutoIgnite.enabled");
        tntUseFakeExplosion = config.getBoolean("config.ingame.tntAutoIgnite.useFakeExplosionInsteadOfTNT");
        tntPower = config.getInt("config.ingame.tntAutoIgnite.fakeExplosionPower");
        tntDelay = config.getInt("config.ingame.tntAutoIgnite.fakeExplosionDelayInSeconds");
        rankingEntries = config.getInt("config.ingame.rankingEntriesSize");
        guiUpdateDelay = gui.getInt("config.updateDelayInSeconds");
        useSponsor = items.getBoolean("sponsor.enabled");
        waitingString = pl.getLang("state.waiting");
        pregameString = pl.getLang("state.pregame");
        playingString = pl.getLang("state.playing");
        deathmatchString = pl.getLang("state.deathmatch");
        disabledString = pl.getLang("state.disabled");
        disabledPage = resolveItemStack(gui.getStringList("items.disabledPageItem"));
        nextPage = resolveItemStack(gui.getStringList("items.nextPageItem"));
        backPage = resolveItemStack(gui.getStringList("items.backPageItem"));
        closePage = resolveItemStack(gui.getStringList("items.closeInventoryItem"));
        noKitItem = resolveItemStack(gui.getStringList("items.spectatorVotingItem"));
        crateInventoryTitle = gui.getString("config.crateInventoryTitle").replaceAll("&", "§");
        for(String s : config.getStringList("config.ingame.entitiesRemovedUponMatchState")) {
            entitiesToRemove.add(EntityType.valueOf(s.toUpperCase()));
        }
        if(useVault && !setupVault()) {
            pl.getLogger().log(Level.WARNING, "Failed to support Vault OR support is disabled.");
            useVault = false;
        }
        if(usePlayerPoints && !setupPlayerPoints()) {
            pl.getLogger().log(Level.WARNING, "Failed to support PlayerPoints OR support is disabled.");
            usePlayerPoints = false;
        }
        if(useMySQL) {
            sql = new MySQL(pl, config.getString("config.mysql.username"), config.getString("config.mysql.password"), config.getString("config.mysql.url"), config.getInt("config.mysql.rankingUpdateDelayInMinutes"));
            if(!sql.createTable()) {
                pl.getLogger().log(Level.WARNING, "Failed to connect to MySQL, disabling it.");
                useMySQL = false;
            } else {
                sql.generateRanking();
            }
        }
        if(useVoting) {
            int timeout = config.getInt("config.voting.votingTimeOutTimeInSeconds");
            boolean restart = config.getBoolean("config.voting.restartAfterGameFinish");
            Location location = resolveLocation(config.getString("config.voting.lobbyLocation"));
            pl.voting = new Voting(pl, timeout, restart, location);
        }
        for(String command : config.getStringList("config.ingame.defaultAllowedCommands")) {
            defaultAllowedCommands.add(command.replaceAll("/", "").toLowerCase());
        }
        for(String material : config.getStringList("config.ingame.autoEquipMaterials.Helmet")) {
            autoequipHelmet.add(Material.valueOf(material.toUpperCase()));
        }
        for(String material : config.getStringList("config.ingame.autoEquipMaterials.Chestplate")) {
            autoequipChestplate.add(Material.valueOf(material.toUpperCase()));
        }
        for(String material : config.getStringList("config.ingame.autoEquipMaterials.Leggings")) {
            autoequipLeggings.add(Material.valueOf(material.toUpperCase()));
        }
        for(String material : config.getStringList("config.ingame.autoEquipMaterials.Boots")) {
            autoequipBoots.add(Material.valueOf(material.toUpperCase()));
        }
        if(config.getConfigurationSection("config.ingame.spectator.spectatorsInventoryItems") != null) {
            for(String path : config.getConfigurationSection("config.ingame.spectator.spectatorsInventoryItems").getKeys(false)) {
                spectatorInventory.add(resolveItemStack(config.getStringList("config.ingame.spectator.spectatorsInventoryItems." + path)));
            }
        }
        for(String path : items.getConfigurationSection("chest-tiers").getKeys(false)) {
            if(!loadTier(path)) {
                pl.getLogger().log(Level.WARNING, "Failed to load chest tier: " + path);
            }
        }
        if(defaultTier == null) {
            pl.getLogger().log(Level.WARNING, "Default tier is null. You won't see anything in chest.");
        }
        if(crateTier == null) {
            pl.getLogger().log(Level.WARNING, "Crate tier is null. You won't see anything in chest.");
        }
        if(lobbies.getConfigurationSection("lobbies") != null) {
            for(String path : lobbies.getConfigurationSection("lobbies").getKeys(false)) {
                if(!loadLobby(path, false)) {
                    pl.getLogger().log(Level.WARNING, "Failed to load arena: " + path);
                }
            }
        }
        if(pl.lobbies.size() == 0) {
            pl.getLogger().log(Level.WARNING, "No lobbies loaded, you don't have anything to join.");
        }
        if(useSponsor) {
            for(String path : items.getConfigurationSection("sponsor-kits").getKeys(false)) {
                if(!loadSponsor(path)) {
                    pl.getLogger().log(Level.WARNING, "Failed to load sponsor kit: " + path);
                }
            }
        }
        for(String path : items.getConfigurationSection("game-kits").getKeys(false)) {
            if(!loadKit(path)) {
                pl.getLogger().log(Level.WARNING, "Failed to load kit: " + path);
            }
        }
        if(defaultKit == null) {
            pl.getLogger().log(Level.WARNING, "Default kit is null. You will probably receive errors.");
        }
        if(muttationKit == null && useMuttation) {
            pl.getLogger().log(Level.WARNING, "Muttation kit is null. You will probably receive errors.");
        }
        if(lobbies.getConfigurationSection("dynamic-signs") != null) {
            for(String path : lobbies.getConfigurationSection("dynamic-signs").getKeys(false)) {
                for(String location : lobbies.getStringList("dynamic-signs." + path)) {
                    Location loc = resolveLocation(location);
                    if(loc.getBlock().getState() instanceof Sign) {
                        pl.dynamicSigns.get(pl.getLobby(path)).add((Sign) loc.getBlock().getState());
                    }
                }
            }
        }
        int i = 0;
        for(String line : config.getStringList("config.ingame.dynamicsigns.signLines")) {
            signLines[i] = line.replaceAll("&", "§");
            i++;
            if(i > 3) {
                break;
            }
        }
    }

    public boolean loadLobby(String path, boolean recreateGUI) {
        FileConfiguration lobbies = lobbiesY.getConfig();
        int min = lobbies.getInt("lobbies." + path + ".minPlayers"),
                max = lobbies.getInt("lobbies." + path + ".maxPlayers"),
                maxDuration = lobbies.getInt("lobbies." + path + ".maxMatchDurationInMinutes"),
                chestdelay = lobbies.getInt("lobbies." + path + ".chestRefreshDelayInSeconds"),
                lightning = lobbies.getInt("lobbies." + path + ".playerCountToLightningEverybody"),
                grace = lobbies.getInt("lobbies." + path + ".immortalityTimeAfterMatchStartInSeconds", 45),
                wait = lobbies.getInt("lobbies." + path + ".waitTimeUntilMatchStartInSeconds", 30),
                warnedmaior = lobbies.getInt("lobbies." + path + ".numberOfTriesWithEnoughPlayers", 2),
                warnedmenor = lobbies.getInt("lobbies." + path + ".numberOfTriesWithoutEnoughPlayers", 3),
                possiblejoinsdelay = lobbies.getInt("lobbies." + path + ".delayOfEachTry", 30);
        Location corner1 = resolveLocation(lobbies.getString("lobbies." + path + ".firstCornerLocation")),
                corner2 = resolveLocation(lobbies.getString("lobbies." + path + ".secondCornerLocation")),
                spectator = resolveLocation(lobbies.getString("lobbies." + path + ".spectatorSpawnLocation")),
                lobbylocation = resolveLocation(lobbies.getString("lobbies." + path + ".lobbyLocation")),
                exitlocation = resolveLocation(lobbies.getString("lobbies." + path + ".exitLocation"));
        boolean search = lobbies.getBoolean("lobbies." + path + ".searchBlocksForChests"),
                usedm = lobbies.getBoolean("lobbies." + path + ".enableDeathmatch"),
                kicknonvips = lobbies.getBoolean("lobbies." + path + ".kickNonVipPlayersForAVipJoin");
        ArrayList<Material> blockedCrafting = new ArrayList<Material>(), allowedPlace = new ArrayList<Material>(), allowedBreak = new ArrayList<Material>();
        for(String s : lobbies.getStringList("lobbies." + path + ".itemsBlockedFromCrafting")) {
            blockedCrafting.add(Material.valueOf(s.toUpperCase()));
        }
        for(String s : lobbies.getStringList("lobbies." + path + ".blocksAllowedToPlace")) {
            allowedPlace.add(Material.valueOf(s.toUpperCase()));
        }
        for(String s : lobbies.getStringList("lobbies." + path + ".blocksAllowedToBreak")) {
            allowedBreak.add(Material.valueOf(s.toUpperCase()));
        }
        HashMap<Location, String> chestLocation = new HashMap<Location, String>();
        for(String s : lobbies.getStringList("lobbies." + path + ".savedChestLocations")) {
            String[] st = s.split("/");
            chestLocation.put(resolveLocation(st[1]), st[0].toLowerCase());
        }
        ArrayList<Location> spawnLocation = new ArrayList<Location>();
        for(String s : lobbies.getStringList("lobbies." + path + ".spawnLocations")) {
            spawnLocation.add(resolveLocation(s));
        }
        ArrayList<String> lores = new ArrayList<String>(), commands = new ArrayList<String>(defaultAllowedCommands);
        for(String lore : lobbies.getStringList("lobbies." + path + ".additionalDescription")) {
            lores.add(lore.replaceAll("&", "§"));
        }
        for(String command : lobbies.getStringList("lobbies." + path + ".allowedCommands")) {
            commands.add(command.replaceAll("/", "").toLowerCase());
        }
        String description = "none", displayname = lobbies.getString("lobbies." + path + ".displayName").replaceAll("&", "§");
        if(useVoting) {
            description = lobbies.getString("lobbies." + path + ".votingMapDescription").replaceAll("&", "§");
        }
        Arena arena;
        int dmDuration = -1, dmDistance = -1;
        if(usedm) {
            dmDuration = lobbies.getInt("lobbies." + path + ".remainingDurationToStartDeathmatch");
            dmDistance = lobbies.getInt("lobbies." + path + ".deathmatchEscapeDistanceInBlocks");
            Location center = resolveLocation(lobbies.getString("lobbies." + path + ".deathmatchCenterToDistanceLocation"));
            ArrayList<Location> dmspawnLocation = new ArrayList<Location>();
            for(String s : lobbies.getStringList("lobbies." + path + ".deathmatchLocations")) {
                dmspawnLocation.add(resolveLocation(s));
            }
            arena = new Arena(pl,
                    search, true,
                    chestLocation,
                    allowedPlace, allowedBreak, blockedCrafting,
                    spawnLocation, dmspawnLocation,
                    spectator, center, corner1, corner2, lobbylocation, exitlocation);
        } else {
            arena = new Arena(pl,
                    search, false,
                    chestLocation,
                    allowedPlace, allowedBreak, blockedCrafting,
                    spawnLocation,
                    spectator, corner1, corner2, lobbylocation, exitlocation);
        }
        Lobby lobby = new Lobby(pl, path.toLowerCase(), displayname, description, lores, commands, min, max, maxDuration, dmDuration, dmDistance, lightning,
                chestdelay, grace, wait, warnedmaior, warnedmenor, possiblejoinsdelay, kicknonvips, arena);
        pl.lobbies.put(path.toLowerCase(), lobby);
        pl.dynamicSigns.put(lobby, new ArrayList<Sign>());
        if(recreateGUI) {
            generateJoinPages();
        }
        return true;
    }

    private boolean loadTier(String path) {
        FileConfiguration items = itemsY.getConfig();
        int itemperchest = items.getInt("chest-tiers." + path + ".itemsPerChest");
        boolean random = items.getBoolean("chest-tiers." + path + ".randomItemAmount");
        HashMap<ItemStack, Double> item = new HashMap<ItemStack, Double>();
        if(items.getConfigurationSection("chest-tiers." + path + ".items") != null) {
            for(String key : items.getConfigurationSection("chest-tiers." + path + ".items").getKeys(false)) {
                item.put(resolveItemStack(items.getStringList("chest-tiers." + path + ".items." + key)), resolveStringChance(items.getStringList("chest-tiers." + path + ".items." + key)));
            }
        }
        Tier tier = new Tier(pl, path.toLowerCase(), itemperchest, random, item);
        if(items.getBoolean("chest-tiers." + path + ".isDefaultTier")) {
            defaultTier = tier;
        }
        if(items.getBoolean("chest-tiers." + path + ".isSupplyCrateTier")) {
            crateTier = tier;
        }
        pl.chestTiers.put(path.toLowerCase(), tier);
        return true;
    }

    private boolean loadSponsor(String path) {
        FileConfiguration items = itemsY.getConfig();
        boolean randomquantity = items.getBoolean("sponsor-kits." + path + ".randomItemAmount");
        boolean randomamplifier = items.getBoolean("sponsor-kits." + path + ".randomPotionEffectAmplifier");
        double percentage = (items.getDouble("sponsor-kits." + path + ".buyierGainPercentage") / 100);
        String cost = items.getString("sponsor-kits." + path + ".priceToBuySponsoring"),
                name = items.getString("sponsor-kits." + path + ".displayName").replaceAll(" ", "").replaceAll("&", "§");
        ItemStack itemstack = resolveItemStack(items.getStringList("sponsor-kits." + path + ".itemShownOnList"));
        HashMap<ItemStack, Double> item = new HashMap<ItemStack, Double>();
        HashMap<PotionEffect, Double> potion = new HashMap<PotionEffect, Double>();
        if(items.getConfigurationSection("sponsor-kits." + path + ".items") != null) {
            for(String key : items.getConfigurationSection("sponsor-kits." + path + ".items").getKeys(false)) {
                item.put(resolveItemStack(items.getStringList("sponsor-kits." + path + ".items." + key)), resolveStringChance(items.getStringList("sponsor-kits." + path + ".items." + key)));
            }
        }
        if(items.getConfigurationSection("sponsor-kits." + path + ".potionEffects") != null) {
            for(String key : items.getConfigurationSection("sponsor-kits." + path + ".potionEffects").getKeys(false)) {
                potion.put(resolvePotionEffect(items.getStringList("sponsor-kits." + path + ".potionEffects." + key)), resolveStringChance(items.getStringList("sponsor-kits." + path + ".potionEffects." + key)));
            }
        }
        SponsorKit sponsor = new SponsorKit(pl, path.toLowerCase(), name, 1, percentage, randomquantity, randomamplifier, itemstack, cost, item, potion);
        pl.sponsorKits.put(path.toLowerCase(), sponsor);
        return true;
    }

    private boolean loadKit(String path) {
        FileConfiguration items = itemsY.getConfig();
        String name = items.getString("game-kits." + path + ".displayName").replaceAll("&", "§").replaceAll(" ", "");
        String permission = items.getString("game-kits." + path + ".permissionRequiredToUse");
        ItemStack shown = resolveItemStack(items.getStringList("game-kits." + path + ".itemShownOnMenu"));
        ArrayList<ItemStack> item = new ArrayList<ItemStack>();
        if(items.getConfigurationSection("game-kits." + path + ".items") != null) {
            for(String key : items.getConfigurationSection("game-kits." + path + ".items").getKeys(false)) {
                item.add(resolveItemStack(items.getStringList("game-kits." + path + ".items." + key)));
            }
        }
        ArrayList<PotionEffect> potioneffects = new ArrayList<PotionEffect>(), potioneffectskill = new ArrayList<PotionEffect>();
        if(items.getConfigurationSection("game-kits." + path + ".potionEffects") != null) {
            for(String key : items.getConfigurationSection("game-kits." + path + ".potionEffects").getKeys(false)) {
                potioneffects.add(resolvePotionEffect(items.getStringList("game-kits." + path + ".potionEffects." + key)));
            }
        }
        if(items.getConfigurationSection("game-kits." + path + ".potionEffectsOnKill") != null) {
            for(String key : items.getConfigurationSection("game-kits." + path + ".potionEffectsOnKill").getKeys(false)) {
                potioneffects.add(resolvePotionEffect(items.getStringList("game-kits." + path + ".potionEffectsOnKill." + key)));
            }
        }
        ArrayList<String> permissionsgiven = new ArrayList<String>();
        for(String string : items.getStringList("game-kits." + path + ".permissionsToGiveOnStart")) {
            permissionsgiven.add(string.toLowerCase());
        }
        Kit kit = new Kit(path.toLowerCase(), name, permission, permissionsgiven, item, shown, potioneffects, potioneffectskill);
        if(items.getBoolean("game-kits." + path + ".isDefaultKit")) {
            defaultKit = kit;
        }
        if(items.getBoolean("game-kits." + path + ".isMuttationKit")) {
            muttationKit = kit;
        }
        pl.gameKits.put(path.toLowerCase(), kit);
        return true;
    }

    private boolean setupVault() {
        RegisteredServiceProvider<Economy> economyProvider = pl.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        RegisteredServiceProvider<Permission> permissionProvider = pl.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if(economyProvider != null && permissionProvider != null) {
            econ = economyProvider.getProvider();
            perm = permissionProvider.getProvider();
        }
        return (perm != null) && (econ != null);
    }

    private boolean setupPlayerPoints() {
        final Plugin plugin = pl.getServer().getPluginManager().getPlugin("PlayerPoints");
        playerp = PlayerPoints.class.cast(plugin);
        return playerp != null;
    }

    public Location resolveLocation(String loc) {
        String[] l = loc.split(";");
        World w = pl.getServer().getWorld(l[0]);
        if(w != null) {
            if(l.length > 4) {
                return new Location(w, Double.parseDouble(l[1]), Double.parseDouble(l[2]), Double.parseDouble(l[3]), Float.parseFloat(l[4]), Float.parseFloat(l[5]));
            } else {
                return new Location(w, Double.parseDouble(l[1]), Double.parseDouble(l[2]), Double.parseDouble(l[3]));
            }
        } else {
            return null;
        }
    }

    public String locationToString(Location loc) {
        if(loc != null) {
            float x = new BigDecimal(loc.getX()).setScale(2, RoundingMode.HALF_UP).floatValue(),
                    y = new BigDecimal(loc.getY()).setScale(2, RoundingMode.HALF_UP).floatValue(),
                    z = new BigDecimal(loc.getZ()).setScale(2, RoundingMode.HALF_UP).floatValue(),
                    yaw = new BigDecimal(loc.getYaw()).setScale(2, RoundingMode.HALF_UP).floatValue(),
                    pitch = new BigDecimal(loc.getPitch()).setScale(2, RoundingMode.HALF_UP).floatValue();
            return loc.getWorld().getName() + ";" + x + ";" + y + ";" + z + ";" + yaw + ";" + pitch;
        } else {
            return "";
        }
    }

    public int[] getStats(String name) {
        if(useMySQL) {
            return sql.getStats(name);
        } else {
            return new int[]{0, 0, 0};
        }
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public ItemStack resolveItemStack(List<String> list) {
        Material material = Material.REDSTONE;
        int quantity = 1, power = 3;
        short damage = 0;
        Color color = Color.BLACK;
        HashMap<Enchantment, Integer> enchantments = new HashMap<Enchantment, Integer>(), unsafeenchantments = new HashMap<Enchantment, Integer>();
        String displayname = "", skullName = "Jaby", bookTitle = "", bookAuthor = "Jaby";
        ArrayList<String> lore = new ArrayList<String>(), bookPages = new ArrayList<String>();
        ArrayList<FireworkEffect> fireworkEffects = new ArrayList<FireworkEffect>();

        for(String properties : list) {
            String[] property = properties.split(":", 2);
            try {
                if(property[0].equalsIgnoreCase("material")) {
                    material = Material.valueOf(property[1].toUpperCase());
                } else if(property[0].equalsIgnoreCase("id")) {
                    for(Material mat : Material.values()) {
                        if(mat.getId() == Integer.parseInt(property[1])) {
                            material = mat;
                        }
                    }
                } else if(property[0].equalsIgnoreCase("quantity")) {
                    quantity = Integer.parseInt(property[1]);
                } else if(property[0].equalsIgnoreCase("damage")) {
                    damage = Short.parseShort(property[1]);
                } else if(property[0].equalsIgnoreCase("displayname")) {
                    displayname = property[1].replaceAll("&", "§");
                } else if(property[0].equalsIgnoreCase("lore")) {
                    lore.add(property[1].replaceAll("&", "§"));
                } else if(property[0].equalsIgnoreCase("enchantment")) {
                    String[] enchantmentProperty = property[1].split(";");
                    Enchantment en = Enchantment.getByName(enchantmentProperty[0].toUpperCase());
                    if(en != null) {
                        enchantments.put(en, Integer.parseInt(enchantmentProperty[1]));
                    } else {
                        pl.getLogger().log(Level.WARNING, "Enchantment not found: " + enchantmentProperty[0]);
                    }
                } else if(property[0].equalsIgnoreCase("unsafeEnchantment")) {
                    String[] enchantmentProperty = property[1].split(";");
                    Enchantment en = Enchantment.getByName(enchantmentProperty[0].toUpperCase());
                    if(en != null) {
                        unsafeenchantments.put(en, Integer.parseInt(enchantmentProperty[1]));
                    } else {
                        pl.getLogger().log(Level.WARNING, "Enchantment not found: " + enchantmentProperty[0]);
                    }

                } else if(property[0].equalsIgnoreCase("fireworkeffect")) {
                    String[] fireworkProp = property[1].split("/");
                    FireworkEffect.Builder fx = FireworkEffect.builder();
                    fx.with(FireworkEffect.Type.valueOf(fireworkProp[0].toUpperCase()));
                    String[] color1 = fireworkProp[1].split(";");
                    fx.withColor(Color.fromRGB(Integer.parseInt(color1[0]), Integer.parseInt(color1[1]), Integer.parseInt(color1[2])));
                    String[] color2 = fireworkProp[2].split(";");
                    fx.withFade(Color.fromRGB(Integer.parseInt(color2[0]), Integer.parseInt(color2[1]), Integer.parseInt(color2[2])));
                    if(Boolean.valueOf(fireworkProp[3])) {
                        fx.withFlicker();
                    }
                    if(Boolean.valueOf(fireworkProp[4])) {
                        fx.withTrail();
                    }
                    fireworkEffects.add(fx.build());
                } else if(property[0].equalsIgnoreCase("fireworkpower")) {
                    power = Integer.parseInt(property[1]);
                } else if(property[0].equalsIgnoreCase("leathercolor")) {
                    String[] colors = property[1].split(";");
                    color = Color.fromRGB(Integer.parseInt(colors[0]), Integer.parseInt(colors[1]), Integer.parseInt(colors[2]));
                } else if(property[0].equalsIgnoreCase("skullname")) {
                    skullName = property[1];
                } else if(property[0].equalsIgnoreCase("bookauthor")) {
                    bookAuthor = property[1];
                } else if(property[0].equalsIgnoreCase("booktitle")) {
                    bookTitle = property[1].replaceAll("&", "§");
                } else if(property[0].equalsIgnoreCase("bookpage")) {
                    if(property[1].length() < 256 && bookPages.size() <= 50) {
                        bookPages.add(property[1].replaceAll("&", "§"));
                    }
                } else {
                    pl.getLogger().log(Level.CONFIG, "Couldn't find property option for " + property[0]);
                }
            } catch(NumberFormatException e) {
                pl.getLogger().log(Level.WARNING, "Couldn't set NUMBER for " + properties);
            }
        }
        ItemStack itemstack = new ItemStack(material, quantity, damage);
        itemstack.addEnchantments(enchantments);
        itemstack.addUnsafeEnchantments(unsafeenchantments);
        ItemMeta meta = itemstack.getItemMeta();
        meta.setLore(lore);
        if(displayname.length() > 0) {
            meta.setDisplayName(displayname);
        }
        itemstack.setItemMeta(meta);
        if(material.equals(Material.LEATHER_BOOTS) || material.equals(Material.LEATHER_CHESTPLATE) || material.equals(Material.LEATHER_LEGGINGS) || material.equals(Material.LEATHER_HELMET)) {
            LeatherArmorMeta leatherMeta = (LeatherArmorMeta) itemstack.getItemMeta();
            leatherMeta.setColor(color);
            itemstack.setItemMeta(leatherMeta);
        }
        if(material.equals(Material.FIREWORK)) {
            FireworkMeta fireworkMeta = (FireworkMeta) itemstack.getItemMeta();
            fireworkMeta.addEffects(fireworkEffects);
            fireworkMeta.setPower(power);
            itemstack.setItemMeta(fireworkMeta);
        }
        if(material.equals(Material.SKULL_ITEM)) {
            SkullMeta skullMeta = (SkullMeta) itemstack.getItemMeta();
            skullMeta.setOwner(skullName);
            itemstack.setItemMeta(skullMeta);
        }
        if(material.equals(Material.BOOK_AND_QUILL) || material.equals(Material.WRITTEN_BOOK)) {
            BookMeta bookMeta = (BookMeta) itemstack.getItemMeta();
            bookMeta.setAuthor(bookAuthor);
            bookMeta.setTitle(bookTitle);
            bookMeta.setPages(bookPages);
            itemstack.setItemMeta(bookMeta);
        }
        return itemstack;
    }

    public Double resolveStringChance(List<String> list) {
        for(String properties : list) {
            String[] property = properties.split(":", 2);
            if(property[0].equalsIgnoreCase("chance")) {
                try {
                    return Double.parseDouble(property[1]);
                } catch(NumberFormatException e) {
                    pl.getLogger().log(Level.WARNING, "Couldn't set NUMBER for " + properties);
                }
            }
        }
        return 0.5D;
    }

    public PotionEffect resolvePotionEffect(List<String> list) {
        PotionEffectType potionType = PotionEffectType.JUMP;
        int duration = 10, amplifier = 1;
        for(String properties : list) {
            String[] property = properties.split(":", 2);
            try {
                if(property[0].equalsIgnoreCase("type")) {
                    potionType = PotionEffectType.getByName(property[1].toUpperCase());
                } else if(property[0].equalsIgnoreCase("amplifier")) {
                    amplifier = Integer.parseInt(property[1]) + 1;
                } else if(property[0].equalsIgnoreCase("duration")) {
                    duration = Integer.parseInt(property[1]);
                } else {
                    pl.getLogger().log(Level.CONFIG, "Couldn't find property option for " + property[0]);
                }
            } catch(NumberFormatException e) {
                pl.getLogger().log(Level.WARNING, "Couldn't set NUMBER for " + properties);
            }
        }
        duration *= 20; // measured in ticks
        return potionType.createEffect(duration, amplifier);
    }

    private void generateJoinPages() {
        int matchCounter = pl.lobbies.size();
        int pageNumber = 0;
        while(matchCounter <= (54 - 3) && matchCounter > 0) {
            pageNumber++;
            matchCounter -= getPageSize(matchCounter, 3);
        }
        if(joinPages != null) {
            for(IconMenu iconmenu : joinPages) {
                iconmenu.destroy();
            }
        }
        joinPages = new IconMenu[pageNumber];
        matchCounter = pl.lobbies.size();
        for(int page = 0; page < pageNumber; page++) {
            int size = getPageSize(matchCounter, 3);
            joinPages[page] = new IconMenu(pl, guiY.getConfig().getString("config.joinInventoryTitle").replaceAll("&", "§"), size, page, new IconMenuEventHandler() {

                @Override
                public void onInventoryClick(IconMenu.InventoryMenuClickEvent e) {
                    switch(e.getSlot()) {
                        case 0: // page next
                            if(e.getPage() + 1 <= joinPages.length - 1) {
                                e.getPlayer().closeInventory();
                                joinPages[e.getPage() + 1].openInventory(e.getPlayer());
                            }
                            e.setClosing(false);
                            break;
                        case 1: // page past
                            if(e.getPage() - 1 >= 0) {
                                e.getPlayer().closeInventory();
                                joinPages[e.getPage() - 1].openInventory(e.getPlayer());
                            }
                            e.setClosing(false);
                            break;
                        case 2: // close
                            e.setClosing(true);
                            break;
                        default:
                            if(e.getItem() != null && e.getItem().getItemMeta().hasDisplayName()) {
                                Lobby lobby = pl.getLobby(e.getItem().getItemMeta().getDisplayName());
                                if(lobby != null) {
                                    prejoin.put(e.getPlayer().getName(), lobby);
                                    e.setClosing(false);
                                    e.getPlayer().closeInventory();
                                    kitPages[0].openInventory(e.getPlayer());
                                } else {
                                    e.getPlayer().sendMessage(pl.getLang("matchNotFound"));
                                }
                            }
                            break;
                    }
                }

                @Override
                public void onInventoryClose(IconMenu.InventoryMenuCloseEvent e) {
                }
            });
            matchCounter -= size;
        }
        generateJoinGUIOptions();
    }

    private void generateKitPages() {
        int kitCounter = pl.gameKits.size();
        int pageNumber = 0;
        while(kitCounter > 0) {
            if(pl.config.useVoting) {
                while(kitCounter <= (54 - 4)) {
                    pageNumber++;
                    kitCounter -= getPageSize(kitCounter, 4);
                    if(kitCounter <= 0) {
                        break;
                    }
                }
            } else {
                while(kitCounter <= (54 - 3)) {
                    pageNumber++;
                    kitCounter -= getPageSize(kitCounter, 3);
                    if(kitCounter <= 0) {
                        break;
                    }
                }
            }
        }
        kitPages = new IconMenu[pageNumber];
        kitCounter = pl.gameKits.size();
        for(int page = 0; page < pageNumber; page++) {
            int size;
            if(pl.config.useVoting) {
                size = getPageSize(kitCounter, 4);
            } else {
                size = getPageSize(kitCounter, 3);
            }
            kitPages[page] = new IconMenu(pl, guiY.getConfig().getString("config.kitInventoryTitle").replaceAll("&", "§"), size, page, new IconMenuEventHandler() {

                @Override
                public void onInventoryClick(IconMenu.InventoryMenuClickEvent e) {
                    switch(e.getSlot()) {
                        case 0: // page next
                            if(e.getPage() + 1 <= kitPages.length - 1) {
                                e.getPlayer().closeInventory();
                                kitPages[e.getPage() + 1].openInventory(e.getPlayer());
                            }
                            e.setClosing(false);
                            break;
                        case 1: // page past
                            if(e.getPage() - 1 >= 0) {
                                e.getPlayer().closeInventory();
                                kitPages[e.getPage() - 1].openInventory(e.getPlayer());
                            }
                            e.setClosing(false);
                            break;
                        case 2: // close
                            e.setClosing(true);
                            break;
                        default:
                            if(e.getItem() != null && e.getItem().getItemMeta().hasDisplayName()) {
                                e.setClosing(true);
                                if(pl.config.useVoting && e.getSlot() == 3) {
                                    pl.voting.addPlayer(e.getPlayer(), true);
                                    break;
                                }
                                Kit kit = pl.gameKits.get(e.getItem().getItemMeta().getDisplayName());
                                if(kit != null) {
                                    if(kit.canUseKit(e.getPlayer())) {
                                        if(useVoting) {
                                            if(pl.voting.getAtual() != null && pl.voting.getAtual().getCurrentState().equals(State.WAITING)) {
                                                pl.voting.getAtual().addPlayer(e.getPlayer(), kit);
                                            } else {
                                                pl.voting.addPlayer(e.getPlayer(), false);
                                            }
                                        } else {
                                            if(prejoin.containsKey(e.getPlayer().getName())) {
                                                prejoin.get(e.getPlayer().getName()).addPlayer(e.getPlayer(), kit);
                                                prejoin.remove(e.getPlayer().getName());
                                                break;
                                            } else {
                                                e.getPlayer().sendMessage(pl.getLang("matchNotFound"));
                                                break;
                                            }
                                        }
                                    } else {
                                        e.getPlayer().sendMessage(pl.getLang("noPermission"));
                                        break;
                                    }
                                } else {
                                    e.setClosing(false);
                                }
                            }
                            break;
                    }
                }

                @Override
                public void onInventoryClose(IconMenu.InventoryMenuCloseEvent e) {
                    if(prejoin.containsKey(e.getPlayer().getName()) && !useVoting) {
                        prejoin.remove(e.getPlayer().getName());
                        e.getPlayer().sendMessage(pl.getLang("lobbyConfigurationReseted"));
                    }
                }
            });
            kitCounter -= size;
        }
        generateKitGUIOptions();
    }

    public int getPageSize(int matches, int slotsUsed) {
        if(matches <= 9 - slotsUsed) {
            return 9;
        } else if(matches <= 18 - slotsUsed) {
            return 18;
        } else if(matches <= 27 - slotsUsed) {
            return 27;
        } else if(matches <= 36 - slotsUsed) {
            return 36;
        } else if(matches <= 45 - slotsUsed) {
            return 45;
        } else {
            return 54;
        }
    }

    private void generateJoinGUIOptions() {
        new BukkitRunnable() {

            @Override
            public void run() {
                ArrayList<Lobby> lobbies = new ArrayList<Lobby>(pl.lobbies.values());
                Iterator<Lobby> iterator = lobbies.iterator();
                for(IconMenu iconMenu : joinPages) {
                    if(iconMenu.getPage() + 1 <= joinPages.length - 1) {
                        iconMenu.setOption(0, nextPage);
                    } else {
                        iconMenu.setOption(0, disabledPage);
                    }
                    if(iconMenu.getPage() - 1 >= 0) {
                        iconMenu.setOption(1, backPage);
                    } else {
                        iconMenu.setOption(1, disabledPage);
                    }
                    iconMenu.setOption(2, closePage);
                    int atual = 3;
                    while(iterator.hasNext()) {
                        if(atual < 54) {
                            Lobby lob = iterator.next();
                            ItemStack shown = getShownItem(lob.getCurrentState());
                            ItemMeta meta = shown.getItemMeta();
                            meta.setDisplayName(lob.getPath());
                            if(meta.hasLore()) {
                                List<String> lore = meta.getLore();
                                List<String> editedLore = new ArrayList<String>();
                                for(String line : lore) {
                                    editedLore.add(line.replaceAll("%state", pl.getStateName(lob.getCurrentState())).replaceAll("%players", Integer.toString(pl.getPlayersFromLobby(lob).size())).replaceAll("%max", Integer.toString(lob.getMaxPlayers())).replaceAll("%min", Integer.toString(lob.getMinPlayers())));
                                }
                                for(String line : lob.getLore()) {
                                    editedLore.add(line.replaceAll("%state", pl.getStateName(lob.getCurrentState())).replaceAll("%players", Integer.toString(pl.getPlayersFromLobby(lob).size())).replaceAll("%max", Integer.toString(lob.getMaxPlayers())).replaceAll("%min", Integer.toString(lob.getMinPlayers())));
                                }
                                meta.setLore(editedLore);
                            }
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
        }.runTaskTimer(pl, 1, 20 * guiUpdateDelay);
    }

    private void generateKitGUIOptions() {
        new BukkitRunnable() {

            @Override
            public void run() {
                ArrayList<Kit> kits = new ArrayList<Kit>(pl.gameKits.values());
                Iterator<Kit> iterator = kits.iterator();
                for(IconMenu iconMenu : kitPages) {
                    if(iconMenu.getPage() + 1 <= kitPages.length - 1) {
                        iconMenu.setOption(0, nextPage);
                    } else {
                        iconMenu.setOption(0, disabledPage);
                    }
                    if(iconMenu.getPage() - 1 >= 0) {
                        iconMenu.setOption(1, backPage);
                    } else {
                        iconMenu.setOption(1, disabledPage);
                    }
                    iconMenu.setOption(2, closePage);
                    if(pl.config.useVoting) {
                        iconMenu.setOption(3, noKitItem);
                        int atual = 4;
                        while(iterator.hasNext()) {
                            if(atual < 54) {
                                Kit kit = iterator.next();
                                ItemStack shown = kit.getItemShown();
                                ItemMeta meta = shown.getItemMeta();
                                meta.setDisplayName(kit.getPath());
                                shown.setItemMeta(meta);
                                iconMenu.setOption(atual, shown);
                                iterator.remove();
                                atual++;
                            } else {
                                break;
                            }
                        }
                    } else {
                        int atual = 3;
                        while(iterator.hasNext()) {
                            if(atual < 54) {
                                Kit kit = iterator.next();
                                ItemStack shown = kit.getItemShown();
                                ItemMeta meta = shown.getItemMeta();
                                meta.setDisplayName(kit.getPath());
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
            }
        }.runTaskTimer(pl, 1, 20 * guiUpdateDelay);
    }

    private ItemStack getShownItem(State state) {
        return resolveItemStack(guiY.getConfig().getStringList("items.state." + state.name()));
    }

    private void generateSponsorPages() {
        int sponsorCounter = pl.sponsorKits.size();
        int pageNumber = 0;
        while(sponsorCounter > 0) {
            while(sponsorCounter <= (54 - 3)) {
                pageNumber++;
                sponsorCounter -= getPageSize(sponsorCounter, 3);
                if(sponsorCounter <= 0) {
                    break;
                }
            }
        }
        sponsorMenu = new IconMenu[pageNumber];
        sponsorCounter = pl.sponsorKits.size();
        for(int page = 0; page < pageNumber; page++) {
            int size = getPageSize(sponsorCounter, 3);
            sponsorMenu[page] = new IconMenu(pl, guiY.getConfig().getString("config.sponsorListInventoryTitle").replaceAll("&", "§"), size, page, new IconMenuEventHandler() {

                @SuppressWarnings("RedundantCast")
                @Override
                public void onInventoryClick(IconMenu.InventoryMenuClickEvent e) {
                    switch(e.getSlot()) {
                        case 0: // page next
                            if(e.getPage() + 1 <= sponsorMenu.length - 1) {
                                e.getPlayer().closeInventory();
                                sponsorMenu[e.getPage() + 1].openInventory(e.getPlayer());
                            }
                            e.setClosing(false);
                            break;
                        case 1: // page past
                            if(e.getPage() - 1 >= 0) {
                                e.getPlayer().closeInventory();
                                sponsorMenu[e.getPage() - 1].openInventory(e.getPlayer());
                            }
                            e.setClosing(false);
                            break;
                        case 2: // close
                            e.setClosing(true);
                            break;
                        default:
                            if(e.getItem() != null && e.getItem().getItemMeta().hasDisplayName()) {
                                e.setClosing(true);
                                SponsorKit kit = pl.sponsorKits.get(e.getItem().getItemMeta().getDisplayName());
                                if(kit != null && pl.players.containsKey(e.getPlayer())) {
                                    Player sponsoring = pl.players.get(e.getPlayer()).getSponsorTo(e.getPlayer());
                                    if(sponsoring != null && pl.players.get(e.getPlayer()).getCurrentState() == State.PLAYING) {
                                        Object sponsor = kit.giveSponsor(sponsoring, e.getPlayer());
                                        if(sponsor != null) {
                                            if(pl.players.get(e.getPlayer()).addSponsor(e.getPlayer(), kit, sponsoring)) {
                                                if(sponsor instanceof ItemStack) {
                                                    sponsoring.getInventory().addItem((ItemStack) sponsor);
                                                } else if(sponsor instanceof PotionEffect) {
                                                    sponsoring.addPotionEffect((PotionEffect) sponsor);
                                                }
                                            }
                                            sponsoring.sendMessage(pl.getLang("youveBeenSponsored").replaceAll("%buyier", e.getPlayer().getDisplayName()));
                                            e.getPlayer().sendMessage(pl.getLang("sponsorPurchased"));
                                            break;
                                        }
                                    } else {
                                        e.getPlayer().sendMessage(pl.getLang("failedToSponsor"));
                                        break;
                                    }
                                } else {
                                    e.getPlayer().sendMessage(pl.getLang("sponsorNotFound"));
                                    break;
                                }
                            }
                            break;
                    }
                }

                @Override
                public void onInventoryClose(IconMenu.InventoryMenuCloseEvent e) {
                    if(pl.players.containsKey(e.getPlayer())) {
                        pl.players.get(e.getPlayer()).removeSponsoring(e.getPlayer());
                    }
                }
            });
            sponsorCounter -= size;
        }
        generateSponsorGUIOptions();
    }

    private void generateSponsorGUIOptions() {
        new BukkitRunnable() {

            @Override
            public void run() {
                ArrayList<SponsorKit> sponsorkits = new ArrayList<SponsorKit>(pl.sponsorKits.values());
                Iterator<SponsorKit> iterator = sponsorkits.iterator();
                for(IconMenu iconMenu : sponsorMenu) {
                    if(iconMenu.getPage() + 1 <= sponsorMenu.length - 1) {
                        iconMenu.setOption(0, nextPage);
                    } else {
                        iconMenu.setOption(0, disabledPage);
                    }
                    if(iconMenu.getPage() - 1 >= 0) {
                        iconMenu.setOption(1, backPage);
                    } else {
                        iconMenu.setOption(1, disabledPage);
                    }
                    iconMenu.setOption(2, closePage);
                    int atual = 3;
                    while(iterator.hasNext()) {
                        if(atual < 54) {
                            SponsorKit kit = iterator.next();
                            ItemStack shown = kit.getShown();
                            ItemMeta meta = shown.getItemMeta();
                            meta.setDisplayName(kit.getPath());
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
        }.runTaskTimer(pl, 1, 20 * guiUpdateDelay);
    }

    public void saveDynamicSigns() {
        FileConfiguration lobbies = lobbiesY.getConfig();
        for(Map.Entry<Lobby, ArrayList<Sign>> set : pl.dynamicSigns.entrySet()) {
            List<String> config = new ArrayList<String>();
            for(Sign sign : set.getValue()) {
                if(sign != null) {
                    config.add(locationToString(sign.getLocation()));
                }
            }
            if(lobbies.getConfigurationSection("dynamic-signs." + set.getKey().getPath()) != null || lobbies.contains("dynamic-signs." + set.getKey().getPath())) {
                lobbies.set("dynamic-signs." + set.getKey().getPath(), config);
            } else {
                lobbies.createSection("dynamic-signs." + set.getKey().getPath());
                lobbies.addDefault("dynamic-signs." + set.getKey().getPath(), config);
            }
        }
        lobbiesY.saveConfig();
    }

    public Lobby getLobbyFromSign(Sign sign) {
        for(Map.Entry<Lobby, ArrayList<Sign>> set : pl.dynamicSigns.entrySet()) {
            if(set.getValue().contains(sign)) {
                return set.getKey();
            }
        }
        return null;
    }

    public boolean removeDynamicSign(Sign sign) {
        for(Map.Entry<Lobby, ArrayList<Sign>> set : pl.dynamicSigns.entrySet()) {
            if(set.getValue().contains(sign)) {
                return set.getValue().remove(sign);
            }
        }
        return false;
    }

    private void generateDynamicSignsTask() {
        new BukkitRunnable() {

            @Override
            public void run() {
                for(Map.Entry<Lobby, ArrayList<Sign>> set : pl.dynamicSigns.entrySet()) {
                    for(Sign sign : set.getValue()) {
                        if(sign != null) {
                            for(int i = 0; i < 4; i++) {
                                sign.setLine(i, signLines[i]
                                        .replaceAll("%path", set.getKey().getPath())
                                        .replaceAll("%displayname", set.getKey().getName())
                                        .replaceAll("%alive", set.getKey().getAliveNonMutattorSize())
                                        .replaceAll("%min", Integer.toString(set.getKey().getMinPlayers()))
                                        .replaceAll("%max", Integer.toString(set.getKey().getMaxPlayers()))
                                        .replaceAll("%state", pl.getStateName(set.getKey().getCurrentState())));
                            }
                            sign.update(true);
                        }
                    }
                }
            }
        }.runTaskTimer(pl, 1, signDelay);
    }

    public void clearVariables() {
        for(Lobby l : pl.lobbies.values()) {
            l.endMatch(true, false);
        }
        pl.voting = null;
        pl.gameKits.clear();
        pl.sponsorKits.clear();
        pl.chestTiers.clear();
        pl.lobbies.clear();
        pl.players.clear();
        pl.muttated.clear();
        pl.invisiblePlayers.clear();
        pl.haveMuttated.clear();
        pl.stuckPlayers.clear();
        pl.supplyCrates.clear();
        prejoin.clear();
        saveDynamicSigns();
        pl.dynamicSigns.clear();
    }

    public boolean saveConfig(GameSetup setup) {
        FileConfiguration lobbies = lobbiesY.getConfig();
        if(!setup.isEdited()) {
            lobbies.createSection("lobbies." + setup.getPath());
            lobbies.addDefault("lobbies." + setup.getPath() + ".minPlayers", setup.getMinPlayer());
            lobbies.addDefault("lobbies." + setup.getPath() + ".maxPlayers", setup.getMaxPlayer());
            lobbies.addDefault("lobbies." + setup.getPath() + ".playerCountToLightningEverybody", setup.getLightningThreshold());
            lobbies.addDefault("lobbies." + setup.getPath() + ".chestRefreshDelayInSeconds", setup.getChestRefresh());
            lobbies.addDefault("lobbies." + setup.getPath() + ".immortalityTimeAfterMatchStartInSeconds", setup.getGraceTime());
            lobbies.addDefault("lobbies." + setup.getPath() + ".waitTimeUntilMatchStartInSeconds", setup.getWaitTime());
            lobbies.addDefault("lobbies." + setup.getPath() + ".numberOfTriesWithEnoughPlayers", setup.getPossibleJoinsWithEnough());
            lobbies.addDefault("lobbies." + setup.getPath() + ".numberOfTriesWithoutEnoughPlayers", setup.getPossibleJoinsWithoutEnough());
            lobbies.addDefault("lobbies." + setup.getPath() + ".delayOfEachTry", setup.getPossibleJoinsTryDelay());
            lobbies.addDefault("lobbies." + setup.getPath() + ".maxMatchDurationInMinutes", setup.getMaxDuration());
            lobbies.addDefault("lobbies." + setup.getPath() + ".votingMapDescription", setup.getVotingDescription());
            lobbies.addDefault("lobbies." + setup.getPath() + ".displayName", setup.getDisplayName());
            lobbies.addDefault("lobbies." + setup.getPath() + ".kickNonVipPlayersForAVipJoin", setup.isKickVips());
            lobbies.addDefault("lobbies." + setup.getPath() + ".searchBlocksForChests", setup.isSearchForChests());
            lobbies.addDefault("lobbies." + setup.getPath() + ".allowedCommands", setup.getAllowedCommands());
            lobbies.addDefault("lobbies." + setup.getPath() + ".additionalDescription", setup.getDescription());
            lobbies.addDefault("lobbies." + setup.getPath() + ".firstCornerLocation", setup.getCorner1());
            lobbies.addDefault("lobbies." + setup.getPath() + ".secondCornerLocation", setup.getCorner2());
            lobbies.addDefault("lobbies." + setup.getPath() + ".spectatorSpawnLocation", setup.getSpectator());
            lobbies.addDefault("lobbies." + setup.getPath() + ".lobbyLocation", setup.getSafeLocation());
            lobbies.addDefault("lobbies." + setup.getPath() + ".exitLocation", setup.getExitLocation());
            lobbies.addDefault("lobbies." + setup.getPath() + ".itemsBlockedFromCrafting", setup.getBlockCrafting());
            lobbies.addDefault("lobbies." + setup.getPath() + ".blocksAllowedToPlace", setup.getAllowPlace());
            lobbies.addDefault("lobbies." + setup.getPath() + ".blocksAllowedToBreak", setup.getAllowBreak());
            lobbies.addDefault("lobbies." + setup.getPath() + ".savedChestLocations", setup.getChestLocation());
            lobbies.addDefault("lobbies." + setup.getPath() + ".spawnLocations", setup.getSpawnLocation());
        /* DEATHMATCH */
            if(setup.isDeathmatchEnabled()) {
                lobbies.addDefault("lobbies." + setup.getPath() + ".enableDeathmatch", true);
                lobbies.addDefault("lobbies." + setup.getPath() + ".deathmatchCenterToDistanceLocation", setup.getDeathmatchCenter());
                lobbies.addDefault("lobbies." + setup.getPath() + ".remainingDurationToStartDeathmatch", setup.getDeathmatchDuration());
                lobbies.addDefault("lobbies." + setup.getPath() + ".deathmatchEscapeDistanceInBlocks", setup.getDeathmatchDistance());
                lobbies.addDefault("lobbies." + setup.getPath() + ".deathmatchLocations", setup.getDeathmatchLocation());
            } else {
                lobbies.addDefault("lobbies." + setup.getPath() + ".enableDeathmatch", false);
            }
        } else {
            lobbies.set("lobbies." + setup.getPath() + ".minPlayers", setup.getMinPlayer());
            lobbies.set("lobbies." + setup.getPath() + ".maxPlayers", setup.getMaxPlayer());
            lobbies.set("lobbies." + setup.getPath() + ".playerCountToLightningEverybody", setup.getLightningThreshold());
            lobbies.set("lobbies." + setup.getPath() + ".chestRefreshDelayInSeconds", setup.getChestRefresh());
            lobbies.set("lobbies." + setup.getPath() + ".immortalityTimeAfterMatchStartInSeconds", setup.getGraceTime());
            lobbies.set("lobbies." + setup.getPath() + ".waitTimeUntilMatchStartInSeconds", setup.getWaitTime());
            lobbies.set("lobbies." + setup.getPath() + ".numberOfTriesWithEnoughPlayers", setup.getPossibleJoinsWithEnough());
            lobbies.set("lobbies." + setup.getPath() + ".numberOfTriesWithoutEnoughPlayers", setup.getPossibleJoinsWithoutEnough());
            lobbies.set("lobbies." + setup.getPath() + ".delayOfEachTry", setup.getPossibleJoinsTryDelay());
            lobbies.set("lobbies." + setup.getPath() + ".maxMatchDurationInMinutes", setup.getMaxDuration());
            lobbies.set("lobbies." + setup.getPath() + ".votingMapDescription", setup.getVotingDescription());
            lobbies.set("lobbies." + setup.getPath() + ".displayName", setup.getDisplayName());
            lobbies.set("lobbies." + setup.getPath() + ".kickNonVipPlayersForAVipJoin", setup.isKickVips());
            lobbies.set("lobbies." + setup.getPath() + ".searchBlocksForChests", setup.isSearchForChests());
            lobbies.set("lobbies." + setup.getPath() + ".allowedCommands", setup.getAllowedCommands());
            lobbies.set("lobbies." + setup.getPath() + ".additionalDescription", setup.getDescription());
            lobbies.set("lobbies." + setup.getPath() + ".firstCornerLocation", setup.getCorner1());
            lobbies.set("lobbies." + setup.getPath() + ".secondCornerLocation", setup.getCorner2());
            lobbies.set("lobbies." + setup.getPath() + ".spectatorSpawnLocation", setup.getSpectator());
            lobbies.set("lobbies." + setup.getPath() + ".lobbyLocation", setup.getSafeLocation());
            lobbies.set("lobbies." + setup.getPath() + ".exitLocation", setup.getExitLocation());
            lobbies.set("lobbies." + setup.getPath() + ".itemsBlockedFromCrafting", setup.getBlockCrafting());
            lobbies.set("lobbies." + setup.getPath() + ".blocksAllowedToPlace", setup.getAllowPlace());
            lobbies.set("lobbies." + setup.getPath() + ".blocksAllowedToBreak", setup.getAllowBreak());
            lobbies.set("lobbies." + setup.getPath() + ".savedChestLocations", setup.getChestLocation());
            lobbies.set("lobbies." + setup.getPath() + ".spawnLocations", setup.getSpawnLocation());
        /* DEATHMATCH */
            if(setup.isDeathmatchEnabled()) {
                lobbies.set("lobbies." + setup.getPath() + ".enableDeathmatch", true);
                lobbies.set("lobbies." + setup.getPath() + ".deathmatchCenterToDistanceLocation", setup.getDeathmatchCenter());
                lobbies.set("lobbies." + setup.getPath() + ".remainingDurationToStartDeathmatch", setup.getDeathmatchDuration());
                lobbies.set("lobbies." + setup.getPath() + ".deathmatchEscapeDistanceInBlocks", setup.getDeathmatchDistance());
                lobbies.set("lobbies." + setup.getPath() + ".deathmatchLocations", setup.getDeathmatchLocation());
            } else {
                lobbies.set("lobbies." + setup.getPath() + ".enableDeathmatch", false);
            }
        }
        lobbiesY.saveConfig();
        return true;
    }

    public boolean removeLobbyPath(String path) {
        FileConfiguration lobbies = lobbiesY.getConfig();
        lobbies.set("lobbies." + path + ".minPlayers", null);
        lobbies.set("lobbies." + path + ".maxPlayers", null);
        lobbies.set("lobbies." + path + ".playerCountToLightningEverybody", null);
        lobbies.set("lobbies." + path + ".chestRefreshDelayInSeconds", null);
        lobbies.set("lobbies." + path + ".immortalityTimeAfterMatchStartInSeconds", null);
        lobbies.set("lobbies." + path + ".waitTimeUntilMatchStartInSeconds", null);
        lobbies.set("lobbies." + path + ".numberOfTriesWithEnoughPlayers", null);
        lobbies.set("lobbies." + path + ".numberOfTriesWithoutEnoughPlayers", null);
        lobbies.set("lobbies." + path + ".durationBetweenEachTryOfStarting", null);
        lobbies.set("lobbies." + path + ".maxMatchDurationInMinutes", null);
        lobbies.set("lobbies." + path + ".votingMapDescription", null);
        lobbies.set("lobbies." + path + ".displayName", null);
        lobbies.set("lobbies." + path + ".kickNonVipPlayersForAVipJoin", null);
        lobbies.set("lobbies." + path + ".searchBlocksForChests", null);
        lobbies.set("lobbies." + path + ".allowedCommands", null);
        lobbies.set("lobbies." + path + ".additionalDescription", null);
        lobbies.set("lobbies." + path + ".firstCornerLocation", null);
        lobbies.set("lobbies." + path + ".secondCornerLocation", null);
        lobbies.set("lobbies." + path + ".spectatorSpawnLocation", null);
        lobbies.set("lobbies." + path + ".lobbyLocation", null);
        lobbies.set("lobbies." + path + ".exitLocation", null);
        lobbies.set("lobbies." + path + ".itemsBlockedFromCrafting", null);
        lobbies.set("lobbies." + path + ".blocksAllowedToPlace", null);
        lobbies.set("lobbies." + path + ".blocksAllowedToBreak", null);
        lobbies.set("lobbies." + path + ".savedChestLocations", null);
        lobbies.set("lobbies." + path + ".spawnLocations", null);
        lobbies.set("lobbies." + path + ".enableDeathmatch", null);
        lobbies.set("lobbies." + path + ".deathmatchCenterToDistanceLocation", null);
        lobbies.set("lobbies." + path + ".remainingDurationToStartDeathmatch", null);
        lobbies.set("lobbies." + path + ".deathmatchEscapeDistanceInBlocks", null);
        lobbies.set("lobbies." + path + ".deathmatchLocations", null);
        lobbiesY.saveConfig();
        return true;
    }

    private void generateScoreboardConfig() {
        FileConfiguration scoreboards = scoreboardY.getConfig();
        String[] waiting = {"&6Players: &a%alive/%min", "blank", "&cMax duration: &4%maxdurationm", "blank", "&4K/D: &c%kdratio", "&4W/L: &c%wlratio", "&aPoints: &e%points"},
                pregame = {"&6Players: &a%alive/%max", "blank", "&cKit: &6%kitname", "blank", "&4K/D: &c%kdratio", "&4W/L: &c%wlratio", "&aPoints: &e%points"},
                playing = {"&6Players: &a%alive/%max", "blank", "&cKit: &6%kitname", "blank", "&4K/D: &c%kdratio", "&4W/L: &c%wlratio", "&aPoints: &e%points", "&cIs alive? &a%isalive"},
                deathmatch = {"&4K/D: &c%kdratio", "&4W/L: &c%wlratio", "&aPoints: &e%points", "&cIs alive? &a%isalive"};
        scoreboards.addDefault("scoreboards." + State.WAITING.toString().toLowerCase() + ".title", "&cWaiting...");
        scoreboards.addDefault("scoreboards." + State.WAITING.toString().toLowerCase() + ".lines", Arrays.asList(waiting));
        scoreboards.addDefault("scoreboards." + State.PREGAME.toString().toLowerCase() + ".title", "&cWait %waiting sec");
        scoreboards.addDefault("scoreboards." + State.PREGAME.toString().toLowerCase() + ".lines", Arrays.asList(pregame));
        scoreboards.addDefault("scoreboards." + State.PLAYING.toString().toLowerCase() + ".title", "&c%duration min");
        scoreboards.addDefault("scoreboards." + State.PLAYING.toString().toLowerCase() + ".lines", Arrays.asList(playing));
        scoreboards.addDefault("scoreboards." + State.DEATHMATCH.toString().toLowerCase() + ".title", "&c%duration min");
        scoreboards.addDefault("scoreboards." + State.DEATHMATCH.toString().toLowerCase() + ".lines", Arrays.asList(deathmatch));
        scoreboards.addDefault("scoreboards." + State.DISABLED.toString().toLowerCase() + ".title", "&4Disabled");
        scoreboards.addDefault("scoreboards." + State.DISABLED.toString().toLowerCase() + ".lines", Arrays.asList(empty));
        scoreboardY.saveConfig();
    }

    public ScoreboardManager.Replacer getReplacer(final Lobby lobby, final Jogador jogador) {
        return new ScoreboardManager.Replacer() {
            @Override
            public String replaceString(String message) {
                return message
                        .replaceAll("%isalive", Boolean.valueOf(jogador.isPlaying()).toString().toLowerCase())
                        .replaceAll("%kitname", jogador.getKit().getName())
                        .replaceAll("%kdratio", jogador.getRanking().getKillDeathRatio())
                        .replaceAll("%wlratio", jogador.getRanking().getWinLosRatio())
                        .replaceAll("%points", jogador.getRanking().getPoints())
                        .replaceAll("%maxduration", Integer.toString(lobby.getMaxDuration()))
                        .replaceAll("%duration", lobby.getDurationRemaining())
                        .replaceAll("%alive", lobby.getAliveNonMutattorSize())
                        .replaceAll("%max", Integer.toString(lobby.getMaxPlayers()))
                        .replaceAll("%min", Integer.toString(lobby.getMinPlayers()))
                        .replaceAll("%waiting", Integer.toString(lobby.getWaitTime()))
                        .replaceAll("&", "§");
            }
        };
    }

    public String getScoreboardTitle(State state) {
        return scoreboardY.getConfig().getString("scoreboards." + state.toString().toLowerCase() + ".title");
    }

    public String[] getScoreboardLines(State state) {
        List<String> list = scoreboardY.getConfig().getStringList("scoreboards." + state.toString().toLowerCase() + ".lines");
        return list.toArray(new String[list.size()]);
    }
}
