package io.github.xf8b.adminbot.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
    private static final File DATABASES = new File("databases");
    private static final File SECRETS = new File("secrets");
    private static final File PREFIXES = new File("databases/prefixes.db");
    private static final File ADMINISTRATORS = new File("databases/administrators.db");
    private static final File WARNS = new File("databases/warns.db");
    private static final File LEVELS = new File("databases/levels.db");
    private static final File CONFIG = new File("secrets/config.json");

    public static void createFolders() {
        if (DATABASES.mkdirs()) LOGGER.info("Databases folder did not exist, creating folder.");
        if (SECRETS.mkdirs()) LOGGER.info("Secrets folder did not exist, creating folder.");
    }

    public static void createFiles() throws IOException {
        if (PREFIXES.createNewFile()) LOGGER.info("Prefixes file did not exist, creating file.");
        if (ADMINISTRATORS.createNewFile()) LOGGER.info("Administrators file did not exist, creating file.");
        if (WARNS.createNewFile()) LOGGER.info("Warns file did not exist, creating file.");
        if (LEVELS.createNewFile()) LOGGER.info("Levels file did not exist, creating file.");
        if (CONFIG.createNewFile()) {
            LOGGER.info("Config file did not exist, creating file.");
            Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
            gson.newJsonWriter(new FileWriter(CONFIG))
                    .beginObject()
                    .name("activity")
                    .value("${prefix}help")
                    .name("token")
                    .value("NotARealToken.PleaseFillIn.WithRealToken")
                    .endObject()
                    .close();
        }
    }
}
