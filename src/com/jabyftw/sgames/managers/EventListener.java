package com.jabyftw.sgames.managers;

import com.jabyftw.sgames.Jogador;
import com.jabyftw.sgames.Spectator;
import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.SurvivalGames.State;
import com.jabyftw.sgames.item.SupplyCrate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

/**
 * @author Rafael
 */
@SuppressWarnings({"UnusedDeclaration", "RedundantCast"})
public class EventListener implements Listener {

    private final SurvivalGames pl;
    private final ArrayList<String> onCooldown = new ArrayList<String>();

    @SuppressWarnings("LeakingThisInConstructor")
    public EventListener(SurvivalGames pl) {
        this.pl = pl;
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        if(pl.players.containsKey(player) && (pl.players.get(player).getCurrentState() == State.PLAYING || pl.players.get(player).getCurrentState() == State.DEATHMATCH) && !player.hasPermission(pl.permissions.operator_usecommands)) {
            boolean blocked = true;
            String command = e.getMessage().replaceAll("/", "").toLowerCase();
            for(String allowed : pl.players.get(player).getAllowedCommands()) {
                if(command.startsWith(allowed)) {
                    blocked = false;
                    break;
                }
            }
            e.setCancelled(blocked);
            if(blocked) {
                player.sendMessage(pl.getLang("noPermission"));
            }
        }
    }

