package com.jabyftw.sgames.util;

import com.jabyftw.sgames.SurvivalGames;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;

public class MySQL {

    private final SurvivalGames pl;
    private final String user, pass, url;
    private final int rankingdelay;
    private final LinkedList<RankingEntry> ranking = new LinkedList<RankingEntry>();
    private Connection conn = null;

    public MySQL(SurvivalGames pl, String username, String password, String url, int rankingdelay) {
        this.pl = pl;
        this.user = username;
        this.pass = password;
        this.url = url;
        this.rankingdelay = rankingdelay;
    }

    public Connection getConn() {
        try {
            if(conn != null && conn.isValid(60)) {
                return conn;
            }
            conn = DriverManager.getConnection(url, user, pass);
        } catch(SQLException e) {
            pl.getLogger().log(Level.WARNING, "Couldn't connect to MySQL: " + e.getMessage());
        }
        return conn;
    }

    public void closeConn() {
        if(conn != null) {
            try {
                conn.close();
                conn = null;
            } catch(SQLException ex) {
                pl.getLogger().log(Level.WARNING, "Couldn't close MySQL connection: " + ex.getMessage());
            }
        }
    }

    public boolean createTable() {
        try {
            getConn().createStatement().execute("CREATE TABLE IF NOT EXISTS `sgames` (\n"
                    + "  `name` VARCHAR(24) NOT NULL,\n"
                    + "  `kills` INT NULL DEFAULT 0,\n"
                    + "  `deaths` INT NULL DEFAULT 0,\n"
                    + "  `wins` INT NULL DEFAULT 0,\n"
                    + "  PRIMARY KEY (`name`),\n"
                    + "  UNIQUE INDEX `name_UNIQUE` (`name` ASC));");
            return true;
        } catch(SQLException ex) {
            pl.getLogger().log(Level.WARNING, "Couldn't create MySQL table: " + ex.getMessage());
            return false;
        }
    }

    public void updateOrInsertPlayer(String name, int kills, int deaths, int wins) {
        name = name.toLowerCase();
        try {
            ResultSet rs = getConn().createStatement().executeQuery("SELECT `name` FROM `sgames` WHERE `name`='" + name + "';");
            if(rs.next()) {
                getConn().createStatement().executeUpdate("UPDATE `sgames` SET `kills`=" + kills + ", `deaths`=" + deaths + ", `wins`=" + wins + " WHERE `name`='" + name + "';");
            } else {
                getConn().createStatement().executeUpdate("INSERT INTO `sgames` (`name`, `kills`, `deaths`, `wins`) VALUE ('" + name + "', " + kills + ", " + deaths + ", " + wins + ");");
            }
        } catch(SQLException e) {
            pl.getLogger().log(Level.WARNING, "Couldn't update/insert/check for player stats: " + e.getMessage());
        }
    }

    public int[] getStats(String name) {
        name = name.toLowerCase();
        int[] result = {0, 0, 0};
        try {
            ResultSet rs = getConn().createStatement().executeQuery("SELECT * FROM `sgames` WHERE `name`='" + name + "' LIMIT 0,1;");
            while(rs.next()) {
                result[0] = rs.getInt("kills");
                result[1] = rs.getInt("deaths");
                result[2] = rs.getInt("wins");
            }
        } catch(SQLException e) {
            pl.getLogger().log(Level.WARNING, "Couldn't get player stats: " + e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    public void generateRanking() {
        new BukkitRunnable() {

            @Override
            public void run() {
                try {
                    ResultSet rs = getConn().createStatement().executeQuery("SELECT * FROM `sgames` ORDER BY `wins`, `kills` DESC LIMIT 0, " + pl.config.rankingEntries + ";");
                    ranking.clear();
                    while(rs.next()) {
                        int[] values = {rs.getInt("kills"), rs.getInt("deaths"), rs.getInt("wins")};
                        ranking.add(new RankingEntry(pl, rs.getString("name"), values));
                    }
                } catch(SQLException ex) {
                    pl.getLogger().log(Level.WARNING, "Couldn't generate ranking: " + ex.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(pl, 20, (20 * 60) * rankingdelay);
    }

    public LinkedList<RankingEntry> getRankingEntries() {
        return ranking;
    }
}
