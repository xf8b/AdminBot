package io.github.xf8b.adminbot.util

import io.github.xf8b.adminbot.handlers.ClearCommandHandler
import io.github.xf8b.adminbot.handlers.EvalCommandHandler
import io.github.xf8b.adminbot.listeners.MessageListener
import org.slf4j.Logger
import kotlin.system.exitProcess

class ShutdownHandler {
    companion object {
        private val logger: Logger by LoggerDelegate()

        @JvmStatic
        fun shutdownWithError(throwable: Throwable): Nothing {
            logger.info("Shutting down due to throwable %s!", throwable)
            exitProcess(0)
        }

        @JvmStatic
        fun shutdown() {
            logger.info("Shutting down!")
            MessageListener.shutdownCommandThreadPool()
            EvalCommandHandler.shutdownEvalPool()
            ClearCommandHandler.shutdownClearThreadPool()
        }
    }
}