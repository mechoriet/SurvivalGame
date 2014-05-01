package com.jabyftw.sgames.util;

import org.bukkit.permissions.Permission;

/**
 * @author Rafael
 */
public class Permissions {

    public final Permission management_kick = new Permission("survivalgames.management.kick"),
            management_stop = new Permission("survivalgames.management.stoplobby"),
            management_start = new Permission("survivalgames.management.startlobby"),
            operator_editsign = new Permission("survivalgames.editsigns"),
            operator_breakall = new Permission("survivalgames.breakallblocks"),
            operator_placeall = new Permission("survivalgames.placeallblocks"),
            operator_usecommands = new Permission("survivalgames.useanycommand"),
            player_joinfulllobby = new Permission("survivalgames.joinfulllobby"),
            player_join = new Permission("survivalgames.join"),
            player_vote = new Permission("survivalgames.vote"),
            player_spectate = new Permission("survivalgames.spectate"),
            player_sponsor = new Permission("survivalgames.sponsor"),
            player_stats = new Permission("survivalgames.stats"),
            player_ranking = new Permission("survivalgames.ranking"),
            setup_permission = new Permission("survivalgames.setup.all");
}
