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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

//credit: https://stackoverflow.com/questions/34416869/idiomatic-way-of-logging-in-kotlin
fun <T : Any> logger(clazz: Class<T>): Logger = LoggerFactory.getLogger(clazz)

class LoggerDelegate<in R : Any> : ReadOnlyProperty<R, Logger> {
    private lateinit var logger: Logger

    override fun getValue(thisRef: R, property: KProperty<*>): Logger {
        if (!::logger.isInitialized) {
            logger = logger(thisRef.javaClass)
        }
        return logger
    }
}
