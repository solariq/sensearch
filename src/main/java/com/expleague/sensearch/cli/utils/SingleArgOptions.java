package com.expleague.sensearch.cli.utils;

import com.expleague.sensearch.cli.Command;
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
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

// TODO: tit should be possible to make any option optional
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

    public static class IntOptionBuilder {

      private String shortOption;
      private String longOption;

      private OptionalInt defaultValue = OptionalInt.empty();
      private String description = "There is no description for the option";
      private IntOptionPredicate[] predicates = new IntOptionPredicate[0];

      private IntOptionBuilder() {
      }

      public IntOptionBuilder description(String description) {
        this.description = description;
        return this;
      }

      public IntOptionBuilder shortOption(String shortOption) {
        this.shortOption = shortOption;
        return this;
      }

      public IntOptionBuilder longOption(String longOption) {
        this.longOption = longOption;
        return this;
      }

      public IntOptionBuilder predicates(IntOptionPredicate... predicates) {
        this.predicates = predicates;
        return this;
      }

      public IntOptionBuilder defaultValue(int defaultValue) {
        this.defaultValue = OptionalInt.of(defaultValue);
        return this;
      }

      public IntOption build() {
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

        return new IntOption(optionBuilder.build(), defaultValue, predicates);
      }
    }

    public static IntOptionBuilder builder() {
      return new IntOptionBuilder();
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

      if (!defaultValue.isPresent() && !commandLine.hasOption(option.getOpt())) {
        throw new IllegalStateException(
            String.format("Tried to get value for option [ -%s ]"
                + " when it has no value!", option.getOpt()));
      }

      return savedValue;
    }

    public boolean hasOption(CommandLine commandLine) {
      return commandLine.hasOption(option.getOpt());
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) throws IllegalArgumentException {
      if (commandLine == null) {
        throw new NullPointerException(String.format(
            "Tried to parse argument [ -%s ] from 'null' command line!", option.getOpt()));
      }

      if (!commandLine.hasOption(option.getOpt())) {
        if (defaultValue.isPresent()) {
          savedCommandLine = commandLine;
          savedValue = defaultValue.getAsInt();
          return;
        }
        throw new IllegalArgumentException(String.format("Option [ -%s ] has no default value!"
            + " Please, pass a value as an argument", option.getOpt()));
      }
      int value;
      String stringValue = commandLine.getOptionValue(option.getOpt());
      try {
        value = Integer.parseInt(stringValue);
      } catch (NullPointerException | NumberFormatException e) {
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

    public static class DoubleOptionBuilder {

      private String shortOption;
      private String longOption;

      private OptionalDouble defaultValue = OptionalDouble.empty();
      private String description = "There is no description for the option";
      private DoubleOptionPredicate[] predicates = new DoubleOptionPredicate[0];

      private DoubleOptionBuilder() {
      }

      public DoubleOptionBuilder description(String description) {
        this.description = description;
        return this;
      }

      public DoubleOptionBuilder shortOption(String shortOption) {
        this.shortOption = shortOption;
        return this;
      }

      public DoubleOptionBuilder longOption(String longOption) {
        this.longOption = longOption;
        return this;
      }

      public DoubleOptionBuilder predicates(DoubleOptionPredicate... predicates) {
        this.predicates = predicates;
        return this;
      }

      public DoubleOptionBuilder defaultValue(double defaultValue) {
        this.defaultValue = OptionalDouble.of(defaultValue);
        return this;
      }

      public DoubleOption build() {
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

        return new DoubleOption(optionBuilder.build(), defaultValue, predicates);
      }
    }

    public static DoubleOptionBuilder builder() {
      return new DoubleOptionBuilder();
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

    public boolean hasOption(CommandLine commandLine) {
      return commandLine.hasOption(option.getOpt());
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) {
      if (commandLine == null) {
        throw new NullPointerException(String.format(
            "Tried to parse argument [ -%s ] from 'null' command line!", option.getOpt()));
      }

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

    public static class LongOptionBuilder {

      private String shortOption;
      private String longOption;

      private OptionalLong defaultValue = OptionalLong.empty();
      private String description = "There is no description for the option";
      private LongOptionPredicate[] predicates = new LongOptionPredicate[0];

      private LongOptionBuilder() {
      }

      public LongOptionBuilder description(String description) {
        this.description = description;
        return this;
      }

      public LongOptionBuilder shortOption(String shortOption) {
        this.shortOption = shortOption;
        return this;
      }

      public LongOptionBuilder longOption(String longOption) {
        this.longOption = longOption;
        return this;
      }

      public LongOptionBuilder predicates(LongOptionPredicate... predicates) {
        this.predicates = predicates;
        return this;
      }

      public LongOptionBuilder defaultValue(long defaultValue) {
        this.defaultValue = OptionalLong.of(defaultValue);
        return this;
      }

      public LongOption build() {
        if (StringUtils.isEmpty(longOption) && StringUtils.isEmpty(shortOption)) {
          throw new IllegalArgumentException(
              "Either short name or long name must be set to build an option!");
        }
        Builder optionBuilder = Option.builder();
        if (StringUtils.isNotEmpty(shortOption)) {
          optionBuilder.argName(shortOption);
        }
        if (StringUtils.isNotEmpty(longOption)) {
          optionBuilder.longOpt(longOption);
        }
        optionBuilder.desc(description);
        optionBuilder.numberOfArgs(1);
        optionBuilder.required(false);

        return new LongOption(optionBuilder.build(), defaultValue, predicates);
      }
    }

    public static LongOptionBuilder builder() {
      return new LongOptionBuilder();
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

    public boolean hasOption(CommandLine commandLine) {
      return commandLine.hasOption(option.getOpt());
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) {
      if (commandLine == null) {
        throw new NullPointerException(String.format(
            "Tried to parse argument [ -%s ] from 'null' command line!", option.getOpt()));
      }

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

    public static class StringOptionBuilder {

      private String shortOption;
      private String longOption;

      private Optional<String> defaultValue = Optional.empty();
      private String description = "There is no description for the option";
      private StringOptionPredicate[] predicates = new StringOptionPredicate[0];

      private StringOptionBuilder() {
      }

      public StringOptionBuilder description(String description) {
        this.description = description;
        return this;
      }

      public StringOptionBuilder shortOption(String shortOption) {
        this.shortOption = shortOption;
        return this;
      }

      public StringOptionBuilder longOption(String longOption) {
        this.longOption = longOption;
        return this;
      }

      public StringOptionBuilder predicates(StringOptionPredicate... predicates) {
        this.predicates = predicates;
        return this;
      }

      public StringOptionBuilder defaultValue(String defaultValue) {
        this.defaultValue = Optional.of(defaultValue);
        return this;
      }

      public StringOption build() {
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

        return new StringOption(optionBuilder.build(), defaultValue, predicates);
      }
    }

    public static StringOptionBuilder builder() {
      return new StringOptionBuilder();
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

    public boolean hasOption(CommandLine commandLine) {
      return commandLine.hasOption(option.getOpt());
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) {
      if (commandLine == null) {
        throw new NullPointerException(String.format(
            "Tried to parse argument [ -%s ] from 'null' command line!", option.getOpt()));
      }

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

    public static class PathOptionBuilder {

      private String shortOption;
      private String longOption;

      private Optional<Path> defaultValue = Optional.empty();
      private String description = "There is no description for the option";
      private PathOptionPredicate[] predicates = new PathOptionPredicate[0];

      private PathOptionBuilder() {
      }

      public PathOptionBuilder description(String description) {
        this.description = description;
        return this;
      }

      public PathOptionBuilder shortOption(String shortOption) {
        this.shortOption = shortOption;
        return this;
      }

      public PathOptionBuilder longOption(String longOption) {
        this.longOption = longOption;
        return this;
      }

      public PathOptionBuilder predicates(PathOptionPredicate... predicates) {
        this.predicates = predicates;
        return this;
      }

      public PathOptionBuilder defaultValue(String defaultValue) {
        this.defaultValue = Optional.of(Paths.get(defaultValue));
        return this;
      }

      public PathOptionBuilder defaultValue(Path defaultValue) {
        this.defaultValue = Optional.of(defaultValue);
        return this;
      }

      public PathOption build() {
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

        return new PathOption(optionBuilder.build(), defaultValue, predicates);
      }
    }

    public static PathOptionBuilder builder() {
      return new PathOptionBuilder();
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

    public boolean hasOption(CommandLine commandLine) {
      return commandLine.hasOption(option.getOpt());
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) {
      if (commandLine == null) {
        throw new NullPointerException(String.format(
            "Tried to parse argument [ -%s ] from 'null' command line!", option.getOpt()));
      }

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

    public static class EnumOptionBuilder<T extends Enum<T>> {

      private String shortOption;
      private String longOption;
      private Class<T> enumType;

      private Optional<T> defaultValue = Optional.empty();
      private String description = "There is no description for the option";

      private EnumOptionBuilder(Class<T> enumType) {
        this.enumType = enumType;
      }

      public EnumOptionBuilder<T> description(String description) {
        this.description = description;
        return this;
      }

      public EnumOptionBuilder<T> shortOption(String shortOption) {
        this.shortOption = shortOption;
        return this;
      }

      public EnumOptionBuilder<T> longOption(String longOption) {
        this.longOption = longOption;
        return this;
      }

      public EnumOptionBuilder<T> defaultValue(T defaultValue) {
        this.defaultValue = Optional.of(defaultValue);
        return this;
      }

      public EnumOption<T> build() {
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

        return new EnumOption<>(optionBuilder.build(), defaultValue, enumType);
      }
    }

    public static <T extends Enum<T>> EnumOptionBuilder<T> builder(Class<T> enumType) {
      return new EnumOptionBuilder<>(enumType);
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

    public boolean hasOption(CommandLine commandLine) {
      return commandLine.hasOption(option.getOpt());
    }

    public void addToOptions(Options options) {
      options.addOption(option);
    }

    @Override
    public void check(CommandLine commandLine) {
      if (commandLine == null) {
        throw new NullPointerException(String.format(
            "Tried to parse argument [ -%s ] from 'null' command line!", option.getOpt()));
      }

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
