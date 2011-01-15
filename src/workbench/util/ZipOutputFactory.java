/*
 * ZipOutputFactory.java
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Thomas Kellerer
 */
public class ZipOutputFactory
	implements OutputFactory
{
	protected File archive;
	protected OutputStream baseOut;
	protected ZipOutputStream zout;
	
	public ZipOutputFactory(File zip)
	{
		this.archive = zip;
	}

	private void initArchive()
		throws IOException
	{
		baseOut = new FileOutputStream(archive);
		zout = new ZipOutputStream(baseOut);
		zout.setLevel(9);
	}

	public boolean isArchive() { return true; }
	
	public OutputStream createOutputStream(File output) 
		throws IOException
	{
		String filename = output.getName();
		return createOutputStream(filename);
	}
	
	public OutputStream createOutputStream(String filename) 
		throws IOException
	{
		if (this.zout == null) initArchive();
		
		ZipEntry currentEntry = new ZipEntry(filename);
		this.zout.putNextEntry(currentEntry);
		return new ZipEntryOutputStream(zout);
	}

	public Writer createWriter(String output, String encoding)
		throws IOException
	{
		OutputStream out = createOutputStream(output);
		return EncodingUtil.createWriter(out, encoding);
	}
	
	public Writer createWriter(File output, String encoding)
		throws IOException
	{
		OutputStream out = createOutputStream(output);
		return EncodingUtil.createWriter(out, encoding);
	}
	
	public void done() throws IOException
	{
		if (this.zout != null) 
		{
			zout.close();
		}
		if (baseOut != null)
		{
			baseOut.close();
		}
	}
	
}
