import jetbrains.exodus.bindings.IntegerBinding;
import jetbrains.exodus.bindings.LongBinding;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;


public class Main {
  public static void main(String[] args) throws Exception {
    Environment env = Environments.newInstance("/tmp/sandulmv/tmpDB");
    Transaction simpleTrans = env.beginTransaction();
    Store store = env.openStore("simpleStore", StoreConfig.USE_EXISTING, simpleTrans);
    store.put(simpleTrans, LongBinding.longToEntry(1), StringBinding.stringToEntry("Document 1"));
    simpleTrans.flush();
    simpleTrans.commit();

    Transaction newTrans = env.beginTransaction();
    Store store1 = env.openStore("simpleStore1", StoreConfig.WITH_DUPLICATES_WITH_PREFIXING, newTrans);
    store1.put(newTrans, StringBinding.stringToEntry("Key1"), LongBinding.longToEntry(1));
    newTrans.flush();
    newTrans.commit();

    env.close();
  }
}
