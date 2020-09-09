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

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

@Builder(setterPrefix = "set")
@EqualsAndHashCode
@ToString
public class TimeFlag implements Flag<Pair<Long, TimeUnit>> {
    private final String shortName;
    private final String longName;
    @Builder.Default
    private final boolean required = true;
    @Builder.Default
    private final Function<String, Pair<Long, TimeUnit>> parseFunction = stringToParse -> {
        Long time = Long.parseLong(stringToParse.replaceAll("[a-zA-Z]", ""));
        String possibleTimeUnit = stringToParse.replaceAll("\\d", "");
        TimeUnit timeUnit = switch (possibleTimeUnit.toLowerCase()) {
            case "d", "day", "days" -> TimeUnit.DAYS;
            case "h", "hr", "hrs", "hours" -> TimeUnit.HOURS;
            case "m", "min", "mins", "minutes" -> TimeUnit.MINUTES;
            case "s", "sec", "secs", "second", "seconds" -> TimeUnit.SECONDS;
            default -> throw new IllegalStateException("The validity check should have run by now!");
        };
        return Pair.of(time, timeUnit);
    };
    @Builder.Default
    private final Predicate<String> validityPredicate = value -> {
        try {
            Long.parseLong(value.replaceAll("[a-zA-Z]", ""));
            String possibleTimeUnit = value.replaceAll("\\d", "");
            return switch (possibleTimeUnit.toLowerCase()) {
                case "d", "day", "days", "m", "mins", "minutes", "h", "hr", "hrs", "hours", "s", "sec", "secs", "second", "seconds" -> true;
                default -> false;
            };
        } catch (NumberFormatException exception) {
            return false;
        }
    };
    @Builder.Default
    private final Function<String, String> invalidValueErrorMessageFunction = invalidValue -> DEFAULT_INVALID_VALUE_ERROR_MESSAGE;

    @Override
    public boolean requiresValue() {
        return true;
    }

    @Override
    public boolean isValidValue(String value) {
        return validityPredicate.test(value);
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public String shortName() {
        return shortName;
    }

    @Override
    public String longName() {
        return longName;
    }

    @Override
    public Pair<Long, TimeUnit> parse(String stringToParse) {
        return parseFunction.apply(stringToParse);
    }

    @Override
    public String getInvalidValueErrorMessage(String invalidValue) {
        return invalidValueErrorMessageFunction.apply(invalidValue);
    }
}
