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

package io.github.xf8b.adminbot.api.commands.arguments;

import com.google.common.collect.Range;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.function.Function;
import java.util.function.Predicate;

@Builder(setterPrefix = "set")
@EqualsAndHashCode
@ToString
public class StringArgument implements Argument<String> {
    private final Range<Integer> index;
    private final String name;
    @Builder.Default
    private final boolean required = true;
    @Builder.Default
    private final Function<String, String> parseFunction = stringToParse -> stringToParse;
    @Builder.Default
    private final Predicate<String> validityPredicate = value -> true;
    @Builder.Default
    private final Function<String, String> invalidValueErrorMessageFunction = invalidValue -> DEFAULT_INVALID_VALUE_ERROR_MESSAGE;


    @Override
    public boolean isValidValue(String value) {
        return validityPredicate.test(value);
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public Range<Integer> index() {
        return index;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String parse(String stringToParse) {
        return parseFunction.apply(stringToParse);
    }

    @Override
    public String getInvalidValueErrorMessage(String invalidValue) {
        return invalidValueErrorMessageFunction.apply(invalidValue);
    }
}
