package components;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SearchServer {

    //Путь до архива
	private static Path pathToZIP = Paths.get("../WikiDocs/Mini_Wiki.zip");

    private static ObjectMapper  objectMapper = new ObjectMapper();

    private static void init() {
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get("./resources/paths.json"));
            objectMapper.readValue(jsonData, Constants.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

 
    public static void main(String[] args) throws Exception {
        init();

        Server server = new Server(8081);

        //server.setHandler(new HandlerCollection(new Handler[] {new PageLoadHandler(), new SuggestionsHandler()}));
        server.setHandler(new PageLoadHandler(pathToZIP.toAbsolutePath()));

        server.start();
        server.join();
    }
}
