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

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import discord4j.common.util.Snowflake;
import lombok.Cleanup;
import lombok.experimental.UtilityClass;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ConfigUtil {
    private CommentedFileConfig readConfig() {
        URL defaultConfigUrl = ConfigUtil.class.getResource("baseConfig.toml");
        @Cleanup
        CommentedFileConfig config = CommentedFileConfig.builder("secrets/config.toml")
                .onFileNotFound(FileNotFoundAction.copyData(defaultConfigUrl))
                .autosave()
                .build();
        config.load();

        return config;
    }

    public String readToken() {
        CommentedFileConfig config = readConfig();
        if (config.get("token") == null) {
            throw new IllegalStateException("Token does not exist in config!");
        }
        return config.get("token");
    }

    public String readActivity() {
        CommentedFileConfig config = readConfig();
        if (config.get("activity") == null) {
            throw new IllegalStateException("Activity does not exist in config!");
        }
        return config.get("activity").toString();
    }

    public List<Snowflake> readAdmins() {
        CommentedFileConfig config = readConfig();
        if (config.get("admins") == null) {
            throw new IllegalStateException("Admins does not exist in config!");
        }
        List<Long> admins = config.get("admins");
        List<Snowflake> adminSnowflakes = new ArrayList<>();
        admins.forEach(snowflakeLong -> adminSnowflakes.add(Snowflake.of(snowflakeLong)));
        return adminSnowflakes;
    }

    public String readLogDumpWebhook() {
        CommentedFileConfig config = readConfig();
        if (config.get("logDumpWebhook") == null) {
            return "";
        }
        return config.get("logDumpWebhook").toString();
    }
}
