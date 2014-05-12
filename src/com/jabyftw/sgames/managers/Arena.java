package com.jabyftw.sgames.managers;

import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.item.SupplyCrate;
import com.jabyftw.sgames.util.Location2D;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Level;

public class Arena {

    private final SurvivalGames pl;
    private final boolean searchForChests;
    private final ArrayList<Material> allowPlace = new ArrayList<Material>(), allowBreak = new ArrayList<Material>(), blockedCrafting = new ArrayList<Material>();
    private final LinkedHashMap<Location, Boolean> Spawns = new LinkedHashMap<Location, Boolean>(), deathmatchSpawns = new LinkedHashMap<Location, Boolean>();
    private final Location spectatorSpawn, center, corner1, corner2, safeLocation, exitLocation;

    private final LinkedHashMap<Location, Material> changedBlocks = new LinkedHashMap<Location, Material>();
    private final HashMap<Location, String> chestLocation = new HashMap<Location, String>();
    private final ArrayList<SupplyCrate> cratesDropped = new ArrayList<SupplyCrate>();
    private final ArrayList<Location2D> cratesUsed = new ArrayList<Location2D>();
    private final ArrayList<Chunk> lobbyChunks = new ArrayList<Chunk>();
    private final ArrayList<Entity> entitiesToRemove = new ArrayList<Entity>();

    private boolean useDeathmatch;

    public Arena(SurvivalGames pl,
                 boolean searchforchests, boolean usedeathmatch,
                 HashMap<Location, String> chestlocation,
                 ArrayList<Material> allowplace, ArrayList<Material> allowbreak, ArrayList<Material> blockedcrafting,
                 ArrayList<Location> spawns, ArrayList<Location> dmspawns,
                 Location spectator, Location center, Location corner1, Location corner2, Location safelocation, Location exitlocation) {
        this.pl = pl;
        this.searchForChests = searchforchests;
        this.useDeathmatch = usedeathmatch;
        for(Material material : allowplace) {
            if(material != null) {
                this.allowPlace.add(material);
            }
        }
        for(Material material : allowbreak) {
            if(material != null) {
                this.allowBreak.add(material);
            }
        }
        for(Material material : blockedcrafting) {
            if(material != null) {
                this.blockedCrafting.add(material);
            }
        }
        for(Location loc : spawns) {
            if(loc != null && loc.getWorld() != null) {
                this.Spawns.put(loc, false);
            }
        }
        for(Location loc : dmspawns) {
            if(loc != null && loc.getWorld() != null) {
                this.deathmatchSpawns.put(loc, false);
            }
        }
        this.spectatorSpawn = spectator;
        this.center = center;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.safeLocation = safelocation;
        this.exitLocation = exitlocation;
        searchChunks();
        validChests(chestlocation.entrySet());
    }

    public Arena(SurvivalGames pl,
                 boolean searchforchests, boolean usedeathmatch,
                 HashMap<Location, String> chestlocation,
                 ArrayList<Material> allowplace, ArrayList<Material> allowbreak, ArrayList<Material> blockedcrafting,
                 ArrayList<Location> spawns,
                 Location spectator, Location corner1, Location corner2, Location safelocation, Location exitlocation) {
        this.pl = pl;
        this.searchForChests = searchforchests;
        this.useDeathmatch = usedeathmatch;
        for(Material material : allowplace) {
            if(material != null) {
                this.allowPlace.add(material);
            }
        }
        for(Material material : allowbreak) {
            if(material != null) {
                this.allowBreak.add(material);
            }
        }
        for(Material material : blockedcrafting) {
            if(material != null) {
                this.blockedCrafting.add(material);
            }
        }
        for(Location loc : spawns) {
            if(loc != null && loc.getWorld() != null) {
                this.Spawns.put(loc, false);
            }
        }
        this.spectatorSpawn = spectator;
        this.center = null;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.safeLocation = safelocation;
        this.exitLocation = exitlocation;
        searchChunks();
        validChests(chestlocation.entrySet());
    }

    public Location getSafeLocation() {
        return safeLocation;
    }

    public Location getExitLocation() {
        return exitLocation;
    }

    public ArrayList<Material> getBlockedCrafting() {
        return blockedCrafting;
    }

