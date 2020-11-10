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

package io.github.xf8b.xf8bot.database.actions.delete

import com.google.crypto.tink.KeysetHandle
import com.mongodb.client.result.DeleteResult
import com.mongodb.reactivestreams.client.MongoCollection
import io.github.xf8b.xf8bot.database.DatabaseAction
import org.bson.Document
import org.reactivestreams.Publisher

class DeleteDocumentAction(
    override val collectionName: String,
    val document: Document
) : DatabaseAction<Publisher<DeleteResult>> {
    override fun run(
        collection: MongoCollection<Document>,
        keySetHandle: KeysetHandle?,
        encrypted: Boolean
    ): Publisher<DeleteResult> = collection.deleteOne(document)
}