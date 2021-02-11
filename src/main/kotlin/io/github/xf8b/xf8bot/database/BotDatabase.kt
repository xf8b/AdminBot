/*
 * Copyright (c) 2020, 2021 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.database

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ValidationDepth
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

// FIXME revert back to Mono<List> instead of Flux
class BotDatabase(private val connectionPool: ConnectionPool/*, private val keySetHandle: KeysetHandle?*/) : Database {
    init {
        execute { connection ->
            connection.createStatement(
                """
                CREATE TABLE IF NOT EXISTS "administratorRoles" (
                --  name        type       nullability  key type
                    guildId     bigint     NOT NULL,
                    roleId      bigint     NOT NULL     PRIMARY KEY,
                    level       int        NOT NULL -- administrator level for the role, ranges from 1 to 4
                );
                CREATE TABLE IF NOT EXISTS "warns" (
                --  name         type       nullability  key type
                    guildId      bigint     NOT NULL,
                    memberId     bigint     NOT NULL,
                    warnerId     bigint     NOT NULL,
                    warnId       uuid       NOT NULL     PRIMARY KEY, -- unique id for the warn
                    reason       text       NOT NULL
                );
                CREATE TABLE IF NOT EXISTS "disabledCommands" (
                --  name        type       nullability
                    guildId     bigint     NOT NULL,
                    command     text       NOT NULL
                );
                CREATE TABLE IF NOT EXISTS "experience" ( 
                --  name         type       nullability
                    guildId      bigint     NOT NULL,
                    memberId     bigint     NOT NULL,
                    xp           bigint     NOT NULL
                );
                CREATE TABLE IF NOT EXISTS "prefixes" (
                --  name        type       nullability  key type
                    guildId     bigint     NOT NULL     PRIMARY KEY,
                    prefix      text       NOT NULL
                );
                """.trimIndent()
            ).execute().toMono().flatMap { it.rowsUpdated.toMono() }.then()
        }.toMono().block()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Mono<*>> execute(action: DatabaseAction<T>): T = connectionPool.create().flatMap { connection ->
        action(connection).flatMap { result: Any ->
            connection.validate(ValidationDepth.LOCAL).toMono().flatMap { connected ->
                if (connected) connection.close().toMono().thenReturn(result)
                else result.toMono()
            }
        }
    } as T

    @Suppress("UNCHECKED_CAST")
    override fun <T : Flux<*>> execute(action: DatabaseAction<T>): T =
        connectionPool.create().flatMapMany { connection ->
            action(connection).flatMap { result: Any ->
                connection.validate(ValidationDepth.LOCAL).toMono().flatMap { connected ->
                    if (connected) connection.close().toMono().thenReturn(result)
                    else result.toMono()
                }
            }
        } as T

    fun <T : Mono<*>> execute(action: (Connection) -> T): T = execute(object : DatabaseAction<T> {
        override val table: String
            get() = error("No table")

        override fun invoke(connection: Connection): T {
            return action(connection)
        }
    })
}