    public boolean addBrokenBlock(Player p, Location loc, Material mat) {
        if(allowBreak.contains(mat) || p.hasPermission(pl.permissions.operator_breakall)) {
            if(changedBlocks.containsKey(loc)) {
                changedBlocks.put(loc, changedBlocks.remove(loc));
            }
            return true;
        }
        return false;
    }

    public boolean addPlacedBlock(Player p, Location loc, Material mat) {
        if(allowPlace.contains(mat) || p.hasPermission(pl.permissions.operator_placeall)) {
            if(changedBlocks.containsKey(loc)) {
                changedBlocks.put(loc, changedBlocks.remove(loc));
            }
            return true;
        }
        return false;
    }

    public void restoreBrokenAndPlacedBlocks() {
        for(Map.Entry<Location, Material> set : changedBlocks.entrySet()) {
            set.getKey().getBlock().setType(set.getValue());
        }
        changedBlocks.clear();
    }

    private void searchChunks() {
        if(corner1.getWorld() != null && corner2.getWorld() != null && corner1.getWorld().equals(corner2.getWorld())) {
            for(int x = Math.min(corner1.getChunk().getX(), corner2.getChunk().getX()); x <= Math.max(corner1.getChunk().getX(), corner2.getChunk().getX()); x++) {
                for(int z = Math.min(corner1.getChunk().getZ(), corner2.getChunk().getZ()); z <= Math.max(corner1.getChunk().getZ(), corner2.getChunk().getZ()); z++) {
                    Chunk c = corner1.getWorld().getChunkAt(x, z);
                    lobbyChunks.add(c);
                    if(searchForChests) {
                        for(BlockState bs : c.getTileEntities()) {
                            if(isAChest(bs)) {
                                addChestLocation(bs.getLocation(), pl.config.defaultTier.getPath());
                            }
                        }
                    }
                }
            }
        } else {
            pl.getLogger().log(Level.WARNING, "Skipping chunk searching, world is null (not found) or first corner's world isn't the same as second corner's world");
            pl.getLogger().log(Level.WARNING, "There WILL be errors and features not working properly.");
        }
    }

    private boolean addChestLocation(Location location, String tier) {
        if(location != null && location.getWorld() != null) {
            if(pl.chestTiers.get(tier) != null) {
                chestLocation.put(location, tier);
            } else {
                chestLocation.put(location, pl.config.defaultTier.getPath());
            }
            return true;
        }
        return false;
    }

    private void validChests(Set<Map.Entry<Location, String>> entrySet) {
        for(Map.Entry<Location, String> set : entrySet) {
            Location loc = set.getKey().clone();
            if(isAChest(loc.getBlock().getState())) {
                addChestLocation(loc, set.getValue());
            } else {
                loc.subtract(0, 1, 0);
                if(isAChest(loc.getBlock().getState())) {
                    addChestLocation(loc, set.getValue());
                }
            }
        }
    }

    public boolean checkVariables() {
        if(spectatorSpawn != null && spectatorSpawn.getWorld() != null && corner1 != null && safeLocation != null && safeLocation.getWorld() != null && corner1.getWorld() != null && corner2 != null && corner2.getWorld() != null) {
            if(center != null && center.getWorld() != null && deathmatchSpawns.size() > 0) {
                return true;
            }
            useDeathmatch = false;
            return true;
        }
        return false;
    }

    public Location getSpawnLocation() {
        for(Map.Entry<Location, Boolean> set : Spawns.entrySet()) {
            if(!set.getValue()) {
                Spawns.put(set.getKey(), true);
                return set.getKey();
            }
        }
        Iterator<Location> it = Spawns.keySet().iterator();
        if(it.hasNext()) {
            return it.next();
        }
        return null;
    }

    public Location getDeathmatchSpawnLocation() {
        if(useDeathmatch) {
            for(Map.Entry<Location, Boolean> set : deathmatchSpawns.entrySet()) {
                if(!set.getValue()) {
                    deathmatchSpawns.put(set.getKey(), true);
                    return set.getKey();
                }
            }
            Iterator<Location> it = deathmatchSpawns.keySet().iterator();
            if(it.hasNext()) {
                return it.next();
            }
        }
        return null;
    }

