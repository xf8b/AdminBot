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

import discord4j.common.util.Snowflake

class FindWarnsAction(criteria: Map<String, *>) : SelectAction(
    table = "warns",
    selectedFields = listOf("*"),
    criteria = criteria
) {
    constructor(
        guildId: Snowflake? = null,
        memberId: Snowflake? = null,
        warnerId: Snowflake? = null,
        warnId: String? = null,
        reason: String? = null
    ) : this(mutableMapOf<String, Any>().apply {
        when {
            guildId != null -> this["guildId"] = guildId.asLong()
            memberId != null -> this["memberId"] = memberId.asLong()
            warnerId != null -> this["warnerId"] = warnerId.asLong()
            warnId != null -> this["warnId"] = warnId
            reason != null -> this["reason"] = reason
        }
    }) {
        if (guildId == null && warnId == null && reason == null && memberId == null && warnerId == null) {
            throw IllegalArgumentException("Criteria is required!")
        }
    }
}