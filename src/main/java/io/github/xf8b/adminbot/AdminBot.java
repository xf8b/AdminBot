package io.github.xf8b.adminbot;

import io.github.xf8b.adminbot.listener.MessageListener;
import io.github.xf8b.adminbot.listener.MessageReactionAddListener;
import io.github.xf8b.adminbot.util.CommandRegistry;
import io.github.xf8b.adminbot.util.RegistryUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class AdminBot {
    public static final String DEFAULT_PREFIX = ">";
    public static String prefix = DEFAULT_PREFIX;
    public static final CommandRegistry commandRegistry = new CommandRegistry();
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBot.class);

    public static void main(String[] arguments) throws Exception {
        File databases = new File("databases");
        File secrets = new File("secrets");
        File prefixes = new File("databases/prefixes.db");
        File administrators = new File("databases/administrators.db");
        File warns = new File("databases/warns.db");
        File levels = new File("databases/levels.db");
        File adminbot_token = new File("secrets/adminbot_token.txt");
        if (databases.mkdirs()) LOGGER.info("Databases folder did not exist, creating folder.");
        if (secrets.mkdirs()) LOGGER.info("Secrets folder did not exist, creating folder.");
        if (prefixes.createNewFile()) LOGGER.info("Prefixes file did not exist, creating file.");
        if (administrators.createNewFile()) LOGGER.info("Administrators file did not exist, creating file.");
        if (warns.createNewFile()) LOGGER.info("Warns file did not exist, creating file.");
        if (levels.createNewFile()) LOGGER.info("Levels file did not exist, creating file.");
        if (adminbot_token.createNewFile()) LOGGER.info("Token file did not exist, creating file.");
        String token = "";
        File file = new File("secrets/adminbot_token.txt");
        try {
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                token = scanner.next();
            }

            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        RegistryUtil.getAllCommandHandlers().forEach(clazz -> {
            try {
                commandRegistry.registerClass(clazz);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        });

        JDA jda = JDABuilder.createDefault(token).setActivity(Activity.playing(DEFAULT_PREFIX + "help")).build();
        jda.addEventListener(new MessageListener(), new MessageReactionAddListener());
        LOGGER.info("Successfully started AdminBot!");
    }
}
