/*
 * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
 *
 * Modifications copyright (c) 2020 xf8b
 * Changed to be used without JDA-Utilities
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.xf8b.adminbot.handler;

import io.github.xf8b.adminbot.util.CommandHandler;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.temporal.ChronoUnit;

@CommandHandler(
        name = "${prefix}ping",
        usage = "${prefix}ping",
        description = "Gets the ping. Pretty useless."
)
public class PingCommandHandler {
    public static void onPingCommand(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        long gatewayPing = event.getJDA().getGatewayPing();
        channel.sendMessage("Getting ping...").queue(message -> {
            long ping = event.getMessage().getTimeCreated().until(message.getTimeCreated(), ChronoUnit.MILLIS);
            message.editMessage("Ping: " + ping + "ms, Websocket: " + gatewayPing + "ms").queue();
        });
    }
}
