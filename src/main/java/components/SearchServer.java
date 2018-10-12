package components;

import org.eclipse.jetty.server.Server;

public class SearchServer {
	
	TestSnippetProvider snippetProvider = new TestSnippetProvider();
 
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        //server.setHandler(new HandlerCollection(new Handler[] {new PageLoadHandler(), new SuggestionsHandler()}));
        server.setHandler(new PageLoadHandler());

        server.start();
        server.join();
    }
}
