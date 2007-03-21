/*
 * BadFileWriter.java
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

import java.io.Writer;
import workbench.db.TableIdentifier;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.EncodingUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A class to write rejected rows into a flat file.
 * 
 * @author support@sql-workbench.net
 */
public class BadfileWriter
{
	private WbFile badFile;
	private int badRows = 0;
	private String encoding = null;
	
	/**
	 * Create a new BadFileWriter. 
	 * 
	 * If fname indicates a directory, the resulting bad file will be name after 
	 * the name of the supplied table (@link TableIdentifier#getTableName()} with 
	 * the extendsion ".bad" and will be created in specified directory.
	 * 
	 * The file will be deleted upon creation of this BadFileWriter but will 
	 * not be created unless at leas one rejected row is written.
	 * 
	 * @param fname the name of the bad file to be created
	 * @param table the table for which the import is currently running
	 * @param enc the encoding for the output file
	 * 
	 * @see #recordRejected(String)
	 */
	public BadfileWriter(String fname, TableIdentifier table, String enc)
	{
		WbFile f = new WbFile(fname);
		if (f.isDirectory())
		{
			String tname = StringUtil.makeFilename(table.getTableName()) + ".bad";
			this.badFile = new WbFile(fname, tname);
		}
		else
		{
			this.badFile = f;
		}
		this.encoding = enc;
		this.badFile.delete();
	}

	public void recordRejected(String record)
	{
		Writer w = null;
		try
		{
			w = EncodingUtil.createWriter(badFile, encoding, true);
			w.write(record);
			w.write(StringUtil.LINE_TERMINATOR);
			badRows ++;
		}
		catch (Exception e)
		{
			LogMgr.logError("BadFileWriter.recordRejected()", "Could not write record", e);
		}
		finally
		{
			try { w.close(); } catch (Throwable th) {}
		}
	}
	
	public int getRows() { return badRows; }
	
	public CharSequence getMessage()
	{
		StringBuilder b = new StringBuilder(50);
		b.append(this.badRows);
		b.append(' ');
		b.append(ResourceMgr.getString("MsgCopyNumRowsRejected") + "\n");
		b.append(ResourceMgr.getString("MsgCopyBadFile"));
		b.append(' ');
		b.append(this.badFile.getFullPath());
		return b;
	}
}
