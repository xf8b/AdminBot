/*
 * Copyright (c) 2020, 2021 xf8b.
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

package io.github.xf8b.xf8bot.commands.info

import io.github.xf8b.utils.tuples.and
import io.github.xf8b.xf8bot.api.commands.Command
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent
import io.github.xf8b.xf8bot.util.extensions.toImmutableList
import reactor.core.publisher.Mono

class SourceCommand : Command(
    name = "\${prefix}source",
    description = "The source code of xf8bot",
    commandType = CommandType.INFO,
    aliases = ("\${prefix}github" and "\${prefix}sourcecode").toImmutableList()
) {
    override fun onCommandFired(event: CommandFiredEvent): Mono<Void> = event.channel.flatMap {
        it.createMessage("https://github.com/xf8b/xf8bot/ (there are multiple branches)")
    }.then()
}