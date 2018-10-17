package components;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import components.snippeter.SnippetsCreator;
import components.snippeter.snippet.Passage;
import components.snippeter.snippet.Snippet;

public class PageLoadHandler extends AbstractHandler {
	
	TestSuggessionsProvider suggestor = new TestSuggessionsProvider();
	
	SnippetBox snipBox = new TestSnippetBox();
	
	ObjectMapper mapper = new ObjectMapper();
	
	@Override
	public void handle(String target,
            Request baseRequest,
            HttpServletRequest request,
            HttpServletResponse response) 
            			throws IOException, ServletException
	{
		
		String requestText = request.getReader().readLine();
		
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		
		if (requestText == null || requestText.isEmpty()) {
			
			try(BufferedReader in = new BufferedReader(new FileReader(new File("resources/mainPage.html")))) {
				response.getWriter().println(in.lines().collect(Collectors.joining("\n")));
			}
			
			String searchString = request.getParameter("searchForm");
			if (searchString != null) {
				response.getWriter().println("<br>Результаты по запросу \"" + searchString + "\":<br><br>");
				
				snipBox.makeQuery(searchString);
				
				for (int i = 0; i < snipBox.size(); i++) {
					Snippet snippet = snipBox.getSnippet(i);
					response.getWriter().println("<br><strong>" + (i+1) + ". " + snippet.getTitle() + "</strong><br>");
					//for (Passage p : snippet.getContent().getSentence()) {
						response.getWriter().println(snippet.getContent() + "...");
					//}
				}
				response.getWriter().println("</body></html>");
			}
		} else {
			response.getWriter().println(mapper.writeValueAsString(suggestor.getSuggestions(requestText)));
		}
	}

}