    @EventHandler
    public void onChatSend(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if(pl.config.useChat && pl.config.usePlayerPoints) {
            e.setFormat(pl.config.chatFormat.replaceAll("%displayname", player.getDisplayName()).replaceAll("%message", e.getMessage()).replaceAll("%points", Integer.toString(pl.config.playerp.getAPI().look(player.getUniqueId()))));
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        if(e.getLine(0).equalsIgnoreCase("[SG]") && e.getPlayer().hasPermission(pl.permissions.operator_editsign)) {
            Lobby lobby = pl.getLobby(e.getLine(1));
            if(lobby != null) {
                pl.dynamicSigns.get(lobby).add((Sign) e.getBlock().getState());
                e.getPlayer().sendMessage(pl.getLang("dynamicSignRegistered"));
            } else {
                e.getPlayer().sendMessage(pl.getLang("matchNotFound"));
            }
        }
    }

    @EventHandler(ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if(pl.players.containsKey(player)) {
            Lobby lobby = pl.players.get(player);
            if(lobby.isStuck()) {
                e.setCancelled(true);
            } else if(lobby.isPlayerAlive(player)) {
                if(player.getItemInHand() != null) {
                    if(e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                        if(player.getItemInHand().getType().equals(pl.config.playerSearcher)) {
                            if(!onCooldown.contains(player.getName())) {
                                Jogador near = null;
                                double lastDistance = (pl.config.maxDistance * pl.config.maxDistance);
                                for(Jogador online : lobby.getAliveJogador()) {
                                    double distance = online.getPlayer().getLocation().distanceSquared(player.getLocation());
                                    if(distance <= lastDistance) {
                                        near = online;
                                        lastDistance = distance;
                                    }
                                }
                                if(near != null) {
                                    player.setCompassTarget(near.getPlayer().getLocation());
                                    addCooldown(player.getName(), 10);
                                } else {
                                    player.sendMessage(pl.getLang("playerNotFound"));
                                    addCooldown(player.getName(), 7);
                                }
                                e.setCancelled(true);
                            }
                        } else if(player.getItemInHand().getType().equals(pl.config.playerCrate) || player.getItemInHand().getType().equals(Material.PISTON_BASE)) {
                            if(player.getInventory().contains(Material.PISTON_BASE)) {
                                player.getInventory().remove(new ItemStack(Material.PISTON_BASE, 1));
                            } else if(player.getInventory().contains(pl.config.playerCrate)) {
                                player.getInventory().remove(new ItemStack(pl.config.playerCrate, 1));
                            } else {
                                return;
                            }
                            Location loc = player.getLocation();
                            if(!lobby.getArena().spawnCrate(loc)) {
                                player.sendMessage(pl.getLang("cantDropCrateHere"));
                                loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.PISTON_BASE, 1));
                            }
                            if(pl.config.announceCrateDeploy) {
                                lobby.broadcastMessage(pl.config.crateAnnouncement.replaceAll("%x", Integer.toString(loc.getBlockX())).replaceAll("%y", Integer.toString(loc.getBlockY())).replaceAll("%z", Integer.toString(loc.getBlockZ())));
                            }
                            e.setCancelled(true);
                        }
                    }
                }
                //noinspection deprecation
                if((e.getAction().equals(Action.LEFT_CLICK_BLOCK) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && e.getClickedBlock().getType().equals(Material.PISTON_BASE) && e.getClickedBlock().getData() == (byte) 6) {
                    for(SupplyCrate sc : pl.supplyCrates) {
                        if(sc.isSameLocation(e.getClickedBlock().getLocation()) && sc.getBlock() != null) {
                            player.openInventory(sc.getInventory());
                            break;
                        }
                    }
                }
            } else if(lobby.isPlayerSpectator(player) && player.getItemInHand() != null) {
                if(player.getItemInHand().getType().equals(pl.config.spectatorPlayerSelector)) {
                    if(!onCooldown.contains(player.getName())) {
                        Spectator spec = lobby.getSpectatorFromPlayer(player);
                        if(spec != null) {
                            if(e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                                player.teleport(spec.getPast(true).getLocation().add(0, 1, 0));
                            } else if(e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                                player.teleport(spec.getNext(true).getLocation().add(0, 1, 0));
                            }
                            addCooldown(player.getName(), 3);
                            e.setCancelled(true);
                        } else {
                            lobby.removePlayer(player, false); // something went wrong, spectator can't be null
                        }
                    } else {
                        player.sendMessage(pl.getLang("onCooldown"));
                    }
                } else if(player.getItemInHand().getType().equals(pl.config.spectatorMuttation)) {
                    Spectator spec = lobby.getSpectatorFromPlayer(player);
                    if(spec instanceof Jogador) {
                        Jogador jogador = (Jogador) spec;
                        if(jogador.canComeBack()) {
                            Player killer = pl.getServer().getPlayerExact(jogador.getKiller());
                            if(lobby.isPlayerAlive(killer) && !jogador.isAlreadyMuttated() && lobby.muttatePlayer(jogador, killer)) {
                                player.sendMessage(pl.getLang("muttationSucceeded"));
                            } else {
                                player.sendMessage(pl.getLang("couldntMuttate"));
                            }
                        }
                    }
                } else if(player.getItemInHand().getType().equals(pl.config.spectatorPlayerSponsor)) {
                    player.performCommand("sg sponsor");
                }
            }
        } else {
            if(e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getClickedBlock().getState() instanceof Sign) {
                Sign sign = (Sign) e.getClickedBlock().getState();
                Lobby lob = pl.config.getLobbyFromSign(sign);
                if(lob != null) {
                    if(lob.getCurrentState() == State.WAITING) {
                        player.performCommand("sg join");
                    } else if(lob.getCurrentState() == State.PLAYING || lob.getCurrentState() == State.DEATHMATCH) {
                        player.performCommand("sg spectate");
                    } else {
                        player.sendMessage(pl.getLang("matchNotReady"));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if(e.getPlayer() instanceof Player && (e.getInventory().getHolder() instanceof Chest || e.getInventory().getHolder() instanceof DoubleChest)) {
            if(pl.muttated.containsKey((Player) e.getPlayer()) || (pl.players.containsKey((Player) e.getPlayer()) && pl.players.get((Player) e.getPlayer()).isPlayerSpectator((Player) e.getPlayer()))) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChangeBlock(EntityChangeBlockEvent e) {
        if(e.getEntity() instanceof FallingBlock) {
            FallingBlock fallingblock = (FallingBlock) e.getEntity();
            for(SupplyCrate sc : pl.supplyCrates) {
                if(sc.getEntity().equals(fallingblock)) {
                    sc.setBlock(e.getBlock());
                    break;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = false)
    public void onEntityTarget(EntityTargetEvent e) {
        if(e.getTarget() instanceof Player && pl.players.containsKey(e.getTarget()) &&
                (pl.players.get((Player) e.getTarget()).getImortality() || pl.players.get((Player) e.getTarget()).isPlayerSpectator((Player) e.getTarget()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareCrafting(PrepareItemCraftEvent e) {
        if(e.getInventory().getHolder() instanceof Player) {
            Player player = (Player) e.getInventory().getHolder();
            if(pl.players.containsKey(player)) {
                if(pl.players.get(player).isPlayerAlive(player)) {
                    if(e.getInventory().getResult() != null && pl.players.get(player).getArena().getBlockedCrafting().contains(e.getInventory().getResult().getType())) {
                        e.getInventory().setResult(null);
                    }
                } else {
                    e.getInventory().setResult(null);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent e) {
        if(pl.players.containsKey(e.getPlayer())) {
            if(pl.players.get(e.getPlayer()).isPlayerSpectator(e.getPlayer()) || pl.players.get(e.getPlayer()).getCurrentState().equals(State.PREGAME) || pl.players.get(e.getPlayer()).getCurrentState().equals(State.WAITING)) {
                e.setCancelled(true);
            } else {
                if(e.getBlockPlaced().getType().equals(Material.TNT) && pl.config.tntAutoIgnite) {
                    e.getBlockPlaced().setType(Material.AIR);
                    final Location loc = e.getBlockPlaced().getLocation();
                    if(pl.config.tntUseFakeExplosion) {
                        new BukkitRunnable() {

                            @Override
                            public void run() {
                                loc.getWorld().createExplosion(loc.getX(), loc.getY() + 0.55D, loc.getZ(), pl.config.tntPower, false, false);
                            }
                        }.runTaskLater(pl, 20 * pl.config.tntDelay);
                    } else {
                        loc.getWorld().spawn(loc, TNTPrimed.class).setFuseTicks(20 * pl.config.tntDelay);
                    }
                    e.setCancelled(false);
                } else {
                    e.setCancelled(!pl.players.get(e.getPlayer()).getArena().addPlacedBlock(e.getPlayer(), e.getBlockPlaced().getLocation(), e.getBlockReplacedState().getType()));
                }
            }
        }
    }

    @EventHandler
    public void onDmgByEntity(EntityDamageByEntityEvent e) {
        if(e.getEntity() instanceof Player) {
            Player damaged = (Player) e.getEntity();
            Player damager = null;
            if(e.getDamager() instanceof Player) {
                damager = (Player) e.getDamager();
            } else if(e.getDamager() instanceof Projectile) {
                Projectile proj = (Projectile) e.getDamager();
                if(proj.getShooter() instanceof Player) {
                    damager = (Player) proj.getShooter();
                    if(pl.players.containsKey(damager) && (!pl.players.containsKey(damaged) || (pl.players.containsKey(damaged) && pl.players.get(damaged).isPlayerSpectator(damaged)))) {
                        proj.setBounce(false);
                        damaged.teleport(damaged.getLocation().add(0, 3, 0));
                        damaged.setFlying(true);
                        Arrow newArrow = damager.launchProjectile(Arrow.class);
                        newArrow.setShooter(damager);
                        newArrow.setVelocity(proj.getVelocity());
                        proj.remove();
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            if(damager != null) {
                if(!pl.players.containsKey(damager) && !pl.players.containsKey(damaged)) {
                    //noinspection UnnecessaryReturnStatement
                    return;
                } else if(pl.players.containsKey(damager) && pl.players.containsKey(damaged) && pl.players.get(damager).isPlayerAlive(damager) && pl.players.get(damager).isPlayerAlive(damaged)) {
                    // if both is playing and alive, allow damage
                    if(pl.players.get(damager).equals(pl.players.get(damaged))) {
                        Lobby lobby = pl.players.get(damager);
                        if(lobby.getImortality()) {
                            e.setCancelled(true);
                        } else {
                            if(pl.muttated.containsKey(damager) && !pl.muttated.get(damager).equals(damaged.getName())) {
                                e.setCancelled(true);
                                damager.sendMessage(pl.getLang("youCanOnlyHitYourTarget").replaceAll("%target", pl.muttated.get(damager)));
                            }
                            if(damaged.getHealth() - e.getDamage() <= 0 && !e.isCancelled()) {
                                lobby.addDeath(damaged, damager);
                            }
                        }
                    } else {
                        e.setCancelled(true);
                    }
                } else { // one of them is playing but the other is not
                    e.setCancelled(true);
                }
            } else {
                if(pl.players.containsKey(damaged) && (pl.players.get(damaged).getImortality() || pl.players.get(damaged).isPlayerSpectator(damaged))) {
                    e.setCancelled(true);
                }
            }
        }
        if(e.getDamager() instanceof Player) {
            Player damager = (Player) e.getDamager();
            if(pl.players.containsKey(damager) && (!pl.players.get(damager).isPlayerAlive(damager)) || pl.players.get(damager).isStuck()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent e) {
        if(e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            if(pl.players.containsKey(player)) {
                if(pl.players.get(player).getImortality() || pl.players.get(player).isStuck() || pl.players.get(player).isPlayerSpectator(player)) {
                    e.setCancelled(true);
                } else {
                    if(player.getHealth() - e.getDamage() <= 0 && !e.isCancelled()) {
                        pl.players.get(player).addDeath(player);
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent e) {
        if(pl.players.containsKey(e.getPlayer())) {
            if(pl.players.get(e.getPlayer()).isPlayerSpectator(e.getPlayer()) || pl.players.get(e.getPlayer()).getCurrentState().equals(State.PREGAME) || pl.players.get(e.getPlayer()).getCurrentState().equals(State.WAITING)) {
                e.setCancelled(true);
            } else {
                e.setCancelled(true);
                if(pl.players.get(e.getPlayer()).getArena().addBrokenBlock(e.getPlayer(), e.getBlock().getLocation(), e.getBlock().getType())) {
                    if(pl.config.blockDropItem) {
                        e.setCancelled(false);
                    } else {
                        e.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        if(e.getBlock().getState() instanceof Sign) {
            Sign sign = (Sign) e.getBlock().getState();
            if(pl.config.removeDynamicSign(sign)) {
                e.getPlayer().sendMessage(pl.getLang("dynamicSignUnregistered"));
            }
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent e) {
        if(pl.players.containsKey(e.getPlayer()) && pl.players.get(e.getPlayer()).isStuck()) {
            Player player = e.getPlayer();
            Location from = e.getFrom();
            Location to = e.getTo();
            if(from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ() || from.getBlockY() != to.getBlockY()) {
                e.setCancelled(true);
                player.teleport(new Location(from.getWorld(), from.getX(), from.getBlockY(), from.getZ(), to.getYaw(), to.getPitch()));
            }
        }
    }

    @EventHandler
    public void onThrow(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        if(pl.players.containsKey(player)) {
            e.setCancelled(pl.players.get(player).isPlayerSpectator(player)
                    || pl.players.get(player).getCurrentState() == State.WAITING
                    || pl.players.get(player).getCurrentState() == State.PREGAME);
        }
    }

    @EventHandler
    public void onPickUp(PlayerPickupItemEvent e) {
        Player player = e.getPlayer();
        if(pl.players.containsKey(player)) {
            e.setCancelled(pl.players.get(player).isPlayerSpectator(player)
                    || pl.players.get(player).getCurrentState() == State.WAITING
                    || pl.players.get(player).getCurrentState() == State.PREGAME
                    || pl.muttated.containsKey(player));
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        if(e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            if(pl.players.containsKey(player) && pl.players.get(player).isPlayerSpectator(player)) {
                e.setFoodLevel(20);
            }
        }

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        checkExit(e.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        checkExit(e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        for(Player invisible : pl.invisiblePlayers) {
            e.getPlayer().hidePlayer(invisible);
        }
        if(pl.config.useAutojoin && pl.config.useVoting) {
            pl.voting.addPlayer(e.getPlayer(), false);
        }
    }

    private void checkExit(Player player) {
        if(pl.players.containsKey(player)) {
            pl.players.get(player).removePlayer(player, true);
        }
        pl.config.prejoin.remove(player.getName());
        pl.muttated.remove(player);
        pl.makeVisible(player);
    }

    private void addCooldown(final String name, int seconds) {
        onCooldown.add(name);
        new BukkitRunnable() {

            @Override
            public void run() {
                onCooldown.remove(name);
            }
        }.runTaskLater(pl, (seconds * 20));
    }
}
