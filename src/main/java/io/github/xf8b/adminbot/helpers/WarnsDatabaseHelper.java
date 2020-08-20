/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of AdminBot.
 *
 * AdminBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminBot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.adminbot.helpers;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;
import java.sql.*;

@UtilityClass
public class WarnsDatabaseHelper {
    public void insertIntoWarns(String guildId, String userId, String warnId, String reason) throws ClassNotFoundException, SQLException {
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

    public void removeWarnsFromUserForGuild(String guildId, String userId, @Nullable String warnId, @Nullable String reason) throws ClassNotFoundException, SQLException {
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

    public boolean doesUserHaveWarn(String guildId, String userId, String reason) throws ClassNotFoundException, SQLException {
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

    public Multimap<String, String> getAllWarnsForUser(String guildId, String userId) throws ClassNotFoundException, SQLException {
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
