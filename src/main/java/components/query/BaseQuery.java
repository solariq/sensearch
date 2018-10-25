package components.query;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.text.lemmer.MyStem;
import components.Constants;
import components.query.term.BaseTerm;
import components.query.term.Term;

public class BaseQuery implements Query {

  MyStem myStem = new MyStem(Paths.get(Constants.getMyStem()));
  List<Term> terms;
  Vec queryVector;

  public BaseQuery(CharSequence queryString) {
    String regex = " ";
    Pattern pattern = Pattern.compile(regex);
    terms = new ArrayList<>();

    for (CharSequence word : pattern.split(queryString)) {
      this.terms.add(new BaseTerm(myStem.parse(word).get(0)));
    }
  }

  public List<Term> getTerms() {
    return this.terms;
  }

  public Vec getQueryVector() {
    return this.queryVector;
  }
}
