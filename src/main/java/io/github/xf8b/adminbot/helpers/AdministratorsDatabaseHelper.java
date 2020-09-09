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

import discord4j.common.util.Snowflake;
import io.github.xf8b.adminbot.data.GuildData;
import lombok.Cleanup;
import lombok.experimental.UtilityClass;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class AdministratorsDatabaseHelper {
    /**
     * adds a role to the database
     *
     * @param guildId             the id of the guild
     * @param administratorRoleId the id of the role
     * @param level               the administrator level for the role
     * @throws SQLException when a sql exception happens
     * @deprecated use {@link GuildData#addAdministratorRole(Snowflake, int)}
     */
    @Deprecated
    public static void add(long guildId, long administratorRoleId, int level) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO administrators VALUES (?, ?, ?);");

        preparedStatement.setLong(1, guildId);
        preparedStatement.setLong(2, administratorRoleId);
        preparedStatement.setInt(3, level);
        preparedStatement.addBatch();

        connection.setAutoCommit(false);
        preparedStatement.executeBatch();
        connection.setAutoCommit(true);
    }

    /**
     * removes the role from the database
     *
     * @param guildId             the id of the guild
     * @param administratorRoleId the role to remove
     * @throws SQLException when a sql exception happens
     * @deprecated use {@link GuildData#removeAdministratorRole(Snowflake)}
     */
    @Deprecated
    public static void remove(long guildId, long administratorRoleId) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM administrators WHERE guildId = ? AND administratorRoleId = ?;");
        preparedStatement.setLong(1, guildId);
        preparedStatement.setLong(2, administratorRoleId);
        preparedStatement.addBatch();

        connection.setAutoCommit(false);
        preparedStatement.executeUpdate();
        connection.setAutoCommit(true);
    }

    /**
     * gets all roles in the guild
     *
     * @param guildId the id of the guild
     * @return all the roles in the guild
     * @throws SQLException when a sql exception happens
     * @deprecated use {@link GuildData#getAdministratorRoles()}
     */
    @Deprecated
    public static Map<Long, Integer> getAllRoles(long guildId) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT administratorRoleId, level FROM administrators WHERE guildId = ?;");
        preparedStatement.setLong(1, guildId);
        preparedStatement.addBatch();

        @Cleanup
        ResultSet resultSet = preparedStatement.executeQuery();
        Map<Long, Integer> allowedRoles = new HashMap<>();

        while (resultSet.next()) {
            allowedRoles.put(resultSet.getLong("administratorRoleId"), resultSet.getInt("level"));
        }

        return allowedRoles;
    }

    /**
     * checks if the role exists in the database
     *
     * @param guildId             the id of the guild
     * @param administratorRoleId the id of the role
     * @return if the role exists in the database
     * @throws SQLException when a sql exception happens
     * @deprecated use {@link io.github.xf8b.adminbot.data.GuildData#hasAdministratorRole(Snowflake)}
     */
    @Deprecated
    public static boolean isRoleInDatabase(long guildId, long administratorRoleId) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT administratorRoleId FROM administrators WHERE guildId = ? AND administratorRoleId = ?;");
        preparedStatement.setLong(1, guildId);
        preparedStatement.setLong(2, administratorRoleId);
        preparedStatement.addBatch();

        @Cleanup
        ResultSet resultSet = preparedStatement.executeQuery();

        return resultSet.next();
    }

    /**
     * gets the level of the role passed in
     *
     * @param guildId             the id of the guild
     * @param administratorRoleId the id of the role
     * @return the level of the role
     * @throws SQLException when a sql exception happens
     * @deprecated use {@link io.github.xf8b.adminbot.data.GuildData#getLevelOfAdministratorRole(Snowflake)}
     */
    @Deprecated
    public static int getLevelOfRole(long guildId, long administratorRoleId) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/administrators.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS administrators (guildId, administratorRoleId, level);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT level FROM administrators WHERE guildId = ? AND administratorRoleId = ?;");
        preparedStatement.setLong(1, guildId);
        preparedStatement.setLong(2, administratorRoleId);
        preparedStatement.addBatch();

        @Cleanup
        ResultSet resultSet = preparedStatement.executeQuery();

        return resultSet.getInt("level");
    }
}
