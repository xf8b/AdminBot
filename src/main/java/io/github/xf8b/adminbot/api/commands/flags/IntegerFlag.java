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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.function.Function;
import java.util.function.Predicate;

@EqualsAndHashCode
@ToString
public class IntegerFlag implements Flag<Integer> {
    private final String shortName;
    private final String longName;
    private final boolean required;
    private final Function<String, Integer> parseFunction;
    private final Predicate<String> validityPredicate;
    private final Function<String, String> invalidValueErrorMessageFunction;

    private IntegerFlag(String shortName, String longName, boolean required, Function<String, Integer> parseFunction, Predicate<String> validityPredicate, Function<String, String> invalidValueErrorMessageFunction) {
        if (shortName == null || longName == null) {
            throw new NullPointerException("The short name and/or long name was not set!");
        }
        this.shortName = shortName;
        this.longName = longName;
        this.required = required;
        this.parseFunction = parseFunction;
        this.validityPredicate = validityPredicate;
        this.invalidValueErrorMessageFunction = invalidValueErrorMessageFunction;
    }

    public static IntegerFlagBuilder builder() {
        return new IntegerFlagBuilder();
    }

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

    public static class IntegerFlagBuilder {
        private String shortName = null;
        private String longName = null;
        private boolean required = true;
        private Function<String, Integer> parseFunction = Integer::parseInt;
        private Predicate<String> validityPredicate = value -> {
            try {
                Integer.parseInt(value);
                return true;
            } catch (NumberFormatException exception) {
                return false;
            }
        };
        private Function<String, String> invalidValueErrorMessageFunction = invalidValue -> DEFAULT_INVALID_VALUE_ERROR_MESSAGE;

        private IntegerFlagBuilder() {
        }

        public IntegerFlagBuilder setShortName(String shortName) {
            this.shortName = shortName;
            return this;
        }

        public IntegerFlagBuilder setLongName(String longName) {
            this.longName = longName;
            return this;
        }

        public IntegerFlagBuilder setRequired(boolean required) {
            this.required = required;
            return this;
        }

        public IntegerFlagBuilder setParseFunction(Function<String, Integer> parseFunction) {
            this.parseFunction = parseFunction;
            return this;
        }

        public IntegerFlagBuilder setValidityPredicate(Predicate<String> validityPredicate) {
            this.validityPredicate = validityPredicate;
            return this;
        }

        public IntegerFlagBuilder setInvalidValueErrorMessageFunction(Function<String, String> invalidValueErrorMessageFunction) {
            this.invalidValueErrorMessageFunction = invalidValueErrorMessageFunction;
            return this;
        }

        public IntegerFlag build() {
            return new IntegerFlag(shortName, longName, required, parseFunction, validityPredicate, invalidValueErrorMessageFunction);
        }

        public String toString() {
            return "IntegerFlag.IntegerFlagBuilder(shortName=" + this.shortName + ", longName=" + this.longName + ", required=" + this.required + ", parseFunction=" + this.parseFunction + ", validityPredicate=" + this.validityPredicate + ", invalidValueErrorMessageFunction=" + this.invalidValueErrorMessageFunction + ")";
        }
    }
}
