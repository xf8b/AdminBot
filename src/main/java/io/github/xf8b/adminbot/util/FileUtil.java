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

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@UtilityClass
@Slf4j
public class FileUtil {
    private final File DATABASES = new File("databases");
    private final File SECRETS = new File("secrets");
    private final File PREFIXES = new File("databases/prefixes.db");
    private final File ADMINISTRATORS = new File("databases/administrators.db");
    private final File WARNS = new File("databases/warns.db");

    public void createFolders() throws IOException {
        if (!DATABASES.exists()) {
            Files.createDirectory(DATABASES.toPath());
            LOGGER.info("Databases folder did not exist, creating folder.");
        }
        if (!SECRETS.exists()) {
            Files.createDirectory(SECRETS.toPath());
            LOGGER.info("Secrets folder did not exist, creating folder.");
        }
    }

    public void createFiles() throws IOException {
        if (!PREFIXES.exists()) {
            Files.createFile(PREFIXES.toPath());
            LOGGER.info("Prefixes file did not exist, creating file.");
        }
        if (!ADMINISTRATORS.exists()) {
            Files.createFile(ADMINISTRATORS.toPath());
            LOGGER.info("Administrators file did not exist, creating file.");
        }
        if (!WARNS.exists()) {
            Files.createFile(WARNS.toPath());
            LOGGER.info("Warns file did not exist, creating file.");
        }
    }
}
