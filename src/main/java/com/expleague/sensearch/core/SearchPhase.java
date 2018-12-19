package com.expleague.sensearch.core;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface SearchPhase extends Predicate<Whiteboard>, Consumer<Whiteboard> {
}
