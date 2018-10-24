package components;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import components.snippeter.snippet.Segment;
import components.snippeter.snippet.Snippet;
import components.suggestor.BigramsBasedSuggestor;
import components.suggestor.Suggestor;

public class PageLoadHandler extends AbstractHandler {

	//SuggestsProvider suggestor = new TestSuggessionsProvider();
	Suggestor suggestor;
	
	//SnippetBox snipBox = new TestSnippetBox();
	SnippetBox snipBox;
	
	ObjectMapper mapper = new ObjectMapper();
	
	public PageLoadHandler(Path path) throws IOException {
		snipBox = new SnippetBoxImpl(path);
		//snipBox = new TestSnippetBox();
		suggestor = new BigramsBasedSuggestor(path);
		//suggestor = new TestSuggessionsProvider();
	}
	
	CharSequence generateBoldedText(CharSequence plain, List<Segment> segments) {
		
		StringBuilder strb = new StringBuilder();
		int left = 0;
		
		for (Segment segment : segments) {
			strb.append(plain.subSequence(left, segment.getLeft()));
			strb.append("<strong>")
			.append(plain.subSequence(segment.getLeft(), segment.getRight())).append("</strong>");
			left = segment.getRight();
		}
		
		strb.append(plain.subSequence(left, plain.length()));
		
		return strb.toString();
	}
	
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
			
			try(BufferedReader in = new BufferedReader(new FileReader(new File(Constants.getMainPageHTML())))) {
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
						response.getWriter().println(
								generateBoldedText(snippet.getContent(),
										snippet.getSelection()) + "...");
					//}
				}
				response.getWriter().println("</body></html>");
			}
		} else {
			response.getWriter().println(mapper.writeValueAsString(suggestor.getSuggestions(requestText)));
		}
	}

}
