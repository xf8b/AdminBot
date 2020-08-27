package io.github.xf8b.adminbot.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

//credit: https://www.reddit.com/r/Kotlin/comments/8gbiul/slf4j_loggers_in_3_ways/
inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)