package components;

import components.snippeter.Snippet;

public class TestSnippetBox implements SnippetBox {

  CharSequence s;
  private TestSnippetProvider provider = new TestSnippetProvider();

  @Override
  public int size() {
    // TODO Auto-generated method stub
    return 3;
  }

  @Override
  public boolean makeQuery(CharSequence s) {
    // TODO Auto-generated method stub
    this.s = s;
    return true;
  }

  @Override
  public Snippet getSnippet(int idx) {
    // TODO Auto-generated method stub
    return provider.getSuitableSnippets(s);
  }

}
