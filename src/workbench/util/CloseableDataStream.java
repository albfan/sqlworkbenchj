/*
 * CloseableDataStream.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * A wrapper class for any "file" object that can be closed.
 * (In JDK 1.5 the Closeable interface could be used, but I want to stay compatible with 1.4)
 * @author support@sql-workbench.net
 */
public class CloseableDataStream
{
	private OutputStream outputStream;
	private Writer writer;
	private InputStream inputStream;
	private Reader reader;
	
	public CloseableDataStream(InputStream s)
	{
		inputStream = s;
	}
	public CloseableDataStream(Reader r)
	{
		reader = r;
	}
	public CloseableDataStream(OutputStream o)
	{
		outputStream = o;
	}
	public CloseableDataStream(Writer w)
	{
		writer = w;
	}
	
	public void close()
	{
		try
		{
			if (inputStream != null) inputStream.close();
			else if (reader != null) reader.close();
			else if (writer != null) writer.close();
			else if (outputStream != null) outputStream.close();
		}
		catch (Throwable th)
		{
		}
	}
	
}
