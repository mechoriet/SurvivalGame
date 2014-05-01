package com.jabyftw.sgames.item;

import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.util.RandomCollection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author Rafael
 */
public class Tier {

    private final SurvivalGames pl;
    private final String path;
    private final int itemsPerChest;
    private final boolean randomQuantity;
    private final RandomCollection<ItemStack> items = new RandomCollection<ItemStack>();

    public Tier(SurvivalGames pl, String path, int itemsperchest, boolean randomquantity, Map<ItemStack, Double> items) {
        this.pl = pl;
        this.path = path;
        this.itemsPerChest = itemsperchest;
        this.randomQuantity = randomquantity;
        for(Map.Entry<ItemStack, Double> set : items.entrySet()) {
            if(set.getKey() != null) {
                this.items.add(set.getValue(), set.getKey());
            }
        }
    }

    public String getPath() {
        return path;
    }

    public ItemStack getItem() {
        ItemStack itemstack = items.next();
        if(randomQuantity) {
            itemstack.setAmount((int) pl.getRandom(1, itemstack.getAmount()));
        }
        return itemstack;
    }

    public ItemStack[] getItems() {
        ArrayList<ItemStack> itemstacks = new ArrayList<ItemStack>();
        for(int x = 0; x < itemsPerChest; x++) {
            itemstacks.add(getItem());
        }
        return itemstacks.toArray(new ItemStack[itemstacks.size()]);
    }
}
