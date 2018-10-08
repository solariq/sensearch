package components.query;

import components.query.term.Term;

import java.util.List;

public interface Query {
    public List<Term> getTerms();
}
