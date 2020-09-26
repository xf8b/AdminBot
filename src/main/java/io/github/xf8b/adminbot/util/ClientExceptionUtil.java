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

package io.github.xf8b.adminbot.util;

import discord4j.rest.http.client.ClientException;
import discord4j.rest.json.response.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public final class ClientExceptionUtil {
    private ClientExceptionUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    @NotNull
    public static Predicate<Throwable> isClientExceptionWithCode(int code) {
        //todo fix error handling
        return throwable -> {
            if (throwable instanceof ClientException) {
                ClientException exception = (ClientException) throwable;
                if (exception.getErrorResponse().isEmpty()) return false;
                ErrorResponse errorResponse = exception.getErrorResponse().get();
                if (errorResponse.getFields().get("code") == null) return false;
                return (int) errorResponse.getFields().get("code") == code;
            } else {
                return false;
            }
        };
    }
}
