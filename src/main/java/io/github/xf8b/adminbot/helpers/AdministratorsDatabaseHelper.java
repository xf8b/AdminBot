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

import lombok.experimental.UtilityClass;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class AdministratorsDatabaseHelper {
    public void addToAdministrators(String guildId, String administratorRoleId, int level) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        PreparedStatement prep = conn.prepareStatement("insert into administrators values (?, ?, ?);");

        prep.setString(1, guildId);
        prep.setString(2, administratorRoleId);
        prep.setInt(3, level);
        prep.addBatch();

        conn.setAutoCommit(false);
        prep.executeBatch();
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
    }

    public void removeFromAdministrators(String guildId, String administratorRoleId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
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

    public Map<String, Integer> getAllAdministratorsForGuild(String guildId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        PreparedStatement prep = conn.prepareStatement("select administratorRoleId, level from administrators where guildId = ?;");
        prep.setString(1, guildId);
        prep.addBatch();

        ResultSet rs = prep.executeQuery();
        Map<String, Integer> allowedRoles = new HashMap<>();
        while (rs.next()) {
            allowedRoles.put("<@&" + rs.getString("administratorRoleId") + ">", rs.getInt("level"));
        }

        conn.setAutoCommit(false);
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
        rs.close();
        return allowedRoles;
    }

    public boolean doesAdministratorRoleExistInDatabase(String guildId, String administratorRoleId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
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

    public int getLevelOfAdministratorRole(String guildId, String administratorRoleId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        Statement stat = conn.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        PreparedStatement prep = conn.prepareStatement("select level from administrators where guildId = ? and administratorRoleId = ?;");
        prep.setString(1, guildId);
        prep.setString(2, administratorRoleId);
        prep.addBatch();

        ResultSet rs = prep.executeQuery();
        int doesExist = rs.getInt("level");

        conn.setAutoCommit(false);
        conn.setAutoCommit(true);
        conn.close();
        stat.close();
        prep.close();
        rs.close();
        return doesExist;
    }
}
