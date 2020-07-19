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
    public static final String DEFAULT_PREFIX = ">";
    public static String prefix = DEFAULT_PREFIX;
    public static final CommandRegistry commandRegistry = new CommandRegistry();
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBot.class);

    public static void main(String[] args) throws Exception {
        FileUtil.createFolders();
        FileUtil.createFiles();
        final File CONFIG = new File("secrets/config.json");
        String token = ConfigUtil.readToken(CONFIG);
        String activity = ConfigUtil.readActivity(CONFIG).replace("${prefix}", prefix);
        commandRegistry.registerCommandHandlers(
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
        LOGGER.info("Successfully started AdminBot!");
    }
}
