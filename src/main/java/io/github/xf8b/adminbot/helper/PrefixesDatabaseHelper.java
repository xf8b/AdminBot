package io.github.xf8b.adminbot.helper;

import java.sql.*;

public class PrefixesDatabaseHelper {
    public static void insertIntoPrefixes(String guildId, String prefix) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        PreparedStatement prep = conn.prepareStatement("insert into prefixes values (?, ?);");

        prep.setString(1, guildId);
        prep.setString(2, prefix);
        prep.addBatch();

        conn.setAutoCommit(false);
        prep.executeBatch();
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
    }

    public static String readFromPrefixes(String guildId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        PreparedStatement prep = conn.prepareStatement("select prefix from prefixes where guildId = ?;");
        prep.setString(1, guildId);
        prep.addBatch();
        ResultSet rs = prep.executeQuery();

        String prefix = rs.getString("prefix");

        conn.setAutoCommit(false);
        conn.setAutoCommit(true);

        rs.close();
        conn.close();
        stat.close();
        prep.close();
        return prefix;
    }

    public static void overwritePrefixForGuild(String guildId, String prefix) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        PreparedStatement prep = conn.prepareStatement("update prefixes set prefix = ? where guildId = ?;");
        prep.setString(1, prefix);
        prep.setString(2, guildId);
        prep.addBatch();

        conn.setAutoCommit(false);
        prep.executeUpdate();
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
    }

    public static boolean doesGuildExistInDatabase(String guildId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        PreparedStatement prep = conn.prepareStatement("select prefix from prefixes where guildId = ?;");
        prep.setString(1, guildId);
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
}