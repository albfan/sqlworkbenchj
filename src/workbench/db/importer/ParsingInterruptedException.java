/*
 * ParsingInterruptedException.java
 *
 * Created on November 22, 2003, 1:21 AM
 */

package workbench.db.importer;

import org.xml.sax.SAXException;

/**
 *
 * @author  workbench@kellerer.org
 */
public class ParsingInterruptedException
	extends SAXException
{
	
	/** Creates a new instance of ParsingInterruptedException */
	public ParsingInterruptedException()
	{
		super("Parsing cancelled");
	}
	
}
