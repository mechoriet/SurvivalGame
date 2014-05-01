package com.jabyftw.sgames.item;

import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.util.Location2D;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

/**
 * @author Rafael
 */
public class SupplyCrate {

    private final SurvivalGames pl;
    private final Location2D location;
    private final Random r = new Random();

    private BukkitTask fireworkEffect;
    private Inventory inventory = null;
    private FallingBlock entity = null;
    private Block block = null;

    public SupplyCrate(final SurvivalGames pl, boolean playfirework, Location2D loc) {
        this.pl = pl;
        this.location = loc;
        this.entity = loc.getWorld().spawnFallingBlock(new Location(loc.getWorld(), loc.getX(), 124, loc.getZ()), Material.PISTON_BASE, (byte) 6);
        if(playfirework) {
            fireworkEffect = new BukkitRunnable() {

                @Override
                public void run() {
                    Firework firework = entity.getLocation().getWorld().spawn(entity.getLocation(), Firework.class);
                    FireworkMeta meta = firework.getFireworkMeta();
                    meta.addEffect(
                            FireworkEffect.builder().flicker(r.nextBoolean())
                                    .with(getRandomType()).trail(true)
                                    .withColor(Color.fromRGB(r.nextInt(254) + 1, r.nextInt(254) + 1, r.nextInt(254) + 1))
                                    .withFade(Color.fromRGB(r.nextInt(254) + 1, r.nextInt(254) + 1, r.nextInt(254) + 1)).build()
                    );
                    meta.setPower(r.nextInt(3) + 1);
                    firework.setFireworkMeta(meta);
                }

                private FireworkEffect.Type getRandomType() {
                    switch(r.nextInt(4) + 1) {
                        case 1:
                            return Type.BALL;
                        case 2:
                            return Type.BALL_LARGE;
                        case 3:
                            return Type.BURST;
                        case 4:
                            return Type.CREEPER;
                        default:
                            return Type.STAR;
                    }
                }
            }.runTaskTimer(pl, 1, 10);
        }
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        inventory = pl.getServer().createInventory(null, 54, pl.config.crateInventoryTitle);
        inventory.addItem(pl.config.crateTier.getItems());
        this.block = block;
        stopFireworkEffect();
        entity = null;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public FallingBlock getEntity() {
        return entity;
    }

    public boolean isSameLocation(Location loc) {
        return (loc.getWorld().equals(location.getWorld()) && loc.getBlockX() == location.getX() && loc.getBlockZ() == location.getZ());
    }

    public void stopFireworkEffect() {
        if(fireworkEffect != null) {
            fireworkEffect.cancel();
            fireworkEffect = null;
        }
    }
}
