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

package io.github.xf8b.xf8bot.database

import com.google.crypto.tink.KeysetHandle
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients

class BotMongoDatabase(
    connectionUrl: String,
    private val databaseName: String,
    private val keySetHandle: KeysetHandle?
) : Database {
    private val client: MongoClient = MongoClients.create(connectionUrl)

    init {
        Runtime.getRuntime().addShutdownHook(Thread { client.close() })
    }

    override fun <T> execute(action: DatabaseAction<T>): T =
        action.run(client.getDatabase(databaseName).getCollection(action.collectionName), keySetHandle)
}