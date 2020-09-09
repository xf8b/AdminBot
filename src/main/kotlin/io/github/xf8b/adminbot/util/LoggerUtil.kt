package io.github.xf8b.adminbot.util

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
