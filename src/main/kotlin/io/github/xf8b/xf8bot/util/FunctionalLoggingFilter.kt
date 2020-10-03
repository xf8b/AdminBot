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

package io.github.xf8b.xf8bot.util

import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

/**
 * anonymous class :irritatered:
 */
class FunctionalLoggingFilter<T>(private val decideClosure: (T) -> FilterReply) : Filter<T>() {
    /**
     * If the decision is [FilterReply.DENY], then the event will be
     * dropped. If the decision is [FilterReply.NEUTRAL], then the next
     * filter, if any, will be invoked. If the decision is
     * [FilterReply.ACCEPT] then the event will be logged without
     * consulting with other filters in the chain.
     *
     * @param event The event to decide upon.
     */
    override fun decide(event: T): FilterReply = decideClosure.invoke(event)
}