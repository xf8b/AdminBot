package io.github.xf8b.adminbot.helper;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class LevelsDatabaseHelper {
    public static void insertIntoLevels(String guildId, String userId, long xp, int level) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/levels.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS levels (guildId, userId, xp, level);");
        PreparedStatement prep = conn.prepareStatement("insert into levels values (?, ?, ?, ?);");

        prep.setString(1, guildId);
        prep.setString(2, userId);
        prep.setLong(3, xp);
        prep.setInt(4, level);
        prep.addBatch();

        conn.setAutoCommit(false);
        prep.executeBatch();
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
    }

    public static void overwriteXP(String guildId, String userId, long xp) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/levels.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS levels (guildId, userId, xp, level);");
        PreparedStatement prep = conn.prepareStatement("update levels set xp = ? where guildId = ? and userId = ?;");
        prep.setLong(1, xp);
        prep.setString(2, guildId);
        prep.setString(3, userId);
        prep.addBatch();

        conn.setAutoCommit(false);
        prep.executeUpdate();
        conn.setAutoCommit(true);

        conn.close();
        stat.close();
        prep.close();
    }

    public static void overwriteLevel(String guildId, String userId, int level) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/levels.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS levels (guildId, userId, xp, level);");
        PreparedStatement prep = conn.prepareStatement("update levels set level = ? where guildId = ? and userId = ?;");
        prep.setLong(1, level);
        prep.setString(2, guildId);
        prep.setString(3, userId);
        prep.addBatch();

        conn.setAutoCommit(false);
        prep.executeUpdate();
        conn.setAutoCommit(true);

        conn.close();
        stat.close();
        prep.close();
    }

    public static boolean isUserInDatabase(String guildId, String userId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/levels.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS levels (guildId, userId, xp, level);");
        PreparedStatement prep = conn.prepareStatement("select level from levels where guildId = ? and userId = ?;");
        prep.setString(1, guildId);
        prep.setString(2, userId);
        prep.addBatch();

        ResultSet rs = prep.executeQuery();

        boolean doesExist = rs.next();

        conn.setAutoCommit(false);
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
        rs.close();
        return doesExist;
    }

    public static long getXPForUser(String guildId, String userId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/levels.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS levels (guildId, userId, xp, level);");
        PreparedStatement prep = conn.prepareStatement("select xp from levels where guildId = ? and userId = ?;");
        prep.setString(1, guildId);
        prep.setString(2, userId);
        prep.addBatch();
        ResultSet rs = prep.executeQuery();

        long level = rs.getLong("xp");

        conn.setAutoCommit(false);
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
        rs.close();
        return level;
    }

    public static int getLevelForUser(String guildId, String userId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/levels.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS levels (guildId, userId, xp, level);");
        PreparedStatement prep = conn.prepareStatement("select level from levels where guildId = ? and userId = ?;");
        prep.setString(1, guildId);
        prep.setString(2, userId);
        prep.addBatch();
        ResultSet rs = prep.executeQuery();

        int level = rs.getInt("level");

        conn.setAutoCommit(false);
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
        rs.close();
        return level;
    }

    public static Map<String, Long> getAllXPForGuild(String guildId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/levels.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS levels (guildId, userId, xp, level);");
        PreparedStatement prep = conn.prepareStatement("select xp, userId from levels where guildId = ?;");
        prep.setString(1, guildId);
        prep.addBatch();
        ResultSet rs = prep.executeQuery();

        Map<String, Long> xps = new HashMap<>();

        while (rs.next()) {
            xps.put(rs.getString("userId"), rs.getLong("xp"));
        }

        conn.setAutoCommit(false);
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
        rs.close();
        return xps;
    }
}
