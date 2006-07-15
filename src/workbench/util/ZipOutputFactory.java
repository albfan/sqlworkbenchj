/*
 * ZipOutputFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
 * @author support@sql-workbench.net
 */
public class ZipOutputFactory
	implements OutputFactory
{
	private File archive;
	private ZipOutputStream zout;
	private ZipEntry currentEntry;
	private String comment;
	
	public ZipOutputFactory(File zip)
	{
		this.archive = zip;
	}

	private void initArchive()
		throws IOException
	{
		OutputStream out = new FileOutputStream(archive);
		this.zout = new ZipOutputStream(out);
		this.zout.setLevel(9);
	}
	
	public OutputStream createOutputStream(File output) throws IOException
	{
		if (this.zout == null) initArchive();
		String filename = output.getName();
		this.currentEntry = new ZipEntry(filename);
		this.zout.putNextEntry(currentEntry);
		return new ZipEntryOutputStream(this.currentEntry, this.zout);
	}

	public Writer createWriter(File output, String encoding)
		throws IOException
	{
		OutputStream out = createOutputStream(output);
		return EncodingUtil.createWriter(out, encoding);
	}
	
	public void done() throws IOException
	{
//		if (currentEntry != null) 
//		{
//			zout.closeEntry();
//		}
		if (this.zout != null) zout.close();
	}
	
}

class ZipEntryOutputStream
	extends OutputStream
{
	private ZipOutputStream archive;
	
	ZipEntryOutputStream(ZipEntry e, ZipOutputStream zip)
	{
		archive = zip;
	}

	public void close() throws IOException
	{
		archive.closeEntry();
	}
	
	public void flush() throws IOException
	{
		archive.flush();
	}
	
	public void write(byte[] b, int off, int len) throws IOException
	{
		archive.write(b, off, len);
	}

	public void write(byte[] b) throws IOException
	{
		archive.write(b);
	}

	public void write(int b) throws IOException
	{
		archive.write(b);
	}
	
}