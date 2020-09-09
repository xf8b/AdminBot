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

import io.github.xf8b.adminbot.data.GuildData;
import lombok.Cleanup;
import lombok.experimental.UtilityClass;

import java.sql.*;

@UtilityClass
public class PrefixesDatabaseHelper {
    /**
     * adds a prefix to the database
     *
     * @param guildId the guild id
     * @param prefix  the prefix to add
     * @throws SQLException when a sql exception happens
     * @deprecated prefixes are automatically added from {@link io.github.xf8b.adminbot.data.GuildData}
     */
    @Deprecated
    public static void add(long guildId, String prefix) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO prefixes VALUES (?, ?);");

        preparedStatement.setLong(1, guildId);
        preparedStatement.setString(2, prefix);
        preparedStatement.addBatch();

        connection.setAutoCommit(false);
        preparedStatement.executeBatch();
        connection.setAutoCommit(true);
        preparedStatement.close();
    }

    /**
     * gets a prefix from the database
     *
     * @param guildId the guild id
     * @return the prefix for the guild
     * @throws SQLException when a sql exception happens
     * @deprecated use {@link GuildData#getPrefix()}
     */
    @Deprecated
    public static String get(long guildId) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT prefix FROM prefixes WHERE guildId = ?;");
        preparedStatement.setLong(1, guildId);
        preparedStatement.addBatch();
        @Cleanup
        ResultSet resultSet = preparedStatement.executeQuery();
        return resultSet.getString("prefix");
    }

    /**
     * overwrites a prefix in the database
     *
     * @param guildId the guild id
     * @param prefix  the new prefix
     * @throws SQLException when a sql exception happens
     * @deprecated use {@link GuildData#setPrefix(String)}
     */
    @Deprecated
    public static void overwrite(long guildId, String prefix) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE prefixes SET prefix = ? WHERE guildId = ?;");
        preparedStatement.setString(1, prefix);
        preparedStatement.setLong(2, guildId);
        preparedStatement.addBatch();

        connection.setAutoCommit(false);
        preparedStatement.executeUpdate();
        connection.setAutoCommit(true);
        preparedStatement.close();
    }

    /**
     * checks if a guild is in the database
     *
     * @param guildId the guild id
     * @return if the guild is in the database
     * @throws SQLException when a sql exception happens
     * @deprecated prefixes are automatically added from {@link io.github.xf8b.adminbot.data.GuildData}
     */
    @Deprecated
    public static boolean isInDatabase(long guildId) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT prefix FROM prefixes WHERE guildId = ?;");
        preparedStatement.setLong(1, guildId);
        preparedStatement.addBatch();
        @Cleanup
        ResultSet resultSet = preparedStatement.executeQuery();

        return resultSet.next();
    }

    /**
     * checks if a guild is not in the database
     *
     * @param guildId the guild id
     * @return if the guild is not in the database
     * @throws SQLException when a sql exception happens
     * @deprecated prefixes are automatically added from {@link io.github.xf8b.adminbot.data.GuildData}
     */
    @Deprecated
    public static boolean isNotInDatabase(long guildId) throws SQLException {
        return !isInDatabase(guildId);
    }
}