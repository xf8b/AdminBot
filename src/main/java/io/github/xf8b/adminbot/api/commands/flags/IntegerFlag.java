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

import java.util.function.Function;
import java.util.function.Predicate;

@Builder(setterPrefix = "set")
@EqualsAndHashCode
@ToString
public class IntegerFlag implements Flag<Integer> {
    private final String shortName;
    private final String longName;
    @Builder.Default
    private final boolean required = true;
    @Builder.Default
    private final Function<String, Integer> parseFunction = stringToParse -> {
        try {
            return Integer.parseInt(stringToParse);
        } catch (NumberFormatException exception) {
            return null; //should never happen because the validity check should return false
        }
    };
    @Builder.Default
    private final Predicate<String> validityPredicate = value -> {
        try {
            Integer.parseInt(value);
            return true;
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
    public Integer parse(String stringToParse) {
        return parseFunction.apply(stringToParse);
    }

    @Override
    public String getInvalidValueErrorMessage(String invalidValue) {
        return invalidValueErrorMessageFunction.apply(invalidValue);
    }
}
