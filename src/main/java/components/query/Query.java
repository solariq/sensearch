package components.query;

import com.expleague.commons.math.vectors.Vec;
import components.query.term.Term;
import java.util.List;

public interface Query {

  public List<Term> getTerms();

  public Vec getQueryVector();
}
