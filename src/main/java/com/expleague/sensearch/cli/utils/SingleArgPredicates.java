package com.expleague.sensearch.cli.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

public class SingleArgPredicates {
  public interface AbstractArgPredicate {
    String failMessage();
  }
  public interface IntOptionPredicate extends IntPredicate, AbstractArgPredicate {
  }

  public interface DoubleOptionPredicate extends DoublePredicate, AbstractArgPredicate {
  }

  public interface LongOptionPredicate extends LongPredicate, AbstractArgPredicate {
  }

  public interface StringOptionPredicate extends Predicate<String>, AbstractArgPredicate {
  }

  public interface PathOptionPredicate extends Predicate<Path>, AbstractArgPredicate {
  }

  public static class PositiveInteger implements IntOptionPredicate {
    private static final PositiveInteger SINGLETON = new PositiveInteger();
    private static final IntOptionPredicate NON_POSITIVE = new PositiveInteger() {
      @Override
      public String failMessage() {
        return "Value must be non positive";
      }

      @Override
      public boolean test(int value) {
        return !super.test(value);
      }

      @Override
      public IntOptionPredicate negate() {
        return SINGLETON;
      }
    };

    private PositiveInteger() {}

    public static PositiveInteger get() {
      return SINGLETON;
    }
    @Override
    public String failMessage() {
      return "Value must be positive!";
    }

    @Override
    public IntOptionPredicate negate() {
      return NON_POSITIVE;
    }

    @Override
    public boolean test(int value) {
      return value > 0;
    }
  }

  public static class NegativeLong implements LongOptionPredicate {
    private static final NegativeLong SINGLETON = new NegativeLong();
    private static final LongOptionPredicate NON_NEGATIVE = new NegativeLong() {
      @Override
      public String failMessage() {
        return "Value must be non negative";
      }

      @Override
      public boolean test(long value) {
        return !super.test(value);
      }

      @Override
      public LongOptionPredicate negate() {
        return SINGLETON;
      }
    };

    private NegativeLong() {}

    public static NegativeLong get() {
      return SINGLETON;
    }

    @Override
    public String failMessage() {
      return "Value must be negative!";
    }

    @Override
    public boolean test(long value) {
      return value < 0;
    }

    @Override
    public LongOptionPredicate negate() {
      return NON_NEGATIVE;
    }
  }

  public static class SegmentDouble implements DoubleOptionPredicate {
    private final double lower;
    private final double upper;

    public static SegmentDouble get(double lower, double upper) {
      return new SegmentDouble(lower, upper);
    }

    private SegmentDouble(double lower, double upper) {
      this.lower = lower;
      this.upper = upper;
    }

    @Override
    public String failMessage() {
      return String.format("Value must be within the range [%f; %f]", lower, upper);
    }

    @Override
    public boolean test(double value) {
      return value >= lower && value <= upper;
    }
    // TODO: override hashCode and equals for the predictae
  }

  public static class PositiveDouble implements DoubleOptionPredicate {
    private static final PositiveDouble SINGLETON = new PositiveDouble();
    private static final DoubleOptionPredicate NON_POSITIVE = new PositiveDouble() {
      @Override
      public String failMessage() {
        return "Value must be non positive!";
      }

      @Override
      public boolean test(double value) {
        return !super.test(value);
      }

      @Override
      public DoubleOptionPredicate negate() {
        return SINGLETON;
      }
    };
    private PositiveDouble() {}

    public static DoubleOptionPredicate get() {
      return SINGLETON;
    }

    public static DoubleOptionPredicate negated() {
      return NON_POSITIVE;
    }

    @Override
    public String failMessage() {
      return "Value must be positive!";
    }

    @Override
    public boolean test(double value) {
      return value > 0;
    }

    @Override
    public DoubleOptionPredicate negate() {
      return NON_POSITIVE;
    }
  }

  public static class ExistingPath implements PathOptionPredicate {
    private static final ExistingPath SINGLETON = new ExistingPath();
    private static final PathOptionPredicate NON_EXISTING_PATH = new ExistingPath() {
      @Override
      public String failMessage() {
        return "Path must not exist";
      }

      @Override
      public boolean test(Path path) {
        return !super.test(path);
      }

      @Override
      public PathOptionPredicate negate() {
        return SINGLETON;
      }
    };

    private ExistingPath() {}

    public static PathOptionPredicate get() {
      return SINGLETON;
    }

    public static PathOptionPredicate negated() {
      return NON_EXISTING_PATH;
    }

    @Override
    public String failMessage() {
      return "Path must exist!";
    }

    @Override
    public boolean test(Path path) {
      return Files.exists(path);
    }

    @Override
    public PathOptionPredicate negate() {
      return NON_EXISTING_PATH;
    }
  }
}
