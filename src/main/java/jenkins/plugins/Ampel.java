package jenkins.plugins;

import hudson.model.BuildListener;

import java.io.IOException;
import java.net.URL;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Gana Tserendondov
 * @version version
 * @since jdk-version
 */
public class Ampel{
	
	private final String SEARCH_XPATH = "//*/td[1]/img";
	private final String SEARCH_ITEM = "src";
	private final String SRC_IMAGE_BLUE = "blue";
	private final String SRC_IMAGE_RED = "red";
	
	
	private int green = 0;
	private int red = 0;
	private HTMLParser parser;
	private int prozent;
	private String content;

	/**
	 * 
	 * @param URL url
	 */
	public Ampel(URL url) {
		parser = new HTMLParser();
	}

	/**
	 * Holt den Content der url und wertet die img's aus.
	 * 
	 * @param URL url
	 * @param BuildListener listener
	 * @return int percentage
	 */
	public int crawlAndTest(URL url, BuildListener listener) {
		parser.openConnection();
		try {
			content = parser.getURLAndContent(parser.request(url));
		} catch (IOException e) {
			System.out.println(e);
		}
		this.prozent = testContent(content, listener);
		parser.closeConnection();
		return this.prozent;
	}
	
	/**
	 * 
	 * @param content
	 * @param listener
	 * @return int prozent
	 */
	private int testContent(String content, BuildListener listener) {
		NodeList nodes = parser.getXPathLinks(content, parser.getXPathExpression(this.SEARCH_XPATH));
		for (int i = 0; nodes != null && i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getAttributes().getNamedItem(this.SEARCH_ITEM) != null){
				String image = node.getAttributes().getNamedItem(this.SEARCH_ITEM).getNodeValue();
				if(image.contains(this.SRC_IMAGE_BLUE)){
					green = green + 1;
				} else if (image.contains(this.SRC_IMAGE_RED)) {
					red = red + 1;
				}
			}
		}
		float a = green + red;
		prozent = (int) (100/a*green);
		return prozent;
	}
}