/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of AdminBot.
 *
 * AdminBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminBot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.adminbot.util

class Result<out T>(result: T?, errorMessage: String?, val resultType: ResultType) {
    val result: T? = result
        get() = field ?: throw NoSuchElementException()
    val errorMessage: String? = errorMessage
        get() = field ?: throw NoSuchElementException()

    companion object {
        @JvmStatic
        fun <T> success(result: T) = Result<T>(result, null, ResultType.SUCCESS)

        @JvmStatic
        fun <T> failure(errorMessage: String) = Result<T>(null, errorMessage, ResultType.FAILURE)

        @JvmStatic
        fun <T> pass() = Result<T>(null, null, ResultType.PASS)
    }

    enum class ResultType {
        SUCCESS,
        PASS,
        FAILURE;
    }
}