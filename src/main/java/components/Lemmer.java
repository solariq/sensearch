package components;

import com.expleague.commons.text.lemmer.MyStem;
import java.nio.file.Paths;

public class Lemmer {

  private static Lemmer instance;

  public MyStem myStem;

  private Lemmer() {
    this.myStem = new MyStem(Paths.get(Constants.getMyStem()));
  }

  private Lemmer(String path) {
    this.myStem = new MyStem(Paths.get(path));
  }

  public static synchronized Lemmer getInstance() {
    if (instance == null) {
      instance = new Lemmer();
    }
    return instance;
  }
}
