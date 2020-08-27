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

package io.github.xf8b.adminbot.handlers;

import com.google.common.collect.Range;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.xf8b.adminbot.api.commands.AbstractCommandHandler;
import io.github.xf8b.adminbot.api.commands.CommandFiredEvent;
import io.github.xf8b.adminbot.api.commands.arguments.StringArgument;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EvalCommandHandler extends AbstractCommandHandler {
    private static final StringArgument CODE_TO_EVAL = StringArgument.builder()
            .setIndex(Range.atLeast(1))
            .setName("code to eval")
            .build();

    private static final ExecutorService EVAL_POOL = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setNameFormat("Eval Command Thread-%d")
            .build());

    public EvalCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}eval")
                .setDescription("Evaluates code. Bot administrators only!")
                .setCommandType(CommandType.BOT_ADMINISTRATOR)
                .addAlias("${prefix}evaluate")
                .setMinimumAmountOfArgs(1)
                .addArgument(CODE_TO_EVAL)
                .setBotAdministratorOnly());
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        String thingToEval = event.getValueOfArgument(CODE_TO_EVAL);
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
