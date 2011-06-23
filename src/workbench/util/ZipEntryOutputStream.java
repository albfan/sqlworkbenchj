/*
 * ZipEntryOutputStream.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Thomas Kellerer
 */
public class ZipEntryOutputStream
	extends OutputStream
{

	private ZipOutputStream zout;

	public ZipEntryOutputStream(ZipOutputStream out)
	{
		zout = out;
	}

	@Override
	public void close()
		throws IOException
	{
		zout.closeEntry();
	}

	@Override
	public void flush()
		throws IOException
	{
		zout.flush();
	}

	@Override
	public void write(byte[] b, int off, int len)
		throws IOException
	{
		zout.write(b, off, len);
	}

	@Override
	public void write(byte[] b)
		throws IOException
	{
		zout.write(b);
	}

	@Override
	public void write(int b)
		throws IOException
	{
		zout.write(b);
	}
}
