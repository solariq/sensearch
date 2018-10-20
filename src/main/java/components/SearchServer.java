package components;

import org.eclipse.jetty.server.Server;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SearchServer {

    //Путь до архива
	private static Path pathToZIP = Paths.get("../WikiDocs/Mini_Wiki.zip");
 
    public static void main(String[] args) throws Exception {
        Server server = new Server(8081);
        //server.setHandler(new HandlerCollection(new Handler[] {new PageLoadHandler(), new SuggestionsHandler()}));
        server.setHandler(new PageLoadHandler(pathToZIP));

        server.start();
        server.join();
    }
}
