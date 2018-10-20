package components;

import org.eclipse.jetty.server.Server;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SearchServer {
	
	private static Path pathToZIP = Paths.get("../WikiDocs/Mini_Wiki.zip");
 
    public static void main(String[] args) throws Exception {
        Server server = new Server(8082);
        //server.setHandler(new HandlerCollection(new Handler[] {new PageLoadHandler(), new SuggestionsHandler()}));
        server.setHandler(new PageLoadHandler(pathToZIP));

        server.start();
        server.join();
    }
}
