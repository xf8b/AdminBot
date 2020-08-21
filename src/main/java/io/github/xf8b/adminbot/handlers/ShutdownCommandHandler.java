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

import io.github.xf8b.adminbot.events.CommandFiredEvent;

public class ShutdownCommandHandler extends AbstractCommandHandler {
    public ShutdownCommandHandler() {
        super(AbstractCommandHandler.builder()
                .setName("${prefix}shutdown")
                .setDescription("Shuts down the bot. Bot administrators only!")
                .setCommandType(CommandType.BOT_ADMINISTRATOR)
                .setBotAdministratorOnly());
    }

    @Override
    public void onCommandFired(CommandFiredEvent event) {
        event.getChannel()
                .flatMap(messageChannel -> messageChannel.createMessage("Shutting down!"))
                .subscribe(message -> System.exit(0));
    }
}
