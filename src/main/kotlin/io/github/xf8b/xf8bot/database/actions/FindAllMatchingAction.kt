/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of xf8bot.
 *
 * xf8bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xf8bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with xf8bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.xf8bot.database.actions

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.mongodb.reactivestreams.client.FindPublisher
import com.mongodb.reactivestreams.client.MongoCollection
import io.github.xf8b.xf8bot.database.DatabaseAction
import org.bson.Document
import org.bson.conversions.Bson

open class FindAllMatchingAction(
    override val collectionName: String,
    val filter: Bson
) : DatabaseAction<FindPublisher<Document>> {
    override fun run(
        collection: MongoCollection<Document>,
        keySetHandle: KeysetHandle?,
        encrypted: Boolean
    ): FindPublisher<Document> {
        var encryptedFilter: Bson = Document()

        if (encrypted) {
            val aead: Aead = keySetHandle!!.getPrimitive(Aead::class.java)
            val values = filter.toBsonDocument(Document::class.java, null).entries
            values.stream().forEach {
                val encryptedValue = aead.encrypt(
                    it.value.asString().value.toByteArray(), null
                ).toString(Charsets.UTF_8)//TODO add associated data
                (encryptedFilter as Document).append(it.key, encryptedValue)
            }
        } else {
            encryptedFilter = filter
        }

        return collection.find(encryptedFilter)
    }
}