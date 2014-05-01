package com.jabyftw.sgames.item;

import com.jabyftw.sgames.Jogador;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rafael
 */
public class Kit {

    private final String name, permission, path;
    private final ItemStack shownOnMenu;
    private final ArrayList<String> permisionsGiven = new ArrayList<String>();
    private final ArrayList<ItemStack> itemList = new ArrayList<ItemStack>();
    private final ArrayList<PotionEffect> potionEffects = new ArrayList<PotionEffect>(), potionEffectsKill = new ArrayList<PotionEffect>();

    public Kit(String path, String name, String permission, List<String> permissiongiven, List<ItemStack> itemlist, ItemStack shown, List<PotionEffect> potioneffects, List<PotionEffect> potioneffectskill) {
        this.path = path;
        this.name = name;
        this.permission = permission.toLowerCase();
        this.permisionsGiven.addAll(permissiongiven);
        this.itemList.addAll(itemlist);
        this.shownOnMenu = shown;
        this.potionEffects.addAll(potioneffects);
        this.potionEffectsKill.addAll(potioneffectskill);
    }

    public String getName() {
        return name;
    }

    public boolean canUseKit(Player p) {
        return permission.equalsIgnoreCase("null") || p.hasPermission(permission);
    }

    public ItemStack getItemShown() {
        return shownOnMenu;
    }

    public ItemStack[] getItems() {
        return itemList.toArray(new ItemStack[itemList.size()]);
    }

    public void usePotionEffects(Player p) {
        if(canUseKit(p)) {
            p.addPotionEffects(potionEffects);
        }
    }

    public void usePotionEffects(Jogador j) {
        usePotionEffects(j.getPlayer());
    }

    public void usePotionEffectsOnKill(Player p) {
        if(canUseKit(p)) {
            p.addPotionEffects(potionEffectsKill);
        }
    }

    public ArrayList<String> getPermissionsToGive() {
        return permisionsGiven;
    }

    public String getPath() {
        return path;
    }
}
