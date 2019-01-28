package com.expleague.sensearch.core;

import java.io.IOException;
import java.util.List;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Russian;
import org.languagetool.rules.RuleMatch;

public class SpellChecker {

  private static final JLanguageTool jLanguageTool = new JLanguageTool(new Russian());

  public CharSequence correct(CharSequence uncorrected) throws IOException {
    List<RuleMatch> rules = jLanguageTool.check(uncorrected.toString());
    StringBuilder result = new StringBuilder();
    int lastInd = 0;

    for (RuleMatch rule : rules) {
      String suggestion =
          rule.getSuggestedReplacements().isEmpty() ? "" : rule.getSuggestedReplacements().get(0);
      result.append(uncorrected.subSequence(lastInd, rule.getFromPos()));
      result.append(suggestion);
      lastInd = rule.getToPos();
    }

    return rules.isEmpty() ? uncorrected : result.toString();
  }
}
