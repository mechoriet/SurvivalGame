package com.jabyftw.sgames.item;

import com.jabyftw.sgames.SurvivalGames;
import com.jabyftw.sgames.util.RandomCollection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Map;

/**
 * @author Rafael
 */
public class SponsorKit extends Tier {

    private final SurvivalGames pl;
    private final String name;
    private final CostType cost;
    private final double gainPercentage;
    private final int costValue;
    private final boolean randomAmplifier;
    private final ItemStack itemShown;
    private final RandomCollection<PotionEffect> potionEffects = new RandomCollection<PotionEffect>();

    public SponsorKit(SurvivalGames pl,
                      String path, String name, int itemsperchest, double gainpercentage,
                      boolean randomquantity, boolean randomamplifier,
                      ItemStack shown, String cost,
                      Map<ItemStack, Double> items, Map<PotionEffect, Double> potioneffects) {
        super(pl, path, itemsperchest, randomquantity, items);
        this.pl = pl;
        this.name = name;
        this.gainPercentage = gainpercentage;
        this.randomAmplifier = randomamplifier;
        // Set cost type
        String[] costs = cost.split(";");
        if(costs[0].equalsIgnoreCase("playerpoints")) {
            this.cost = CostType.PLAYERPOINTS;
        } else {
            this.cost = CostType.MONEY;
        }
        costValue = Integer.parseInt(costs[1]);
        // set description
        this.itemShown = shown;
        // add potionEffects
        for(Map.Entry<PotionEffect, Double> set : potioneffects.entrySet()) {
            if(set.getKey() != null) {
                this.potionEffects.add(set.getValue(), set.getKey());
            }
        }
    }

    public ItemStack getShown() {
        return itemShown;
    }

    public PotionEffect getPotionEffect() {
        PotionEffect potionEffect = potionEffects.next();
        if(randomAmplifier) {
            potionEffect = potionEffect.getType().createEffect(potionEffect.getDuration(), (int) pl.getRandom(1, potionEffect.getAmplifier()));
        }
        return potionEffect;
    }

    public Object giveSponsor(Player p, Player buyier) {
        if(purchaseSponsor(buyier)) {
            if(pl.players.containsKey(p) && pl.players.get(p).isPlayerAlive(p)) {
                if(pl.getRandom(0, 1) > 0.555D) {
                    return getItem();
                } else {
                    return getPotionEffect();
                }
            }
        } else {
            buyier.sendMessage(pl.getLang("cantPurchaseSponsorKit"));
        }
        return null;
    }

    public double getPercentage() {
        return gainPercentage;
    }

    private boolean purchaseSponsor(Player p) {
        if(cost == CostType.MONEY && pl.config.useVault) {
            if(pl.config.econ.has(p.getName(), costValue)) {
                return pl.config.econ.withdrawPlayer(p.getName(), costValue).transactionSuccess();
            }
        } else if(cost == CostType.PLAYERPOINTS && pl.config.usePlayerPoints) {
            if(pl.config.playerp.getAPI().look(p.getUniqueId()) >= costValue) {
                pl.config.playerp.getAPI().take(p.getUniqueId(), costValue);
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public static enum CostType {

        MONEY, PLAYERPOINTS
    }
}
