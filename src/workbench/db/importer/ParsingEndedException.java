/*
 * ParsingEndedException.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import org.xml.sax.SAXException;

/**
 *
 * @author  Thomas Kellerer
 */
public class ParsingEndedException
	extends SAXException
{
	
	public ParsingEndedException()
	{
		super("Parsing ended");
	}
	
}
