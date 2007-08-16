/*
 * LobFileParameter.java
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

import java.io.Closeable;
import workbench.util.FileUtil;

/**
 * @author support@sql-workbench.net
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
    FileUtil.closeQuitely(dataStream);
	}
	
	public void setBinary(boolean flag) { binary = flag; }
	public boolean isBinary() { return binary; }
	public String getFilename() { return filename; }
	public void setFilename(String fname) { filename = fname; }
	
	public String getEncoding() { return encoding; }
	public void setEncoding(String enc) { encoding = enc; }
}
