package io.github.xf8b.adminbot.settings

interface Configuration {
    fun <T> get(name: String): T

    fun <T> set(name: String, newValue: T)
}