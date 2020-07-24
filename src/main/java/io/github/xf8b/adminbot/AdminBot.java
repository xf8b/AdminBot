package io.github.xf8b.adminbot;

import io.github.xf8b.adminbot.handler.*;
import io.github.xf8b.adminbot.listener.MessageListener;
import io.github.xf8b.adminbot.listener.MessageReactionAddListener;
import io.github.xf8b.adminbot.util.CommandRegistry;
import io.github.xf8b.adminbot.util.ConfigUtil;
import io.github.xf8b.adminbot.util.FileUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class AdminBot {
    private static AdminBot instance;
    public final String DEFAULT_PREFIX;
    public String prefix;
    public final CommandRegistry COMMAND_REGISTRY;
    private final Logger LOGGER;

    private AdminBot() {
        instance = this;
        DEFAULT_PREFIX = ">";
        prefix = DEFAULT_PREFIX;
        COMMAND_REGISTRY = new CommandRegistry();
        LOGGER = LoggerFactory.getLogger(this.getClass());
    }

    public static AdminBot getInstance() {
        return instance;
    }

    public static void main(String[] args) throws Exception {
        new AdminBot();
        FileUtil.createFolders();
        FileUtil.createFiles();
        final File CONFIG = new File("secrets/config.json");
        String token = ConfigUtil.readToken(CONFIG);
        String activity = ConfigUtil.readActivity(CONFIG).replace("${prefix}", instance.prefix);
        instance.COMMAND_REGISTRY.registerCommandHandlers(
                new AdministratorsCommandHandler(),
                new BanCommandHandler(),
                new ClearCommandHandler(),
                new HelpCommandHandler(),
                new KickCommandHandler(),
                new LeaderboardCommandHandler(),
                new LevelCommandHandler(),
                new MuteCommandHandler(),
                new NicknameCommandHandler(),
                new PingCommandHandler(),
                new PrefixCommandHandler(),
                new RemoveWarnCommandHandler(),
                new UnbanCommandHandler(),
                new WarnCommandHandler(),
                new WarnsCommandHandler()
        );
        JDA jda = JDABuilder.createDefault(token).setActivity(Activity.playing(activity)).build();
        jda.addEventListener(new MessageListener(), new MessageReactionAddListener());
        instance.LOGGER.info("Successfully started AdminBot!");
    }
}
