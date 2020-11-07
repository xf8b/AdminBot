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

import com.google.crypto.tink.KeysetHandle
import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoCollection
import discord4j.common.util.Snowflake
import io.github.xf8b.xf8bot.database.DatabaseAction
import org.bson.Document
import org.reactivestreams.Publisher
import reactor.kotlin.core.publisher.toMono

class FindAdministratorRoleLevelAction(
    val guildId: Snowflake,
    val roleId: Snowflake
) : DatabaseAction<Publisher<Int>> {
    override val collectionName: String
        get() = "administratorRoles"

    override fun run(
        collection: MongoCollection<Document>,
        keySetHandle: KeysetHandle?,
        encrypted: Boolean
    ): Publisher<Int> = collection.find(
        Filters.and(
            Filters.eq("roleId", roleId.asLong()),
            Filters.eq("guildId", guildId.asLong())
        )
    ).toMono().map {
        /*
        if (encrypted) {
            val aead: Aead = keySetHandle!!.getPrimitive(Aead::class.java)
            val encryptedValue = it.getString("level")
            aead.decrypt(encryptedValue.toByteArray(), null).toString(UTF_8).toInt()
        } else {*/
        it.getInteger("level")
        //}
    }.defaultIfEmpty(0)
}