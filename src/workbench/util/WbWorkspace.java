/*
 * WbWorkspace.java
 *
 * Created on March 29, 2003, 11:53 AM
 */

package workbench.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import workbench.util.StringUtil;

/**
 *
 * @author  thomas
 */
public class WbWorkspace
{
	private String dir;
	private String basename;
	private ZipOutputStream zout;
	private ZipFile archive;
	private ArrayList entries;
	
	private boolean isReadOnly;
	
	public WbWorkspace(String archiveName, boolean createNew)
		throws IOException
	{
		if (createNew)
		{
			this.isReadOnly = false;
			File f = new File(archiveName);
			OutputStream out = new FileOutputStream(f);
			this.zout = new ZipOutputStream(out);
			this.zout.setLevel(9);
			this.zout.setComment("SQL Workbench/J Workspace file");
		}
		else
		{
			this.isReadOnly = true;
			this.zout = null;
			this.archive = new ZipFile(archiveName);
			Enumeration e = this.archive.entries();
			this.entries = new ArrayList(10);
			while (e.hasMoreElements())
			{
				ZipEntry entry = (ZipEntry)e.nextElement();
				this.entries.add(entry);
			}
		}
	}

	public void addHistoryEntry(String aFilename, ArrayList data)
		throws IOException
	{
		if (this.isReadOnly) throw new IllegalStateException("Workspace is opened for reading. addHistoryEntry() may not be called");
		
		File f = new File(aFilename);
		String filename = f.getName();
		ZipEntry entry = new ZipEntry(filename);
		this.zout.putNextEntry(entry);
		StringUtil.writeStringList(data, this.zout);
		zout.closeEntry();
	}

	public int getEntryCount()
	{
		if (!this.isReadOnly) throw new IllegalStateException("Workspace is opened for writing. Entry count is not available");
		if (this.entries == null) return 0;
		return this.entries.size();
	}
	
	public ArrayList getHistoryData(int anIndex)
		throws IOException
	{
		if (!this.isReadOnly) throw new IllegalStateException("Workspace is opened for writing. Entry count is not available");
		if (anIndex > this.entries.size()) throw new IndexOutOfBoundsException("Index " + anIndex + " is great then " + (this.entries.size() - 1));
		ZipEntry e = (ZipEntry)this.entries.get(anIndex);
		InputStream in = this.archive.getInputStream(e);
		ArrayList data = StringUtil.readStringList(in);
		
		return data;
	}
	
	public void close()
		throws IOException
	{
		if (this.zout != null)
		{
			this.zout.close();
		}
	}
	
	public static void main(String[] args)
	{
		try
		{
			ArrayList data = new ArrayList(10);
			for (int i=0; i < 10; i++)
				data.add("statement" + i);

			WbWorkspace w = new WbWorkspace("test.wksp", true);
			w.addHistoryEntry("file1.txt", data);
			w.addHistoryEntry("file2.txt", data);
			w.addHistoryEntry("file3.txt", data);
			w.close();
			System.out.println("done.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
