package io.github.xf8b.adminbot.helper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import javax.annotation.Nullable;
import java.sql.*;

public class WarnsDatabaseHelper {
    public static void insertIntoWarns(String guildId, String userId, String warnId, String reason) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/warns.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS warns (guildId, userId, warnId, reason);");
        PreparedStatement prep = conn.prepareStatement("insert into warns values (?, ?, ?, ?);");

        prep.setString(1, guildId);
        prep.setString(2, userId);
        prep.setString(3, warnId);
        prep.setString(4, reason);
        prep.addBatch();

        conn.setAutoCommit(false);
        prep.executeBatch();
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
    }

    public static void removeWarnsFromUserForGuild(String guildId, String userId, @Nullable String warnId, @Nullable String reason) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/warns.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS warns (guildId, userId, warnId, reason);");
        boolean removeAllWarns = false;
        if (reason == null) {
            removeAllWarns = true;
        }
        boolean removeDuplicateWarns = false;
        if (warnId == null) {
            removeDuplicateWarns = true;
        }
        if (removeAllWarns) {
            PreparedStatement prep;
            if (removeDuplicateWarns) {
                prep = conn.prepareStatement("delete from warns where guildId = ? and userId = ?;");
                prep.setString(1, guildId);
                prep.setString(2, userId);
            } else {
                prep = conn.prepareStatement("delete from warns where guildId = ? and userId = ? and warnId = ?;");
                prep.setString(1, guildId);
                prep.setString(2, userId);
                prep.setString(3, warnId);

            }
            prep.addBatch();
            conn.setAutoCommit(false);
            prep.executeUpdate();
            conn.setAutoCommit(true);
            prep.close();
        } else {
            PreparedStatement prep;
            if (removeDuplicateWarns) {
                prep = conn.prepareStatement("delete from warns where guildId = ? and userId = ? and reason = ?;");
                prep.setString(1, guildId);
                prep.setString(2, userId);
                prep.setString(3, reason);

            } else {
                prep = conn.prepareStatement("delete from warns where guildId = ? and userId = ? and warnId = ? and reason = ?;");
                prep.setString(1, guildId);
                prep.setString(2, userId);
                prep.setString(3, warnId);
                prep.setString(4, reason);

            }
            prep.addBatch();
            conn.setAutoCommit(false);
            prep.executeUpdate();
            conn.setAutoCommit(true);
            prep.close();
        }

        conn.close();
        stat.close();
    }

    public static boolean doesUserHaveWarn(String guildId, String userId, String reason) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/warns.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS warns (guildId, userId, warnId, reason);");
        PreparedStatement prep = conn.prepareStatement("select reason from warns where guildId = ? and userId = ? and reason = ?;");
        prep.setString(1, guildId);
        prep.setString(2, userId);
        prep.setString(3, reason);
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

    public static Multimap<String, String> getAllWarnsForUser(String guildId, String userId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/warns.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS warns (guildId, userId, warnId, reason);");
        PreparedStatement prep = conn.prepareStatement("select reason, warnId from warns where guildId = ? and userId = ?;");
        prep.setString(1, guildId);
        prep.setString(2, userId);
        prep.addBatch();
        ResultSet rs = prep.executeQuery();
        Multimap<String, String> warns = ArrayListMultimap.create();

        while (rs.next()) {
            warns.put(rs.getString("reason"), rs.getString("warnId"));
        }

        conn.setAutoCommit(false);
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
        rs.close();
        return warns;
    }
}
