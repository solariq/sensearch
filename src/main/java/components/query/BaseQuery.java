package components.query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import components.query.term.BaseTerm;
import components.query.term.Term;

public class BaseQuery implements Query{

    List<Term> terms;

    public BaseQuery() {
        terms = new ArrayList<Term>();
    }

    public BaseQuery(CharSequence queryString) {
        String regex = " ";
        Pattern pattern = Pattern.compile(regex);

        terms = new ArrayList<>();
        for (CharSequence word: pattern.split(queryString)) {
            this.terms.add(new BaseTerm(word));
        }
    }

    public List<Term> getTerms() {
        return this.terms;
    }
}
