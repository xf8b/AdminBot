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

import lombok.Cleanup;
import lombok.experimental.UtilityClass;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class AdministratorsDatabaseHelper {
    public void add(String guildId, String administratorRoleId, int level) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO administrators VALUES (?, ?, ?);");

        preparedStatement.setString(1, guildId);
        preparedStatement.setString(2, administratorRoleId);
        preparedStatement.setInt(3, level);
        preparedStatement.addBatch();

        connection.setAutoCommit(false);
        preparedStatement.executeBatch();
        connection.setAutoCommit(true);
    }

    public void remove(String guildId, String administratorRoleId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM administrators WHERE guildId = ? AND administratorRoleId = ?;");
        preparedStatement.setString(1, guildId);
        preparedStatement.setString(2, administratorRoleId);
        preparedStatement.addBatch();

        connection.setAutoCommit(false);
        preparedStatement.executeUpdate();
        connection.setAutoCommit(true);
    }

    public Map<String, Integer> getAllRoles(String guildId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT administratorRoleId, level FROM administrators WHERE guildId = ?;");
        preparedStatement.setString(1, guildId);
        preparedStatement.addBatch();

        @Cleanup
        ResultSet resultSet = preparedStatement.executeQuery();
        Map<String, Integer> allowedRoles = new HashMap<>();
        while (resultSet.next()) {
            allowedRoles.put("<@&" + resultSet.getString("administratorRoleId") + ">", resultSet.getInt("level"));
        }

        return allowedRoles;
    }

    public boolean isRoleInDatabase(String guildId, String administratorRoleId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT administratorRoleId FROM administrators WHERE guildId = ? AND administratorRoleId = ?;");
        preparedStatement.setString(1, guildId);
        preparedStatement.setString(2, administratorRoleId);
        preparedStatement.addBatch();

        @Cleanup
        ResultSet resultSet = preparedStatement.executeQuery();

        return resultSet.next();
    }

    public int getLevelOfRole(String guildId, String administratorRoleId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT level FROM administrators WHERE guildId = ? AND administratorRoleId = ?;");
        preparedStatement.setString(1, guildId);
        preparedStatement.setString(2, administratorRoleId);
        preparedStatement.addBatch();

        @Cleanup
        ResultSet resultSet = preparedStatement.executeQuery();

        return resultSet.getInt("level");
    }
}
