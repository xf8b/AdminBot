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
import io.github.xf8b.adminbot.data.MemberData;
import io.github.xf8b.adminbot.data.WarnContext;
import lombok.Cleanup;
import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class WarnsDatabaseHelper {
    /**
     * adds a warn to the database
     *
     * @param guildId           the guild id
     * @param userId            the user id
     * @param memberWhoWarnedId the member who warned's id
     * @param warnId            the warn id
     * @param reason            the reason for the warn
     * @throws SQLException when a sql exception happens
     * @deprecated use {@link MemberData#addWarn(Snowflake, int, String)}
     */
    @Deprecated
    public static void add(long guildId, long userId, long memberWhoWarnedId, int warnId, String reason) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/warns.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS warns (guildId, userId, memberWhoWarnedId, warnId, reason);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO warns VALUES (?, ?, ?, ?, ?);");

        preparedStatement.setLong(1, guildId);
        preparedStatement.setLong(2, userId);
        preparedStatement.setLong(3, memberWhoWarnedId);
        preparedStatement.setInt(4, warnId);
        preparedStatement.setString(5, reason);
        preparedStatement.addBatch();

        connection.setAutoCommit(false);
        preparedStatement.executeBatch();
        connection.setAutoCommit(true);
        connection.close();
    }

    /**
     * removes a warn from the database
     *
     * @param guildId           the guild id
     * @param userId            the user id
     * @param memberWhoWarnedId the member who warned's id (will remove all warns with same warnId and reason if {@code -1})
     * @param warnId            the warn id (will remove duplicate warns if {@code -1})
     * @param reason            the warn reason (will remove all warns if null)
     * @throws SQLException when a sql exception happens
     * @deprecated use {@link MemberData#removeWarn(Snowflake, int, String)}
     */
    //TODO: fix if there is an error
    @Deprecated
    public static void remove(long guildId, long userId, long memberWhoWarnedId, int warnId, @Nullable String reason) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/warns.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS warns (guildId, userId, memberWhoWarnedId, warnId, reason);");
        boolean removeAllWarns = reason == null;
        boolean removeDuplicateWarns = warnId == -1;
        boolean removeWarnsRegardlessOfAuthor = memberWhoWarnedId == -1L;
        StringBuilder statementBuilder = new StringBuilder("DELETE FROM warns WHERE guildId = ? AND userId = ?");
        if (!removeAllWarns) {
            statementBuilder.append(" AND reason = ?");
        }
        if (!removeDuplicateWarns) {
            statementBuilder.append(" AND warnId = ?");
        }
        if (!removeWarnsRegardlessOfAuthor) {
            statementBuilder.append(" AND memberWhoWarnedId = ?");
        }
        statementBuilder.append(";");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement(statementBuilder.toString());
        preparedStatement.setLong(1, guildId);
        preparedStatement.setLong(2, userId);
        if (!removeAllWarns && !removeDuplicateWarns && !removeWarnsRegardlessOfAuthor) {
            preparedStatement.setString(3, reason);
            preparedStatement.setInt(4, warnId);
            preparedStatement.setLong(5, memberWhoWarnedId);
        } else if (!removeAllWarns && !removeDuplicateWarns) {
            preparedStatement.setString(3, reason);
            preparedStatement.setInt(4, warnId);
        } else if (!removeAllWarns) {
            preparedStatement.setString(3, reason);
        } else if (!removeDuplicateWarns) {
            preparedStatement.setInt(3, warnId);
        } else if (!removeWarnsRegardlessOfAuthor) {
            preparedStatement.setLong(3, memberWhoWarnedId);
        }
        preparedStatement.addBatch();
        connection.setAutoCommit(false);
        preparedStatement.executeUpdate();
        connection.setAutoCommit(true);
    }

    /**
     * checks if a member has a warn with the specified reason
     *
     * @param guildId the guild id
     * @param userId  the user id
     * @param reason  the reason
     * @return if the member has the warn
     * @throws SQLException when a sql exception happens
     * @deprecated use {@link MemberData#hasWarn(String)}
     */
    @Deprecated
    public static boolean hasWarn(long guildId, long userId, String reason) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/warns.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS warns (guildId, userId, memberWhoWarnedId, warnId, reason);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT reason FROM warns WHERE guildId = ? AND userId = ? AND reason = ?;");
        preparedStatement.setLong(1, guildId);
        preparedStatement.setLong(2, userId);
        preparedStatement.setString(3, reason);
        preparedStatement.addBatch();
        @Cleanup
        ResultSet resultSet = preparedStatement.executeQuery();

        return resultSet.next();
    }

    /**
     * gets the warns for the user
     *
     * @param guildId the guild id
     * @param userId  the user id
     * @return all the warns for the user
     * @throws SQLException when a sql exception happens
     * @deprecated use {@link MemberData#getWarns()}
     */
    @Deprecated
    public static List<WarnContext> getWarnsForUser(long guildId, long userId) throws SQLException {
        @Cleanup
        Connection connection = DriverManager.getConnection("jdbc:sqlite:databases/warns.db");
        @Cleanup
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS warns (guildId, userId, memberWhoWarnedId, warnId, reason);");
        @Cleanup
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT reason, warnId, memberWhoWarnedId FROM warns WHERE guildId = ? AND userId = ?;");
        preparedStatement.setLong(1, guildId);
        preparedStatement.setLong(2, userId);
        preparedStatement.addBatch();
        @Cleanup
        ResultSet resultSet = preparedStatement.executeQuery();
        List<WarnContext> warns = new ArrayList<>();

        while (resultSet.next()) {
            warns.add(new WarnContext(
                    Snowflake.of(resultSet.getString("memberWhoWarnedId")),
                    resultSet.getString("reason"),
                    Integer.parseInt(resultSet.getString("warnId"))
            ));
        }

        return warns;
    }
}
