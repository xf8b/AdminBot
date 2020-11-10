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

import com.mongodb.client.model.Filters
import discord4j.common.util.Snowflake

class FindAdministratorRoleAction(val guildId: Snowflake, val roleId: Snowflake) : FindAllMatchingAction(
    "administratorRoles",
    Filters.and(
        Filters.eq("roleId", roleId.asLong()),
        Filters.eq("guildId", guildId.asLong())
    )
)