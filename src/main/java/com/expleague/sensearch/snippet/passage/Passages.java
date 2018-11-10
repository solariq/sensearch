package com.expleague.sensearch.snippet.passage;

import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.query.term.BaseTerm;
import com.expleague.sensearch.query.term.Term;
import com.expleague.sensearch.snippet.Segment;
import java.util.ArrayList;
import java.util.List;

public class Passages {

  public static boolean contains(Lemmer lemmer, CharSequence s, CharSequence t) {
    /*return lemmer.myStem
        .parse(s)
        .stream()
        .anyMatch(x -> {
          Term term = new BaseTerm(x);
          return term.getNormalized() == t;
        });*/
    return !containsSelection(s, t).isEmpty();
  }

  public static List<Segment> containsSelection(CharSequence text, CharSequence t) {
    final CharSequence s = text.toString().toLowerCase();
    int n = s.length();
    int m = t.length();

    List<Segment> selection = new ArrayList<>();

    // Алгоритм Бойера — Мура

    int[] suffixShift = new int[m + 1];
    for (int i = 0; i < m + 1; ++i) {
      suffixShift[i] = m;
    }
    int[] z = new int[m];

    for (int j = 1, maxJ = 0, maxZ = 0; j < m; ++j) {
      if (j <= maxZ) {
        z[j] = Math.min(maxZ - j + 1, z[j - maxJ]);
      }
      while (j + z[j] < m && t.charAt(m - 1 - z[j]) == t.charAt(m - 1 - (j + z[j]))) {
        z[j]++;
      }
      if (j + z[j] - 1 > maxZ) {
        maxJ = j;
        maxZ = j + z[j] - 1;
      }
    }
    for (int j = m - 1; j > 0; --j) {
      suffixShift[m - z[j]] = j;
    }
    for (int j = 1, r = 0; j <= m - 1; ++j) {
      if (j + z[j] == m) {
        for (; r <= j; r++) {
          if (suffixShift[r] == m) {
            suffixShift[r] = j;
          }
        }
      }
    }

    boolean ok = false;
    int j, bound = 0;
    for (int i = 0; i <= n - m; i += suffixShift[j + 1]) {
      for (j = m - 1; j >= bound && t.charAt(j) == s.charAt(i + j); j--) {
        ;
      }
      if (j < bound) {
        ok = true;
        if ((i == 0 || !Character.isAlphabetic(s.charAt(i - 1))) && (i + m == n || !Character
            .isAlphabetic(s.charAt(i + m)))) {
          selection.add(new Segment(i, i + m));
        }
        bound = m - suffixShift[0];
        j = -1;
      } else {
        bound = 0;
      }
    }
    return selection;
  }
}
