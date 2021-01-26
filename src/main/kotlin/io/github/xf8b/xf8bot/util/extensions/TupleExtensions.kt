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

import reactor.util.function.*
import javax.script.AbstractScriptEngine

operator fun <A, B> Tuple2<A, B>.component1(): A = this.t1
operator fun <A, B> Tuple2<A, B>.component2(): B = this.t2
operator fun <A, B, C> Tuple3<A, B, C>.component3(): C = this.t3
operator fun <A, B, C, D> Tuple4<A, B, C, D>.component4(): D = this.t4
operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component5(): E = this.t5
operator fun <A, B, C, D, E, F> Tuple6<A, B, C, D, E, F>.component6(): F = this.t6
operator fun <A, B, C, D, E, F, G> Tuple7<A, B, C, D, E, F, G>.component7(): G = this.t7
operator fun <A, B, C, D, E, F, G, H> Tuple8<A, B, C, D, E, F, G, H>.component8(): H = this.t8
operator fun AbstractScriptEngine.set(key: String, value: Any?) = this.put(key, value)