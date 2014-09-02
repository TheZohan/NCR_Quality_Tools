package tfsAccess;
import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;  
import javax.xml.parsers.SAXParser;  
import javax.xml.parsers.SAXParserFactory;  
import org.xml.sax.Attributes;  
import org.xml.sax.SAXException;  
import org.xml.sax.helpers.DefaultHandler;  

public class TFSQueryReader {
	private SAXHandler handler; 
	    
	  public TFSQueryReader (File input_file) throws Exception {  
	    SAXParserFactory parserFactor = SAXParserFactory.newInstance();  
	        
	    handler = new SAXHandler();  
	     
	    try {  
	        SAXParser parser = parserFactor.newSAXParser();  
	        parser.parse(input_file, handler);  
	      } catch (ParserConfigurationException e) {  
	        System.out.println("ParserConfig error");  
	      } catch (SAXException e) {  
	        System.out.println("SAXException : xml not well formed");  
	      } catch (IOException e) {  
	        System.out.println("IO error, check filename");  
	      }  
	      
	  } 
	    
	  public String getWiqlQuery() { 
	      return handler.retWiqlQuery(); 
	  } 
	  public String getProjectName () {
		  return handler.retProjectName();
	  }
	}  
	/**  
	 * The Handler for SAX Events.  
	 */
	class SAXHandler extends DefaultHandler {  
	  String wiqlQuery = new String();
	  String projectName = new String();
	  boolean getStringFlag = false;
	  boolean getProjectName = false;
	  String content, content2;  
	  @Override
	  //Triggered when the start of tag is found.  
	  public void startElement(String uri, String localName,   
	                           String elementName, Attributes attributes)   
	                           throws SAXException {  
	    
	    switch(elementName){  
	      case "Wiql":  
	    	  getStringFlag = true;
	        break;  
	      case "TeamProject":
	    	  getProjectName = true;
	    }  
	  }  
	    
	  @Override
	  public void endElement(String uri, String localName,   
	                         String elementName) throws SAXException {  
	   switch(elementName){  
	     case "Wiql":  
	    	 getStringFlag = false;       
	       break;   
	     case "TeamProject":
	       getProjectName = false;
	       break;
	   }
	  }  
	    
	  @Override
	  public void characters(char[] ch, int start, int length)   
	          throws SAXException {
		  if (getStringFlag == true) {
			  content = new String(ch, start, length);  
			  wiqlQuery += content;
		  }
		  if (getProjectName == true) {
			  content2 = new String(ch, start, length);
			  projectName += content2;
		  }
	  }  

	  public String retWiqlQuery(){ 
	      return this.wiqlQuery; 
	  } 
	  public String retProjectName() {
		  return this.projectName;
	  }
}
