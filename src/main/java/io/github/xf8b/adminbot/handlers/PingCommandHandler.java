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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.PermissionSet;
import io.github.xf8b.adminbot.events.CommandFiredEvent;

import java.time.temporal.ChronoUnit;

public class PingCommandHandler extends AbstractCommandHandler {
    public PingCommandHandler() {
        super(
                "${prefix}ping",
                "${prefix}ping",
                "Gets the ping. Pretty useless.",
                ImmutableMap.of(),
                ImmutableList.of(),
                CommandType.OTHER,
                0,
                PermissionSet.none(),
                0
        );
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        MessageChannel channel = event.getChannel().block();
        long gatewayPing = event.getClient().getGatewayClient(event.getShardInfo().getIndex()).orElseThrow().getResponseTime().toMillis();
        channel.createMessage("Getting ping...").flatMap(message -> {
            long ping = event.getMessage().getTimestamp().until(message.getTimestamp(), ChronoUnit.MILLIS);
            return message.edit(messageEditSpec -> messageEditSpec.setContent("Ping: " + ping + "ms, Websocket: " + gatewayPing + "ms"));
        }).subscribe();
    }
}
