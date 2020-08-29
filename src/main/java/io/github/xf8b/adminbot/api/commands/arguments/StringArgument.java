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

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

//delomboked because kotlin doesnt like lombok
public class StringArgument implements Argument<String> {
    private final Range<Integer> index;
    private final String name;
    private final boolean required;
    private final Function<String, String> parseFunction;
    private final Predicate<String> validityPredicate;
    private final Function<String, String> invalidValueErrorMessageFunction;

    private StringArgument(Range<Integer> index, String name, boolean required, Function<String, String> parseFunction, Predicate<String> validityPredicate, Function<String, String> invalidValueErrorMessageFunction) {
        if (index == null || name == null) {
            throw new NullPointerException("You are missing a index or name!");
        }
        this.index = index;
        this.name = name;
        this.required = required;
        this.parseFunction = parseFunction;
        this.validityPredicate = validityPredicate;
        this.invalidValueErrorMessageFunction = invalidValueErrorMessageFunction;
    }

    public static StringArgumentBuilder builder() {
        return new StringArgumentBuilder();
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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof StringArgument)) return false;
        final StringArgument other = (StringArgument) o;
        if (!other.canEqual(this)) return false;
        if (!Objects.equals(this.index, other.index)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (!Objects.equals(this$name, other$name)) return false;
        if (this.isRequired() != other.isRequired()) return false;
        if (!this.parseFunction.equals(other.parseFunction))
            return false;
        if (!this.validityPredicate.equals(other.validityPredicate))
            return false;
        return this.invalidValueErrorMessageFunction.equals(other.invalidValueErrorMessageFunction);
    }

    private boolean canEqual(final Object other) {
        return other instanceof StringArgument;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + ((Object) this.index).hashCode();
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        result = result * PRIME + (this.isRequired() ? 79 : 97);
        result = result * PRIME + this.parseFunction.hashCode();
        result = result * PRIME + this.validityPredicate.hashCode();
        result = result * PRIME + this.invalidValueErrorMessageFunction.hashCode();
        return result;
    }

    public String toString() {
        return "StringArgument(index=" + this.index + ", name=" + this.getName() + ", required=" + this.isRequired() + ", parseFunction=" + this.parseFunction + ", validityPredicate=" + this.validityPredicate + ", invalidValueErrorMessageFunction=" + this.invalidValueErrorMessageFunction + ")";
    }

    public static class StringArgumentBuilder {
        private Range<Integer> index = null;
        private String name = null;
        private boolean required = true;
        private Function<String, String> parseFunction = stringToParse -> stringToParse;
        private Predicate<String> validityPredicate = value -> true;
        private Function<String, String> invalidValueErrorMessageFunction = invalidValue -> DEFAULT_INVALID_VALUE_ERROR_MESSAGE;

        private StringArgumentBuilder() {
        }

        public StringArgument.StringArgumentBuilder setIndex(Range<Integer> index) {
            this.index = index;
            return this;
        }

        public StringArgument.StringArgumentBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public StringArgument.StringArgumentBuilder setRequired(boolean required) {
            this.required = required;
            return this;
        }

        public StringArgument.StringArgumentBuilder setParseFunction(Function<String, String> parseFunction) {
            this.parseFunction = parseFunction;
            return this;
        }

        public StringArgument.StringArgumentBuilder setValidityPredicate(Predicate<String> validityPredicate) {
            this.validityPredicate = validityPredicate;
            return this;
        }

        public StringArgument.StringArgumentBuilder setInvalidValueErrorMessageFunction(Function<String, String> invalidValueErrorMessageFunction) {
            this.invalidValueErrorMessageFunction = invalidValueErrorMessageFunction;
            return this;
        }

        public StringArgument build() {
            return new StringArgument(index, name, required, parseFunction, validityPredicate, invalidValueErrorMessageFunction);
        }

        public String toString() {
            return "StringArgument.StringArgumentBuilder(index=" + this.index + ", name=" + this.name + ", required=" + this.required + ", parseFunction=" + this.parseFunction + ", validityPredicate=" + this.validityPredicate + ", invalidValueErrorMessageFunction=" + this.invalidValueErrorMessageFunction + ")";
        }
    }
}
