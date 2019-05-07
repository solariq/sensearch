package com.expleague.sensearch.cli.utils;

import com.expleague.sensearch.cli.utils.CommandLineTools.CheckableOption;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

public final class MultiArgOptions {
  private MultiArgOptions() { }
  public static class MultiEnumOption<T extends Enum<T>> implements CheckableOption {
    private final Class<T> enumType;
    private final Option option;
    private final Optional<Collection<T>> defaultValue;

    private CommandLine savedCommandLine;
    private Collection<T> savedValue;

    public static class MultiEnumOptionBuilder<T extends Enum<T>> {
      private Class<T> enumType;

      private int argumentsLimit;
      private String shortOption;
      private String longOption;
      private String description = "There is no description for the option";

      private Optional<Collection<T>> defaultValue = Optional.empty();

      private MultiEnumOptionBuilder(Class<T> enumType) {
        this.enumType = enumType;
      }

      public MultiEnumOptionBuilder<T> description(String description) {
        this.description = description;
        return this;
      }

      public MultiEnumOptionBuilder<T> shortOption(String shortOption) {
        this.shortOption = shortOption;
        return this;
      }

      public MultiEnumOptionBuilder<T> longOption(String longOption) {
        this.longOption = longOption;
        return this;
      }

      public MultiEnumOptionBuilder<T> argsLimit(int argumentsLimit) {
        this.argumentsLimit = argumentsLimit;
        return this;
      }

      public MultiEnumOptionBuilder<T> defaultValue(T... defaultValues) {
        if (defaultValues.length == 0) {
          throw new IllegalArgumentException("Default value cannot be empty!");
        }
        this.defaultValue = Optional.of(Stream.of(defaultValues).collect(Collectors.toSet()));
        return this;
      }

      public MultiEnumOption<T> build() {
        if (StringUtils.isEmpty(longOption) && StringUtils.isEmpty(shortOption)) {
          throw new IllegalArgumentException(
              "Either short name or long name must be set to build an option!");
        }

        Builder optionBuilder = StringUtils.isNotEmpty(shortOption) ?
            Option.builder(shortOption) : Option.builder();
        if (StringUtils.isNotEmpty(longOption)) {
          optionBuilder.longOpt(longOption);
        }
        optionBuilder.desc(description);
        optionBuilder.numberOfArgs(1);
        optionBuilder.required(false);

        return new MultiEnumOption<>(optionBuilder.build(), defaultValue, enumType);
      }
    }

    public static <T extends Enum<T>> MultiEnumOptionBuilder<T> builder(Class<T> enumType) {
      return new MultiEnumOptionBuilder<>(enumType);
    }

    private MultiEnumOption(Option option, Optional<Collection<T>> defaultValue, Class<T> enumType) {
      this.option = option;
      this.defaultValue = defaultValue;
      this.enumType = enumType;
    }

    public Collection<T> value(CommandLine commandLine) {
      return null;
    }

    public boolean hasValue(CommandLine commandLine) {
      return false;
    }

    public void addToOptions(Options options) {

    }

    @Override
    public void check(CommandLine commandLine) {

    }
  }
}
