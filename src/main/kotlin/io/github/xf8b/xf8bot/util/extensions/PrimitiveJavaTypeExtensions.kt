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

package io.github.xf8b.xf8bot.util.extensions

val Byte.Companion.JAVA_WRAPPER_TYPE get() = java.lang.Byte::class.java
val Short.Companion.JAVA_WRAPPER_TYPE get() = java.lang.Short::class.java
val Int.Companion.JAVA_WRAPPER_TYPE get() = java.lang.Integer::class.java
val Long.Companion.JAVA_WRAPPER_TYPE get() = java.lang.Long::class.java
val Float.Companion.JAVA_WRAPPER_TYPE get() = java.lang.Float::class.java
val Double.Companion.JAVA_WRAPPER_TYPE get() = java.lang.Double::class.java