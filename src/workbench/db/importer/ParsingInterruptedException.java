/*
 * ParsingInterruptedException.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.importer;

import org.xml.sax.SAXException;

/**
 *
 * @author  info@sql-workbench.net
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
