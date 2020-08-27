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

package io.github.xf8b.adminbot.api.commands.flags;

public interface Flag<T> {
    String DEFAULT_INVALID_VALUE_ERROR_MESSAGE = "Invalid value `%s`! Required value: %s.";

    String REGEX = "(--?)(\\w+) ?=? ?(\"?[\\w ]+\"?)";

    boolean requiresValue();

    boolean isValidValue(String value);

    boolean isRequired();

    String shortName();

    String longName();

    T parse(String stringToParse);

    String getInvalidValueErrorMessage(String invalidValue);
}
