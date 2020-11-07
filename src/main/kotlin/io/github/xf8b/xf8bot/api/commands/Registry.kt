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

package io.github.xf8b.xf8bot.api.commands

import io.github.xf8b.xf8bot.util.LoggerDelegate
import io.github.xf8b.xf8bot.util.getSubTypesOf
import io.github.xf8b.xf8bot.util.logger
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.slf4j.Logger

open class Registry<T : Any> : AbstractList<T>() {
    val registered: MutableList<T> = ArrayList()
    override val size: Int
        get() = registered.size
    var locked: Boolean = false
    private val logger: Logger by LoggerDelegate()

    /**
     * Registers the passed in [T].
     *
     * @param t the [T] to be registered
     */
    open fun register(t: T) {
        if (locked) throw UnsupportedOperationException("Registry is currently locked!")
        if (registered.find { it == t } != null) {
            throw IllegalArgumentException("Cannot register same thing twice!")
        }
        registered.add(t)
    }

    override fun get(index: Int): T = registered[index]

    override fun iterator(): MutableIterator<T> = registered.iterator()

    inline fun <reified E : T> findRegisteredWithType(): E = E::class.java.cast(registered.stream()
        .filter { it.javaClass == E::class.java }
        .findFirst()
        .orElseThrow { IllegalArgumentException("No registered object matches the class inputted!") })
}

inline fun <reified T : Any> Registry<T>.findAndRegister(packagePrefix: String) {
    val reflections = Reflections(packagePrefix, SubTypesScanner())
    val logger: Logger = logger(this::class.java)

    reflections.getSubTypesOf<T>().forEach {
        try {
            register(it.getConstructor().newInstance())
        } catch (exception: Exception) {
            logger.error("An error happened while trying to find and register!", exception)
        }
    }

    locked = true
}
