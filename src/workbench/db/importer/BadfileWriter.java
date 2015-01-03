/*
 * BadfileWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A class to write rejected rows into a flat file.
 * 
 * @author Thomas Kellerer
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

	public synchronized void recordRejected(String record)
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
			FileUtil.closeQuietely(w);
		}
	}
	
	public synchronized int getRows()
	{
		return badRows;
	}
	
	public CharSequence getMessage()
	{
		StringBuilder b = new StringBuilder(50);
		b.append(this.badRows);
		b.append(' ');
		b.append(ResourceMgr.getString("MsgCopyNumRowsRejected") + "\n");
		b.append(ResourceMgr.getFormattedString("MsgCopyBadFile", this.badFile.getFullPath()));
		return b;
	}
}
