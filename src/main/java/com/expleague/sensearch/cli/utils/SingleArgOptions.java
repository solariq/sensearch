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

// TODO: All constructors should NOT accept Options and build them within themselves
public class SingleArgOptions {

  public static void checkOptions(CommandLine commandLine, CheckableOption... options) {
    for (CheckableOption option : options) {
      option.check(commandLine);
    }
  }

  public interface CheckableOption {

    void check(CommandLine commandLine);
  }

  public static class IntOption implements CheckableOption {

    private final Option option;
    private final OptionalInt defaultValue;
    private final IntOptionPredicate[] predicates;

    private int savedValue;
    private CommandLine savedCommandLine;

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
      if (commandLine != savedCommandLine) {
        check(commandLine);
      }
      return savedValue;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) throws IllegalArgumentException {
      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          savedCommandLine = commandLine;
          savedValue = defaultValue.getAsInt();
          return;
        }
        throw new IllegalArgumentException(String.format("Option [ -%s ] has no default value!"
            + " Please, pass a value as argument", option.getOpt()));
      }

      int value;
      String stringValue = commandLine.getOptionValue(option.getOpt());
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

      savedCommandLine = commandLine;
      savedValue = value;
    }
  }

  public static class DoubleOption implements CheckableOption {

    private final Option option;
    private final OptionalDouble defaultValue;
    private final DoubleOptionPredicate[] predicates;

    private double savedValue;
    private CommandLine savedCommandLine;

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
      if (commandLine != savedCommandLine) {
        check(commandLine);
      }
      return savedValue;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) {
      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          savedCommandLine = commandLine;
          savedValue = defaultValue.getAsDouble();
          return;
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

      savedValue = value;
      savedCommandLine = commandLine;
    }
  }

  public static class LongOption implements CheckableOption {

    private final Option option;
    private final OptionalLong defaultValue;
    private final LongOptionPredicate[] predicates;

    private long savedValue;
    private CommandLine savedCommandLine;

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
      if (commandLine != savedCommandLine) {
        check(commandLine);
      }
      return savedValue;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) {
      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          savedValue = defaultValue.getAsLong();
          savedCommandLine = commandLine;
          return;
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

      savedValue = value;
      savedCommandLine = commandLine;
    }
  }

  public static class StringOption implements CheckableOption {

    private final Option option;
    private final Optional<String> defaultValue;
    private final StringOptionPredicate[] predicates;

    private CommandLine savedCommandLine;
    private String savedValue;

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
      if (commandLine != savedCommandLine) {
        check(commandLine);
      }

      return savedValue;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) {
      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          savedValue = defaultValue.get();
          savedCommandLine = commandLine;
          return;
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

      savedValue = stringValue;
      savedCommandLine = commandLine;
    }
  }

  public static class PathOption implements CheckableOption {

    private final Option option;
    private final Optional<Path> defaultValue;
    private final PathOptionPredicate[] predicates;

    private CommandLine savedCommandLine;
    private Path savedValue;

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
      if (commandLine != savedCommandLine) {
        check(commandLine);
      }

      return savedValue;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) {

      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          savedValue = defaultValue.get();
          savedCommandLine = commandLine;
          return;
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

      savedValue = value;
      savedCommandLine = commandLine;
    }
  }

  public static class EnumOption<T extends Enum<T>> implements CheckableOption {

    private final Option option;
    private final Class<T> enumType;
    private final Optional<T> defaultValue;
    private final String availableValusHint;

    private T savedValue;
    private CommandLine savedCommandLine;

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
      if (commandLine != savedCommandLine) {
        check(commandLine);
      }

      return savedValue;
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) {
      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          savedValue = defaultValue.get();
          savedCommandLine = commandLine;
          return;
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

      savedValue = value;
      savedCommandLine = commandLine;
    }
  }
}
