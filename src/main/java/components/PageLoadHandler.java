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

import components.snippeter.snippet.Cluster;
import components.snippeter.snippet.Passage;

public class PageLoadHandler extends AbstractHandler {
	
	TestSuggessionsProvider suggestor = new TestSuggessionsProvider();
	TestSnippetProvider testSnip = new TestSnippetProvider();
	
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
			BufferedReader in = new BufferedReader(new FileReader(new File("resources/mainPage.html")));
			response.getWriter().println(in.lines().collect(Collectors.joining("\n")));
			
			String searchString = request.getParameter("searchForm");
			if (searchString != null) {
				response.getWriter().println("<br>Результаты по запросу \"" + searchString + "\":<br><br>");
				Cluster snippets = testSnip.getSuitableSnippets(searchString);
				int nsnip = 1;
				for (Passage p : snippets.getPassages()) {
					response.getWriter().println(nsnip++ + ". " + p.getSentence() + "<br>");
				}
				response.getWriter().println("</body></html>");
			}
		} else {
			response.getWriter().println(mapper.writeValueAsString(suggestor.getSuggestions(requestText)));
		}
	}

}
