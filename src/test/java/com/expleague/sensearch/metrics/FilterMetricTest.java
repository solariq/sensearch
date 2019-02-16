package com.expleague.sensearch.metrics;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class FilterMetricTest {

    private final MeasureFilterQuality filterMetric = new MeasureFilterQuality();

    private void testTemplate(String queryStr, Set<URI> URIs, DoublePredicate predicate) {
        Assert.assertTrue(
            predicate.test(filterMetric.calc(queryStr, URIs))
        );
    }

    @Test
    public void negativeValueTest() {
        String queryStr = "вася";
        Set<URI> URIs = Collections.emptySet();
        DoublePredicate predicate = v -> v < 0.;
        testTemplate(queryStr, URIs, predicate);
    }

    @Test
    public void positiveValueTest() {
        String queryStr = "александр";
        String[] URIStrs = new String[] {
            "https://ru.wikipedia.org/wiki/Александр_Македонский",
            "https://ru.wikipedia.org/wiki/Носик,_Александр_Валерьевич",
            "https://ru.wikipedia.org/wiki/Петров,_Александр_Андреевич_(актёр)"

        };
        Set<URI> URIs = Arrays.stream(URIStrs).map(URI::create).collect(Collectors.toSet());
        DoublePredicate predicate = v -> v > 0.;
        testTemplate(queryStr, URIs, predicate);
    }
}
