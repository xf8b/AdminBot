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

package io.github.xf8b.xf8bot.api.commands

import io.github.xf8b.xf8bot.util.getSubTypesOf
import io.github.xf8b.xf8bot.util.logger
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.slf4j.Logger

open class Registry<T : Any> : AbstractList<T>() {
    val registered: MutableList<T> = ArrayList()
    override val size get() = registered.size

    /**
     * Registers the passed in [T].
     *
     * @param t the [T] to be registered
     */
    open fun register(t: T) {
        if (registered.find { it == t } != null) {
            throw IllegalArgumentException("Cannot register same thing twice!")
        } else {
            registered += t
        }
    }

    override fun get(index: Int): T = registered[index]

    override fun iterator(): MutableIterator<T> = registered.iterator()

    inline fun <reified E : T> findRegisteredWithType(): E = E::class.java
        .cast(registered.find { it.javaClass == E::class.java }
            ?: throw IllegalArgumentException("No registered object matches the class inputted!"))
}

inline fun <reified T : Any> Registry<T>.findAndRegister(packagePrefix: String) {
    val reflections = Reflections(packagePrefix, SubTypesScanner())
    val logger: Logger = logger<Registry<T>>()

    for (klass in reflections.getSubTypesOf<T>()) {
        try {
            register(klass.getConstructor().newInstance())
        } catch (throwable: Throwable) {
            logger.error("An error happened while trying to register $klass!", throwable)
        }
    }
}
