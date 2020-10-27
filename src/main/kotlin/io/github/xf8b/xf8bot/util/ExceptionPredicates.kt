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

import discord4j.rest.http.client.ClientException
import java.util.function.Predicate

object ExceptionPredicates {
    @JvmStatic
    fun isClientExceptionWithCode(code: Int): Predicate<Throwable> = Predicate { throwable ->
        if (throwable is ClientException) {
            throwable.errorResponse
                .map { it.fields["code"] }
                .map { it as Int == code }
                .orElse(false)
        } else {
            false
        }
    }
}