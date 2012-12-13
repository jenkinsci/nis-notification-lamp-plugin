package jenkins.plugins;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;


/**
 * Diese Klasse ist für die 
 * 
 * HTML-Parser und HTTP-Client
 * 
 * @see JTidy (http://jtidy.sourceforge.net/)
 * 
 */
public class HTMLParser {
	
	private Tidy tidy;
	protected XPathFactory xfactory;
	protected XPath xpath;
	protected DefaultHttpClient httpClient;

	public HTMLParser() {
		tidy = new Tidy();
		// muss Unicode sein.
		tidy.setCharEncoding(3);
		tidy.setXHTML(true);
		tidy.setXmlOut(true);
		// kein warning
		tidy.setQuiet(true);
		tidy.setShowWarnings(false);
		tidy.setMakeClean(true);
		tidy.setEncloseBlockText(true);
		
		xfactory = XPathFactory.newInstance();
		xpath = xfactory.newXPath();
	}

	/**
	 * Erzeugt neue DefaultHttpClient.
	 * 
	 * @return void
	 */
	public void openConnection() {
        
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setContentCharset(params, "utf-8");
        params.setParameter(CoreProtocolPNames.USER_AGENT, "NIS Jenkins Webtest");
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, 50000);
        
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        registry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

        ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(registry);
        manager.setDefaultMaxPerRoute(100);
        manager.setMaxTotal(100);
        httpClient = new DefaultHttpClient(manager, params);
	}

	/**
	 * schliesst die Verbindung.
	 */
	public void closeConnection() {
		httpClient.getConnectionManager().shutdown();
	}

	/**
	 * Send the HTTP request and return an HTTP response
	 * 
	 * @param url
	 * @return Http response
	 * @throws IOException 
	 *
	 */
	public HttpResponse request(URL url){
		HttpContext context = new BasicHttpContext();
		HttpGet method = null;
		HttpResponse response = null;
			try {
				// nur httpGet
				method = new HttpGet(url.toString());
				// http client aufruf der url
				response = httpClient.execute(method, context);
			} catch (Exception e) {
				System.err.println(e);
			}
		return response;
	}
	
	public static String NEWLINE = System.getProperty("line.separator");
	
	

	/**
	 * Gibt den Content der URL zurueck.
	 * 
	 * @param URL
	 * @return String content;
	 * @throws IOException
	 */
	public String getURLAndContent(HttpResponse response) throws IOException {
		String content = null;
		if(response != null) {
			// nur text/html
			if (!response.getEntity().getContentType().getValue().toLowerCase().trim().startsWith("text/html")) {
				if (response.getEntity() != null) {
					EntityUtils.consume(response.getEntity());
				}
				return content;
			}
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String line;
				StringBuffer buffer = new StringBuffer();
				while ((line = reader.readLine()) != null) {
					buffer.append(line).append(NEWLINE);
				}
				content = buffer.toString();
			} catch (IOException e) {
				throw e;
			} finally {
				// close reader and http client
				if (reader != null) {
					try {
						reader.close();
					} catch (Exception e) {
						System.err.println(e);
					}
				}
				if (response.getEntity() != null) {
					EntityUtils.consume(response.getEntity());
				}
			}
		}
		return content;
	}
	
	/**
	 * Liefert ein XPathExpression aus den String
	 * 
	 * @param String xpath
	 * @return XPathExpression
	 */
	public XPathExpression getXPathExpression(String pathString){
		XPathExpression xpathSubNavi = null;
		try {
			xpathSubNavi = xpath.compile(pathString);
		} catch (XPathExpressionException e) {
			System.out.println("XPathExpressionException: getXPathExpression");
			System.out.println(e);
		}
		return xpathSubNavi;
	}
	
	/**
	 * Filtert alle XPath-Links und gibt NodeList Zurueck.
	 * 
	 * @param String content
	 * @param XPathExpression xpath
	 * @return NodeList
	 */
	public NodeList getXPathLinks(String content, XPathExpression xpath) {
		
		try {
			Document document = this.getDocument(content);
//			System.out.println(content);
			NodeList nodeList = (NodeList) xpath.evaluate(document, XPathConstants.NODESET);
			return nodeList;
		} catch (Exception e) {
			System.out.println("Exeption: HTMLParser.getXPathLinks");
			System.err.println(e.toString());
		}
		return null;
	}
	
	/**
	 * liest den Content und gibt Document zurück.
	 * 
	 * @param String content
	 * @return Document {@link Document}
	 * @throws Exception
	 */
	private Document getDocument(String content){
		
		InputStream sr = null;
		Document document = null;
		try {
			sr = new ByteArrayInputStream(content.getBytes("UTF-8"));
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		if (sr == null) {
			return null;
		}
		try {
			document = tidy.parseDOM(sr, null);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		
		String wellFormedContent = "";
		InputStream wellFormedContent1 = null;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		tidy.pprint(document, outputStream);
		wellFormedContent += new String(outputStream.toByteArray(), Charset.forName("UTF-8"));
		try {
			wellFormedContent1 = new ByteArrayInputStream(wellFormedContent.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			System.out.println(e1.toString());
		}
		try{
			document = tidy.parseDOM(wellFormedContent1, null);
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return document;
	}
}