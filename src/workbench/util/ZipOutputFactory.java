/*
 * ZipOutputFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

	@Override
	public boolean isArchive()
	{
		return true;
	}

	@Override
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

	@Override
	public Writer createWriter(String output, String encoding)
		throws IOException
	{
		OutputStream out = createOutputStream(output);
		return EncodingUtil.createWriter(out, encoding);
	}

	@Override
	public Writer createWriter(File output, String encoding)
		throws IOException
	{
		OutputStream out = createOutputStream(output);
		return EncodingUtil.createWriter(out, encoding);
	}

	@Override
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
