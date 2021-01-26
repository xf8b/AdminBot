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

import io.r2dbc.spi.Result
import org.apache.commons.lang3.StringUtils
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.function.Function

// reduce boilerplate
fun <I, R> functionReturning(returnedValue: R): Function<I, R> = Function { returnedValue }

val Result.updatedRows: Mono<Boolean> get() = this.rowsUpdated.toMono().map { it != 0 }

// increase clarity
fun String.isAlpha() = StringUtils.isAlpha(this)