package com.expleague.sensearch.cli.utils;

import com.expleague.sensearch.cli.utils.SingleArgPredicates.DoubleOptionPredicate;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.IntOptionPredicate;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.LongOptionPredicate;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.PathOptionPredicate;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.StringOptionPredicate;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class SingleArgOptions {

  public static class IntOption {

    private final Option option;
    private final OptionalInt defaultValue;
    private final IntOptionPredicate[] predicates;

    public IntOption(Option option, IntOptionPredicate... predicates) {
      this(option, OptionalInt.empty(), predicates);
    }

    public IntOption(Option option, int defaultValue, IntOptionPredicate... predicates) {
      this(option, OptionalInt.of(defaultValue), predicates);
    }

    private IntOption(Option option, OptionalInt defaultValue, IntOptionPredicate... predicates) {
      this.option = option;
      this.defaultValue = defaultValue;
      this.predicates = predicates;
    }

    public int value(CommandLine commandLine) {
      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          return defaultValue.getAsInt();
        }
        throw new IllegalArgumentException(String.format("Option [ -%s ] has no default value!"
            + " Please, pass a value as argument", option.getOpt()));
      }

      String stringValue = commandLine.getOptionValue(option.getOpt());
      int value;
      try {
        value = Integer.parseInt(stringValue);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(String.format("Parameter of the option [ -%s ]"
            + " must be an integer! Received [ %s ] instead", option.getOpt(), stringValue));
      }
      for (IntOptionPredicate predicate : predicates) {
        if (!predicate.test(value)) {
          throw new IllegalArgumentException(
              String.format("Option [ -%s ] received invalid an argument: [ %s ]\n%s",
                  option.getOpt(), stringValue, predicate.failMessage()));
        }
      }

      return value;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }
  }

  public static class DoubleOption {

    private final Option option;
    private final OptionalDouble defaultValue;
    private final DoubleOptionPredicate[] predicates;

    public DoubleOption(Option option, DoubleOptionPredicate... predicates) {
      this(option, OptionalDouble.empty(), predicates);
    }

    public DoubleOption(Option option, double defaultValue, DoubleOptionPredicate... predicates) {
      this(option, OptionalDouble.of(defaultValue), predicates);
    }

    private DoubleOption(Option option, OptionalDouble defaultValue,
        DoubleOptionPredicate[] predicates) {
      this.option = option;
      this.defaultValue = defaultValue;
      this.predicates = predicates;
    }

    public double value(CommandLine commandLine) {
      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          return defaultValue.getAsDouble();
        }
        throw new IllegalArgumentException(String.format("Option [ -%s ] has no default value!"
            + " Please, pass a value as argument", option.getOpt()));
      }

      String stringValue = commandLine.getOptionValue(option.getOpt());
      double value;
      try {
        value = Double.parseDouble(stringValue);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(String.format("Parameter of the option [ -%s ]"
                + " must be a double with a dot as decimal separator! Received [ %s ] instead",
            option.getOpt(), stringValue));
      }
      for (DoubleOptionPredicate predicate : predicates) {
        if (!predicate.test(value)) {
          throw new IllegalArgumentException(
              String.format("Option [ -%s ] received invalid an argument: [ %s ]\n%s",
                  option.getOpt(), stringValue, predicate.failMessage()));
        }
      }

      return value;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }
  }

  public static class LongOption {

    private final Option option;
    private final OptionalLong defaultValue;
    private final LongOptionPredicate[] predicates;

    public LongOption(Option option, LongOptionPredicate... predicates) {
      this(option, OptionalLong.empty(), predicates);
    }

    public LongOption(Option option, long defaultValue, LongOptionPredicate... predicates) {
      this(option, OptionalLong.of(defaultValue), predicates);
    }

    private LongOption(Option option, OptionalLong defaultValue,
        LongOptionPredicate... predicates) {
      this.option = option;
      this.defaultValue = defaultValue;
      this.predicates = predicates;
    }

    public long value(CommandLine commandLine) {
      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          return defaultValue.getAsLong();
        }
        throw new IllegalArgumentException(String.format("Option [ -%s ] has no default value!"
            + " Please, pass a value as argument", option.getOpt()));
      }

      String stringValue = commandLine.getOptionValue(option.getOpt());
      long value;
      try {
        value = Long.parseLong(stringValue);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            String.format("Parameter of the option [ -%s ] must be a long integer!"
                + " Received [ %s ] instead", option.getOpt(), stringValue));
      }
      for (LongOptionPredicate predicate : predicates) {
        if (!predicate.test(value)) {
          throw new IllegalArgumentException(
              String.format("Option [ -%s ] received invalid an argument: [ %s ]\n%s",
                  option.getOpt(), stringValue, predicate.failMessage()));
        }
      }

      return value;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }
  }

  public static class StringOption {

    private final Option option;
    private final Optional<String> defaultValue;
    private final StringOptionPredicate[] predicates;

    public StringOption(Option option, StringOptionPredicate... predicates) {
      this(option, Optional.empty(), predicates);
    }

    public StringOption(Option option, String defaultValue, StringOptionPredicate... predicates) {
      this(option, Optional.of(defaultValue), predicates);
    }

    private StringOption(Option option, Optional<String> defaultValue,
        StringOptionPredicate... predicates) {
      this.option = option;
      this.defaultValue = defaultValue;
      this.predicates = predicates;
    }

    public String value(CommandLine commandLine) {
      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          return defaultValue.get();
        }
        throw new IllegalArgumentException(String.format("Option [ -%s ] has no default value!"
            + " Please, pass a value as an argument", option.getOpt()));
      }

      String stringValue = commandLine.getOptionValue(option.getOpt());
      for (StringOptionPredicate predicate : predicates) {
        if (!predicate.test(stringValue)) {
          throw new IllegalArgumentException(
              String.format("Option [ -%s ] received invalid argument: [ %s ]\n%s", option.getOpt(),
                  stringValue, predicate.failMessage()));
        }
      }

      return stringValue;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }
  }

  public static class PathOption {

    private final Option option;
    private final Optional<Path> defaultValue;
    private final PathOptionPredicate[] predicates;

    public PathOption(Option option, PathOptionPredicate... predicates) {
      this(option, Optional.empty(), predicates);
    }

    public PathOption(Option option, String defaultValue, PathOptionPredicate... predicates) {
      this(option, Optional.of(Paths.get(defaultValue)), predicates);
    }

    public PathOption(Option option, Path defaultValue, PathOptionPredicate... predicates) {
      this(option, Optional.of(defaultValue), predicates);
    }

    private PathOption(Option option, Optional<Path> defaultValue,
        PathOptionPredicate[] predicates) {
      this.option = option;
      this.defaultValue = defaultValue;
      this.predicates = predicates;
    }

    public Path value(CommandLine commandLine) {
      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          return defaultValue.get();
        }
        throw new IllegalArgumentException(String.format("Option [ -%s ] has no default value!"
            + " Please, pass a value as an argument", option.getOpt()));
      }

      String stringValue = commandLine.getOptionValue(option.getOpt());
      Path value = Paths.get(stringValue);
      for (PathOptionPredicate predicate : predicates) {
        if (!predicate.test(value)) {
          throw new IllegalArgumentException(
              String.format("Option [ -%s ] received invalid argument: [ %s ]\n%s", option.getOpt(),
                  stringValue, predicate.failMessage()));
        }
      }

      return value;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }
  }

  public static class EnumOption<T extends Enum<T>> {

    private final Option option;
    private final Class<T> enumType;
    private final Optional<T> defaultValue;
    private final String availableValusHint;

    public EnumOption(Option option, Class<T> enumType) {
      this(option, Optional.empty(), enumType);
    }

    public EnumOption(Option option, T defaultValue, Class<T> enumType) {
      this(option, Optional.of(defaultValue), enumType);
    }

    private EnumOption(Option option, Optional<T> defaultValue, Class<T> enumType) {
      this.option = option;
      this.defaultValue = defaultValue;
      this.enumType = enumType;
      StringBuilder stringBuilder = new StringBuilder();
      for (T eType : enumType.getEnumConstants()) {
        stringBuilder.append(eType.toString()).append(", ");
      }
      availableValusHint = stringBuilder.substring(0, stringBuilder.length() - 2);
    }

    public T value(CommandLine commandLine) {
      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          return defaultValue.get();
        }
        throw new IllegalArgumentException(String.format("Option [ -%s ] has no default value!"
            + " Please, pass a value as argument. Available values: %s",
            option.getOpt(), availableValusHint));
      }

      String stringValue = commandLine.getOptionValue(option.getOpt());
      T value;
      try {
        value = Enum.valueOf(enumType, stringValue);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(String.format("Parameter of the option [ -%s ]"
                + " must be one of vales: [ %s ]! Received [ %s ]",
            option.getOpt(), availableValusHint, stringValue));
      }

      return value;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }
  }
}
