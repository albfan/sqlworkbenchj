/*
 * ImportFileHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import workbench.db.exporter.RowDataConverter;
import workbench.util.ClipboardFile;
import workbench.util.EncodingUtil;
import workbench.util.WbFile;
import workbench.util.ZipUtil;

/**
 * @author support@sql-workbench.net
 */
public class ImportFileHandler
{
	private File baseFile;
	private File baseDir;
	private String encoding;
	private boolean isZip;
	private ZipFile mainArchive;
	private ZipFile attachments;
	private BufferedReader mainReader;
	
	public ImportFileHandler()
	{
	}
	
	public void setMainFile(File mainFile, String enc)
		throws IOException
	{
		this.done();
		this.mainArchive = null;
		this.attachments = null;
		this.encoding = enc;
	
		this.baseFile = mainFile;
		this.baseDir = baseFile.getParentFile();
		if (this.baseDir == null) baseDir = new File(".");
		isZip = ZipUtil.isZipFile(baseFile);
		this.initAttachements();
	}
	
	boolean isZip() { return isZip; }

	public BufferedReader getMainFileReader()
		throws IOException
	{
		if (this.mainReader != null)
		{
			try { mainReader.close(); } catch (Throwable th) {}
		}
		Reader r = null;
		if (baseFile instanceof ClipboardFile)
		{
			ClipboardFile cb = (ClipboardFile)baseFile;
			r = new StringReader(cb.getContents());
		}
		else if (isZip)
		{
			mainArchive = new ZipFile(baseFile);
			Enumeration entries = mainArchive.entries();
			if (entries.hasMoreElements())
			{
				ZipEntry entry = (ZipEntry)entries.nextElement();
				InputStream in = mainArchive.getInputStream(entry);
				r = EncodingUtil.createReader(in, encoding);
			}
			else
			{
				throw new FileNotFoundException("Zipfile " + this.baseFile.getAbsolutePath() + " does not contain any entries!");
			}
		}
		else
		{
			r = EncodingUtil.createReader(baseFile, encoding);
		}
		mainReader = new BufferedReader(r, 32*1024);
		return mainReader;
	}

	private void initAttachements()
		throws IOException
	{
		if (baseFile instanceof ClipboardFile) return;
		
		WbFile f = new WbFile(baseFile);
		String basename = f.getFileName();
		String attFileName = basename + RowDataConverter.BLOB_ARCHIVE_SUFFIX + ".zip";
		File attFile = new File(baseDir, attFileName);
		if (attFile.exists())
		{
			this.attachments = new ZipFile(attFile);
		}
	}

	protected ZipEntry findEntry(File f)
		throws IOException
	{
		ZipEntry entry = this.attachments.getEntry(f.getName());
		if (entry != null) return entry;
		
		throw new FileNotFoundException("Attachment file " + f.getName() + " not found in archive " + this.attachments.getName());	
	}
	
	public InputStream getAttachedFileStream(File f)
		throws IOException
	{
		if (baseFile instanceof ClipboardFile) throw new IOException("Attachments not supported for Clipboard");
		
		if (this.isZip)
		{
			ZipEntry entry = findEntry(f);
			return attachments.getInputStream(entry);
		}
		else
		{
			if (f.isAbsolute())
			{
				return new FileInputStream(f);
			}
			else
			{
				File realFile = new File(this.baseDir, f.getName());
				return new FileInputStream(realFile);
			}
			
		}
	}

	public long getLength(File f)
		throws IOException
	{
		if (this.isZip)
		{
			ZipEntry entry = findEntry(f);
			return entry.getSize();
		}
		else
		{
			if (f.isAbsolute())
			{
				return f.length();
			}
			File realFile = new File(this.baseDir, f.getName());
			return realFile.length();
		}
	}

	public String getEncoding() { return this.encoding; }
	
	public void done()
	{

		try { if (mainReader != null) mainReader.close(); } catch (Throwable th) {}
		try { if (mainArchive != null) mainArchive.close(); } catch (Throwable th) {}
		try { if (attachments != null) attachments.close(); } catch (Throwable th) {}
		mainReader = null;
		mainArchive = null;
		attachments = null;
	}
}
