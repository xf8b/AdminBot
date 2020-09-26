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

package io.github.xf8b.xf8bot.commands;

import com.google.common.collect.Range;
import io.github.xf8b.xf8bot.api.commands.AbstractCommand;
import io.github.xf8b.xf8bot.api.commands.CommandFiredEvent;
import io.github.xf8b.xf8bot.api.commands.arguments.StringArgument;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class EvalCommand extends AbstractCommand {
    private static final StringArgument CODE_TO_EVAL = StringArgument.builder()
            .setIndex(Range.atLeast(1))
            .setName("code to eval")
            .build();

    public EvalCommand() {
        super(AbstractCommand.builder()
                .setName("${prefix}eval")
                .setDescription("Evaluates code. Bot administrators only!")
                .setCommandType(CommandType.BOT_ADMINISTRATOR)
                .addAlias("${prefix}evaluate")
                .setMinimumAmountOfArgs(1)
                .addArgument(CODE_TO_EVAL)
                .setBotAdministratorOnly());
    }

    @NotNull
    @Override
    public Mono<Void> onCommandFired(@NotNull CommandFiredEvent event) {
        String thingToEval = event.getValueOfArgument(CODE_TO_EVAL).get();
        ScriptEngine engine = new GroovyScriptEngineImpl();
        try {
            engine.put("event", event);
            engine.put("guild", event.getGuild().block());
            engine.put("channel", event.getChannel().block());
            engine.put("member", event.getMember().get());
            String result = String.valueOf(engine.eval("import discord4j.common.util.Snowflake;" + thingToEval));
            return event.getMessage()
                    .getChannel()
                    .flatMap(messageChannel -> messageChannel.createMessage("Result: " + result))
                    .subscribeOn(Schedulers.boundedElastic())
                    .then();
        } catch (ScriptException exception) {
            return event.getMessage()
                    .getChannel()
                    .flatMap(messageChannel -> messageChannel.createMessage("ScriptException: " + exception))
                    .then();
        }
        //TODO: see if this threading works
    }
}
