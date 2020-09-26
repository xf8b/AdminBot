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

package io.github.xf8b.xf8bot.util;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.github.napstr.logback.DiscordAppender;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public class LogbackUtil {
    public void setupDiscordAppender(@NotNull String webhookUrl, String username, String avatarUrl) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        AsyncAppender discordAsync = (AsyncAppender) loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("ASYNC_DISCORD");
        DiscordAppender discordAppender = (DiscordAppender) discordAsync.getAppender("DISCORD");
        discordAppender.setUsername(username);
        discordAppender.setAvatarUrl(avatarUrl);
        if (!webhookUrl.trim().isBlank()) discordAppender.setWebhookUri(webhookUrl);
        discordAppender.addFilter(new Filter<>() {
            @NotNull
            @Override
            public FilterReply decide(ILoggingEvent event) {
                if (webhookUrl.trim().isBlank()) {
                    return FilterReply.DENY;
                } else {
                    return FilterReply.NEUTRAL;
                }
            }
        });
    }
}
