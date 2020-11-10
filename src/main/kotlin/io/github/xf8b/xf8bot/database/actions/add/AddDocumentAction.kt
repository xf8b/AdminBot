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

package io.github.xf8b.xf8bot.database.actions.add

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.mongodb.client.result.InsertOneResult
import com.mongodb.reactivestreams.client.MongoCollection
import io.github.xf8b.xf8bot.database.DatabaseAction
import org.bson.Document
import org.reactivestreams.Publisher
import kotlin.text.Charsets.UTF_8

open class AddDocumentAction(
    override val collectionName: String,
    val document: Document
) : DatabaseAction<Publisher<InsertOneResult>> {
    override fun run(
        collection: MongoCollection<Document>,
        keySetHandle: KeysetHandle?,
        encrypted: Boolean
    ): Publisher<InsertOneResult> {
        var encryptedDocument = Document()

        if (encrypted) {
            val aead: Aead = keySetHandle!!.getPrimitive(Aead::class.java)
            val values = document.entries
            values.stream().forEach {
                val encryptedValue =
                    aead.encrypt((it.value as String).toByteArray(), null).toString(UTF_8)//TODO add associated data
                encryptedDocument.append(it.key, encryptedValue)
            }
        } else {
            encryptedDocument = document
        }

        return collection.insertOne(encryptedDocument)
    }
}
