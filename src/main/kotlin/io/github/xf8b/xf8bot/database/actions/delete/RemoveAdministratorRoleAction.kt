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

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoCollection
import discord4j.common.util.Snowflake
import io.github.xf8b.xf8bot.database.DatabaseAction
import org.bson.Document
import org.reactivestreams.Publisher

class RemoveAdministratorRoleAction(
    val guildId: Snowflake,
    val roleId: Snowflake
) : DatabaseAction<Publisher<Document>> {
    override val collectionName: String
        get() = "administratorRoles"

    override fun run(
        collection: MongoCollection<Document>,
        keySetHandle: KeysetHandle?,
        encrypted: Boolean
    ): Publisher<Document> {
        if (encrypted) {
            val aead: Aead = keySetHandle!!.getPrimitive(Aead::class.java)
            val encryptedRoleId =
                aead.encrypt(roleId.asString().toByteArray(), null).toString(Charsets.UTF_8)//TODO add associated data
            val encryptedGuildId =
                aead.encrypt(guildId.asString().toByteArray(), null).toString(Charsets.UTF_8)//TODO add associated data

            return collection.findOneAndDelete(
                Filters.and(
                    Filters.eq("roleId", encryptedRoleId),
                    Filters.eq("guildId", encryptedGuildId)
                )
            )
        } else {
            return collection.findOneAndDelete(
                Filters.and(
                    Filters.eq("roleId", roleId.asLong()),
                    Filters.eq("guildId", guildId.asLong())
                )
            )
        }
    }
}