/*
 * LobFileParameter.java
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

import java.io.Closeable;

/**
 * @author Thomas Kellerer
 */
public class LobFileParameter
{
	private Closeable dataStream;
	private String filename;
	private String encoding;
	private boolean binary;

	public LobFileParameter()
	{
	}

	public LobFileParameter(String fname, String enc, boolean isBinary)
	{
		setFilename(fname);
		setEncoding(enc);
		setBinary(isBinary);
	}

	@Override
	public String toString()
	{
		return "filename=[" + filename + "], binary=" + binary + ", encoding=" + encoding;
	}

	public void setDataStream(Closeable in)
	{
		this.dataStream = in;
	}

	public void close()
	{
		FileUtil.closeQuietely(dataStream);
	}

	public final void setBinary(boolean flag)
	{
		binary = flag;
	}

	public boolean isBinary()
	{
		return binary;
	}

	public String getFilename()
	{
		return filename;
	}

	public final void setFilename(String fname)
	{
		filename = fname;
	}

	public String getEncoding()
	{
		return encoding;
	}

	public final void setEncoding(String enc)
	{
		encoding = enc;
	}
}