    public void resetSpawnUsage() {
        for(Location loc : Spawns.keySet()) {
            Spawns.put(loc, false);
        }
        if(useDeathmatch) {
            for(Location loc : deathmatchSpawns.keySet()) {
                deathmatchSpawns.put(loc, false);
            }
        }
    }

    public boolean useDeathmatch() {
        return useDeathmatch;
    }

    public boolean refreshChests() {
        if(chestLocation.size() > 0) {
            boolean failed = false;
            for(Map.Entry<Location, String> set : chestLocation.entrySet()) {
                Inventory inventory = getInventoryFromLocation(set.getKey().getBlock());
                if(inventory != null) {
                    inventory.clear();
                    if(pl.chestTiers.get(set.getValue()) != null) {
                        for(ItemStack is : pl.chestTiers.get(set.getValue()).getItems()) {
                            inventory.setItem((int) pl.getRandom(0, inventory.getSize() - 1), is);
                        }
                    } else {
                        failed = true;
                    }
                }
            }
            if(failed) {
                pl.getLogger().log(Level.WARNING, "Failed to obtain tier for chest.");
            }
            return !failed;
        }
        return false;
    }

    private Inventory getInventoryFromLocation(Block block) {
        if(block != null && block.getState() != null) {
            if(block.getState() instanceof Chest) {
                return ((Chest) block.getState()).getInventory();
            } else if(block.getState() instanceof DoubleChest) {
                return ((DoubleChest) block.getState()).getInventory();
            }
        }
        return null;
    }

    public Location getDeathmatchCenter() {
        return center;
    }

    public void addToRemove(Entity entity) {
        entitiesToRemove.add(entity);
    }

    public void removeEntities() {
        for(Chunk c : lobbyChunks) {
            for(Entity e : c.getEntities()) {
                if(pl.config.entitiesToRemove.contains(e.getType()) && !e.getType().equals(EntityType.PLAYER)) {
                    e.remove();
                }
            }
        }
        for(Entity e : entitiesToRemove) {
            if(pl.config.entitiesToRemove.contains(e.getType()) && !e.getType().equals(EntityType.PLAYER)) {
                if(e.isValid()) {
                    e.remove();
                }
            }
        }
        entitiesToRemove.clear();
    }

    private boolean isAChest(BlockState bs) {
        return (bs instanceof Chest || bs instanceof DoubleChest);
    }

    public void clearInventories() {
        for(Location location : chestLocation.keySet()) {
            Inventory inventory = getInventoryFromLocation(location.getBlock());
            if(inventory != null) {
                inventory.clear();
            }
        }
    }

    public boolean haveSuficientSpawns(int i) {
        return (Spawns.size() >= i);
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public void removeFallenCrates() {
        for(SupplyCrate sc : cratesDropped) {
            if(sc.getBlock() != null) {
                sc.getBlock().setType(Material.AIR);
            } else {
                sc.stopFireworkEffect();
                sc.getEntity().remove();
            }
            pl.supplyCrates.remove(sc);
        }
        cratesDropped.clear();
        cratesUsed.clear();
    }

    public boolean spawnCrate(Location loc) {
        Location2D loc2d = new Location2D(loc.getWorld(), loc.getBlockX(), loc.getBlockZ());
        if(!cratesUsed.contains(loc2d)) {
            SupplyCrate sc = new SupplyCrate(pl, pl.config.playCrateFirework, loc2d);
            pl.supplyCrates.add(sc);
            cratesDropped.add(sc);
            addToRemove(sc.getEntity());
            cratesUsed.add(loc2d);
            return true;
        }
        return false;
    }

    public ArrayList<Material> getAllowedPlace() {
        return allowPlace;
    }

    public ArrayList<Material> getAllowedBreak() {
        return allowBreak;
    }

    public Set<Map.Entry<Location, String>> getChestLocation() {
        return chestLocation.entrySet();
    }

    public Set<Location> getSpawns() {
        return Spawns.keySet();
    }

    public Set<Location> getDeathmatchSpawns() {
        return deathmatchSpawns.keySet();
    }

    public Location getCorner1() {
        return corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    public boolean searchChests() {
        return searchForChests;
    }
}
