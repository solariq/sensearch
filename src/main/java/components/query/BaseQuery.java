package components.query;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.text.lemmer.MyStem;
import components.Constants;
import components.embedding.impl.EmbeddingImpl;
import components.query.term.BaseTerm;
import components.query.term.Term;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BaseQuery implements Query {

    private MyStem myStem = new MyStem(Paths.get(Constants.getMyStem()));
    private List<Term> terms;
    private Vec queryVector;

    public BaseQuery(CharSequence queryString) {
        //todo replace for "smart" tokenizer when it zavezut
        String regex = " ";
        Pattern pattern = Pattern.compile(regex);
        terms = new ArrayList<>();

        for (CharSequence word : pattern.split(queryString)) {
            this.terms.add(new BaseTerm(myStem.parse(word).get(0), EmbeddingImpl.getInstance().getVec(word.toString())));
        }

        this.queryVector = EmbeddingImpl.getInstance().getVec(this.terms);
    }

    @Override
    public List<Term> getTerms() {
        return this.terms;
    }

    @Override
    public Vec getQueryVector() {
        return this.queryVector;
    }
}
