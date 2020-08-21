/*
 * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
 *
 * Modifications copyright (c) 2020 xf8b
 * Changed to be used without JDA-Utilities and JDA
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

package io.github.xf8b.adminbot.handlers;

import discord4j.core.object.entity.channel.MessageChannel;
import io.github.xf8b.adminbot.events.CommandFiredEvent;

import java.time.temporal.ChronoUnit;

public class PingCommandHandler extends AbstractCommandHandler {
    public PingCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}ping")
                .setDescription("Gets the ping. Pretty useless.")
                .setCommandType(CommandType.OTHER));
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        channel.createMessage("Getting ping...").flatMap(message -> {
            long gatewayPing = event.getClient().getGatewayClient(event.getShardInfo().getIndex()).orElseThrow().getResponseTime().toMillis();
            long ping = event.getMessage().getTimestamp().until(message.getTimestamp(), ChronoUnit.MILLIS);
            return message.edit(messageEditSpec -> messageEditSpec.setContent("Ping: " + ping + "ms, Websocket: " + gatewayPing + "ms"));
        }).subscribe();
    }
}
