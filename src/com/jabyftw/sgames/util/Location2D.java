package com.jabyftw.sgames.util;

import org.bukkit.World;

/**
 * @author Rafael
 */
public class Location2D {

    private final World world;
    private final int x, z;

    public Location2D(World w, int x, int z) {
        this.world = w;
        this.x = x;
        this.z = z;
    }

    public World getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }
}
