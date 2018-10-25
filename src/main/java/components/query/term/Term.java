package components.query.term;

public interface Term {

  public CharSequence getRaw();

  public CharSequence getNormalized();

  //public LemmaInfo getLemma();
}
