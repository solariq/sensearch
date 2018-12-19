package com.expleague.sensearch.metrics;

import com.expleague.commons.seq.CharSeqTools;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LSHSynonymsMetric {
    private final Path path;

    public LSHSynonymsMetric(Path path) {
        this.path = path;
    }

    public double calc(long mainId, Set<Long> LSHSet) {
        File file = path.resolve("_" + mainId).toFile();
        if (file.exists()) {
            Set<Long> defaultSet = new HashSet<>();
            try (Reader in = new InputStreamReader(new FileInputStream(file))) {
                CharSeqTools.lines(in).forEach(line -> {
                    CharSequence[] parts = CharSeqTools.split(line, ' ');
                    Arrays.stream(parts).forEach(cs -> defaultSet.add(CharSeqTools.parseLong(cs)));
                });
            } catch (IOException ignored) {}
            LSHSet.retainAll(defaultSet);
            return ((double) LSHSet.size()) / ((double) defaultSet.size());
        }
        return -1.;
    }
}