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

import com.google.crypto.tink.KeysetHandle
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
    override fun run(
        connection: Connection,
        keySetHandle: KeysetHandle?
    ): Mono<List<Result>> {
        var sql = """SELECT ${selectedFields.joinToString()} FROM "$table" WHERE """
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
            .collectList()
            .cast()
    }
}