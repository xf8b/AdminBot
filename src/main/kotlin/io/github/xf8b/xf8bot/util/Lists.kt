package io.github.xf8b.xf8bot.util

import com.google.common.collect.ImmutableList

fun <T> immutableListOf(vararg elements: T): ImmutableList<T> = ImmutableList.copyOf(elements)