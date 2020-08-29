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

@UtilityClass
public class PrefixesDatabaseHelper {
    public static void add(String guildId, String prefix) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        @Cleanup
        PreparedStatement prep = connection.prepareStatement("INSERT INTO prefixes VALUES (?, ?);");

        prep.setString(1, guildId);
        prep.setString(2, prefix);
        prep.addBatch();

        connection.setAutoCommit(false);
        prep.executeBatch();
        connection.setAutoCommit(true);
        prep.close();
    }

    public static String get(String guildId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        @Cleanup
        PreparedStatement prep = connection.prepareStatement("SELECT prefix FROM prefixes WHERE guildId = ?;");
        prep.setString(1, guildId);
        prep.addBatch();
        @Cleanup
        ResultSet resultSet = prep.executeQuery();

        return resultSet.getString("prefix");
    }

    public static void overwrite(String guildId, String prefix) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        @Cleanup
        PreparedStatement prep = connection.prepareStatement("UPDATE prefixes SET prefix = ? WHERE guildId = ?;");
        prep.setString(1, prefix);
        prep.setString(2, guildId);
        prep.addBatch();

        connection.setAutoCommit(false);
        prep.executeUpdate();
        connection.setAutoCommit(true);
        prep.close();
    }

    public static boolean isNotInDatabase(String guildId) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/prefixes.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS prefixes (guildId, prefix);");
        @Cleanup
        PreparedStatement prep = connection.prepareStatement("SELECT prefix FROM prefixes WHERE guildId = ?;");
        prep.setString(1, guildId);
        prep.addBatch();
        @Cleanup
        ResultSet resultSet = prep.executeQuery();

        boolean doesExist = resultSet.next();

        return !doesExist;
    }
}