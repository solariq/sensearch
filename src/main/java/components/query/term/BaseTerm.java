package components.query.term;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.WordInfo;

public class BaseTerm implements Term{

    private WordInfo wordInfo;
    private Vec vector;

    public BaseTerm(WordInfo wordInfo) {
        this.wordInfo = wordInfo;
        //this.vector = getVec() ?
    }

    public CharSequence getRaw() {
        return wordInfo.token();
    }

    public CharSequence getNormalized(){
        return this.wordInfo.lemma().lemma();
    }

    public LemmaInfo getLemma() {
        return this.wordInfo.lemma();
    }
}
