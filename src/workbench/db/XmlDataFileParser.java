/*
 * XmlDataFileParser.java
 *
 * Created on October 15, 2003, 11:59 PM
 */

package workbench.db;

import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import workbench.log.LogMgr;

/**
 *
 * @author  thomas
 */
public class XmlDataFileParser
extends DefaultHandler
{
	private String inputFile;
	private String tableName;
	
	private List columnNames;
	private List columnClasses;

	/** Creates a new instance of XmlDataFileParser */
	public XmlDataFileParser(String inputFile)
	{
		this.inputFile = inputFile;
	}
	
	public void parse()
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try
		{
			// Parse the input
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse( this.inputFile, this);
		}
		catch (Exception e)
		{
			LogMgr.logError("XmlDataFileparser.parse()", "Error when parsing XML file", e);
		}
	}
	
	public void startDocument()
		throws SAXException
	{
	}
	
	public void endDocument()
		throws SAXException
	{
	}
	
	public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
		throws SAXException
	{
		System.out.println("processing element " + qName);
	}
	
	public void endElement(String namespaceURI, String sName, String qName)
		throws SAXException
	{
		System.out.println("done with element " + qName);
	}
	
	public void characters(char buf[], int offset, int len)
		throws SAXException
	{
		String s = new String(buf, offset, len);
		System.out.println("content: " + s);
	}
	
	public void ignorableWhitespace(char buf[], int offset, int len)
		throws SAXException
	{
		// Ignore it
	}
	
	//===========================================================
	// SAX ErrorHandler methods
	//===========================================================
	
	// treat validation errors as fatal
	public void error(SAXParseException e)
		throws SAXParseException
	{
		throw e;
	}
	
	// dump warnings too
	public void warning(SAXParseException err)
		throws SAXParseException
	{
		System.out.println("** Warning, line " + err.getLineNumber() + ", uri " + err.getSystemId());
		System.out.println("   " + err.getMessage());
	}
	

	public static void main(String[] args)
	{
		XmlDataFileParser parser = new XmlDataFileParser("d:/temp/test1.xml");
		parser.parse();
	}
}
