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

@UtilityClass
public class PrefixesDatabaseHelper {
    public static void insertIntoPrefixes(String guildId, String prefix) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        PreparedStatement prep = connection.prepareStatement("insert into prefixes values (?, ?);");

        prep.setString(1, guildId);
        prep.setString(2, prefix);
        prep.addBatch();

        connection.setAutoCommit(false);
        prep.executeBatch();
        connection.setAutoCommit(true);
        connection.close();
        statement.close();
        prep.close();
    }

    public static String readFromPrefixes(String guildId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        PreparedStatement prep = connection.prepareStatement("select prefix from prefixes where guildId = ?;");
        prep.setString(1, guildId);
        prep.addBatch();
        ResultSet resultSet = prep.executeQuery();

        String prefix = resultSet.getString("prefix");

        connection.setAutoCommit(false);
        connection.setAutoCommit(true);

        resultSet.close();
        connection.close();
        statement.close();
        prep.close();
        return prefix;
    }

    public static void overwritePrefixForGuild(String guildId, String prefix) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        PreparedStatement prep = connection.prepareStatement("update prefixes set prefix = ? where guildId = ?;");
        prep.setString(1, prefix);
        prep.setString(2, guildId);
        prep.addBatch();

        connection.setAutoCommit(false);
        prep.executeUpdate();
        connection.setAutoCommit(true);
        connection.close();
        statement.close();
        prep.close();
    }

    public static boolean doesGuildNotExistInDatabase(String guildId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        PreparedStatement prep = connection.prepareStatement("select prefix from prefixes where guildId = ?;");
        prep.setString(1, guildId);
        prep.addBatch();
        ResultSet resultSet = prep.executeQuery();

        boolean doesExist = resultSet.next();

        connection.setAutoCommit(false);
        connection.setAutoCommit(true);
        connection.close();
        statement.close();
        prep.close();
        resultSet.close();
        return !doesExist;
    }
}