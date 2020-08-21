/*
 * Copyright (c) 2020 xf8b.
 *
 * This file is part of AdminBot.
 *
 * AdminBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminBot.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.xf8b.adminbot.util;

import io.github.xf8b.adminbot.handlers.EvalCommandHandler;
import io.github.xf8b.adminbot.listeners.MessageListener;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ShutdownHandler {
    public void shutdownWithError(Throwable throwable) {
        LOGGER.info("Shutting down due to throwable %s!", throwable);
        System.exit(0);
    }

    public void shutdown() {
        LOGGER.info("Shutting down!");
        MessageListener.shutdownCommandThreadPool();
        EvalCommandHandler.shutdownEvalPool();
    }
}