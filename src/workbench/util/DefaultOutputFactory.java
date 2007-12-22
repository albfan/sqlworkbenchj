/*
 * DefaultOutputFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 *
 * @author support@sql-workbench.net
 */
public class DefaultOutputFactory
	implements OutputFactory
{
	
	public DefaultOutputFactory()
	{
	}

	public boolean isArchive() { return false; }
	
	public OutputStream createOutputStream(File output) 
		throws IOException
	{
		return new FileOutputStream(output);
	}

	public Writer createWriter(File output, String encoding)
		throws IOException
	{
		OutputStream out = createOutputStream(output);
		return EncodingUtil.createWriter(out, encoding);
	}
	
	public void done() 
		throws IOException
	{
	}
	
}
