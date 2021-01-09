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

package io.github.xf8b.xf8bot.database.actions.update

import io.github.xf8b.xf8bot.database.DatabaseAction
import io.r2dbc.spi.Connection
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

open class UpdateAction(
    override val table: String,
    private val setFields: Map<String, *>,
    private val criteria: Map<String, *>
) : DatabaseAction<Void> {
    private fun update(connection: Connection, fields: Map<String, *>, updateCriteria: Map<String, *>): Mono<Void> {
        var sql = "UPDATE $table SET "
        val indexedParametersForSet = mutableListOf<String>()
        val indexedParametersForCriteria = mutableListOf<String>()

        for (i in 1..fields.size) {
            indexedParametersForSet.add("${fields.keys.toList()[i - 1]} = $$i")
        }

        for (i in fields.size until (updateCriteria.size + fields.size)) {
            indexedParametersForCriteria.add("${updateCriteria.keys.toList()[i - 1]} = $$i")
        }

        sql += indexedParametersForSet.joinToString()
        sql += " WHERE "
        sql += indexedParametersForCriteria.joinToString(separator = " AND ")

        return connection.createStatement(sql)
            .apply {
                for (i in 1 until (indexedParametersForSet + indexedParametersForCriteria).size) {
                    bind("$$i", updateCriteria.values.toList()[i - 1]!!)
                }
            }
            .execute()
            .toFlux()
            .flatMap { it.rowsUpdated.toMono() }
            .then()
    }

    override fun run(connection: Connection): Mono<Void> = update(connection, setFields, criteria)

    /*
    override fun runEncrypted(connection: Connection, keySetHandle: KeysetHandle): Mono<Void> {
        val primitive: Aead = keySetHandle.getPrimitive(Aead::class.java)

        val encryptedSetFields = setFields.mapKeys {
            primitive.encrypt(it.toString().toByteArray(), null).decodeToString()
        }
        val encryptedCriteria = criteria.mapValues { (_, value) ->
            primitive.encrypt(value.toString().toByteArray(), null).decodeToString()
        }

        return update(connection, encryptedSetFields, encryptedCriteria)
    }
    */
}