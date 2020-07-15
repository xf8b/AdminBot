package io.github.xf8b.adminbot.helper;

import java.sql.*;
import java.util.ArrayList;

public class AdministratorsDatabaseHelper {
    public static void addToAdministrators(String guildId, String administratorRoleId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId);");
        PreparedStatement prep = conn.prepareStatement("insert into administrators values (?, ?);");

        prep.setString(1, guildId);
        prep.setString(2, administratorRoleId);
        prep.addBatch();

        conn.setAutoCommit(false);
        prep.executeBatch();
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
    }

    public static void removeFromAdministrators(String guildId, String administratorRoleId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId);");
        PreparedStatement prep = conn.prepareStatement("delete from administrators where guildId = ? and administratorRoleId = ?;");
        prep.setString(1, guildId);
        prep.setString(2, administratorRoleId);
        prep.addBatch();

        conn.setAutoCommit(false);
        prep.executeUpdate();
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
    }

    public static ArrayList<String> getAllAdministratorsForGuild(String guildId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId);");
        PreparedStatement prep = conn.prepareStatement("select administratorRoleId from administrators where guildId = ?;");
        prep.setString(1, guildId);
        prep.addBatch();

        ResultSet rs = prep.executeQuery();
        ArrayList<String> allowedRoles = new ArrayList<>();
        while (rs.next()) {
            allowedRoles.add("<@&" + rs.getString("administratorRoleId") + ">");
        }

        conn.setAutoCommit(false);
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
        rs.close();
        return allowedRoles;
    }

    public static boolean doesAdministratorRoleExistInDatabase(String guildId, String administratorRoleId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId);");
        PreparedStatement prep = conn.prepareStatement("select administratorRoleId from administrators where guildId = ? and administratorRoleId = ?;");
        prep.setString(1, guildId);
        prep.setString(2, administratorRoleId);
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
