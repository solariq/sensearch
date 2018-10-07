package components;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class SearchServer extends AbstractHandler
{
	TestSnippetProvider snippetProvider = new TestSnippetProvider();
	
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        
        StringBuilder responseBuilder = new StringBuilder();
        String searchString = request.getParameter("searchform");
        responseBuilder.append(
        		"<form>"
        		+ "<center>"
        		+ "Введите поисковый запрос <br>"
        		+ "<input type=\"text\" name=\"searchform\">"
        		+ "<br>"
        		+ "<input type = \"submit\">"
        		+ "<br>"
        		+ request.getParameter("searchform")
        		+ "</center>"
        		+ "</form>"
        	);
        
        responseBuilder.append("<center>");
        List<String> snippets = snippetProvider.getSuitableSnippets(searchString);
        int count = 0;
        
        for (String s : snippets) {
        	count++;
        	responseBuilder.append(count + ". <b>" + s + "</b><br>");
        }
        
        responseBuilder.append("</center>");
        
        response.getWriter().println(responseBuilder.toString());
    }
 
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);
        server.setHandler(new SearchServer());
 
        server.start();
        server.join();
    }
}
