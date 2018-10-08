package components.query.term;


public class BaseTerm implements Term{

    CharSequence rawWord;
    CharSequence normalizedWord;
    //Lemma lemma;

    public BaseTerm() {
        this.rawWord = "";
        this.normalizedWord = "";
    }

    public BaseTerm(CharSequence word) {
        this.rawWord = word;
        //todo: normalize word
        this.normalizedWord = word;
    }

    public CharSequence getRaw() {
        return this.rawWord;
    }

    public CharSequence getNormalized(){
        return this.normalizedWord;
    }

    /*public Lemma getLemma() {
        return this.lemma;
     */
}
