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

package io.github.xf8b.xf8bot.database.actions.delete

import io.github.xf8b.xf8bot.database.DatabaseAction
import io.r2dbc.spi.Connection
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

open class DeleteAction(
    override val table: String,
    private val criteria: Map<String, *>
) : DatabaseAction<Mono<out Void>> {
    private fun internalDelete(connection: Connection, criteria: Map<String, *>): Mono<out Void> {
        var sql = """DELETE FROM "$table" WHERE """
        val indexedParameters = mutableListOf<String>()

        for (i in 1..criteria.size) {
            indexedParameters.add("${criteria.keys.toList()[i - 1]} = $$i")
        }

        sql += indexedParameters.joinToString(separator = " AND ")

        return connection.createStatement(sql)
            .apply {
                for (i in 1..indexedParameters.size) {
                    bind("$$i", criteria.values.toList()[i - 1]!!)
                }
            }
            .execute()
            .toFlux()
            .flatMap { it.rowsUpdated.toMono() }
            .then()
    }

    override fun invoke(connection: Connection) = internalDelete(connection, criteria)

    /*
    override fun runEncrypted(connection: Connection, keySetHandle: KeysetHandle): Mono<Void> {
        val primitive: Aead = keySetHandle.getPrimitive(Aead::class.java)

        val encryptedCriteria = criteria.mapValues { (_, value) ->
            primitive.encrypt(value.toString().toByteArray(), null).decodeToString()
        }

        return delete(connection, encryptedCriteria)
    }
    */
}