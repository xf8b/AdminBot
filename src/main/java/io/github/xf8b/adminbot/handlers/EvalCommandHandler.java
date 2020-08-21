package io.github.xf8b.adminbot.handlers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.xf8b.adminbot.events.CommandFiredEvent;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EvalCommandHandler extends AbstractCommandHandler {
    private static final ExecutorService EVAL_POOL = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setNameFormat("Eval Command Thread-%d")
            .build());

    public EvalCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}eval")
                .setUsage("${prefix}eval <code>")
                .setDescription("Evaluates code. Bot administrators only!")
                .setCommandType(CommandType.BOT_ADMINISTRATOR)
                .addAlias("${prefix}evaluate")
                .setMinimumAmountOfArgs(1)
                .setBotAdministratorOnly());
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        String thingToEval = event.getMessage()
                .getContent()
                .trim()
                .substring(event.getMessage().getContent().indexOf(" ") + 1)
                .trim();
        ScriptEngine engine = new GroovyScriptEngineImpl();
        EVAL_POOL.submit(() -> {
            try {
                engine.put("event", event);
                engine.put("guild", event.getGuild().block());
                engine.put("channel", event.getChannel().block());
                engine.put("member", event.getMember().get());
                String result = String.valueOf(engine.eval("import discord4j.common.util.Snowflake;" + thingToEval));
                event.getMessage()
                        .getChannel()
                        .flatMap(messageChannel -> messageChannel.createMessage("Result: " + result))
                        .subscribe();
            } catch (ScriptException exception) {
                event.getMessage()
                        .getChannel()
                        .flatMap(messageChannel -> messageChannel.createMessage("ScriptException: " + exception))
                        .subscribe();
            }
        });
    }

    public static void shutdownEvalPool() {
        EVAL_POOL.shutdown();
    }
}
