/*
 * Copyright (c) 2020 xf8b.
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

package io.github.xf8b.xf8bot.database.actions.find

import io.github.xf8b.xf8bot.database.DatabaseAction
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Result
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.toFlux


open class SelectAction(
    override val table: String,
    private val selectedFields: List<String>,
    private val criteria: Map<String, *>
) : DatabaseAction<List<Result>> {
    private fun select(
        connection: Connection,
        fields: List<String>,
        selectCriteria: Map<String, *>,
        // keySetHandle: KeysetHandle? = null
    ): Mono<List<Result>> {
        var sql = """SELECT ${fields.joinToString()} FROM "$table" WHERE """
        val indexedParameters = selectCriteria.keys.mapIndexed { index, key -> "$key = $${index + 1}" }

        sql += indexedParameters.joinToString(separator = " AND ")

        return connection.createStatement(sql)
            .apply {
                indexedParameters.indices.forEach {
                    bind("$${it + 1}", selectCriteria.values.toList()[it]!!)
                }
            }
            .execute()
            .toFlux()
            /*
            .flatMap { it.map { row, _ -> row } }
            .map {
                // TODO: figure out how to decrypt this properly
                if (keySetHandle != null) {
                    val primitive: Aead = keySetHandle.getPrimitive(Aead::class.java)

                    fields.map { field ->
                        field to primitive.decrypt(it[field, String::class.java]!!.encodeToByteArray(), null).toString()
                    }.toMap()
                } else {
                    fields.map { field -> field to it[field] }.toMap()
                }
            }
            */
            .collectList()
            .cast()
    }

    override fun run(connection: Connection) = select(connection, selectedFields, criteria)

    /*
    override fun runEncrypted(connection: Connection, keySetHandle: KeysetHandle): Mono<List<Result>> {
        val primitive: Aead = keySetHandle.getPrimitive(Aead::class.java)

        val encryptedSelectedFields = selectedFields.map {
            primitive.encrypt(it.toByteArray(), null).decodeToString()
        }
        val encryptedCriteria = criteria.mapValues { (_, value) ->
            primitive.encrypt(value.toString().toByteArray(), null).decodeToString()
        }

        return select(connection, encryptedSelectedFields, encryptedCriteria, keySetHandle)
    }
    */
}