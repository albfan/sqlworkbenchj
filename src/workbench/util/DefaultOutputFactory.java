/*
 * DefaultOutputFactory.java
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

/**
 *
 * @author Thomas Kellerer
 */
public class DefaultOutputFactory
	implements OutputFactory
{

	@Override
	public boolean isArchive()
	{
		return false;
	}

	@Override
	public OutputStream createOutputStream(File output)
		throws IOException
	{
		return new FileOutputStream(output);
	}

	@Override
	public Writer createWriter(File output, String encoding)
		throws IOException
	{
		OutputStream out = createOutputStream(output);
		return EncodingUtil.createWriter(out, encoding);
	}

	@Override
	public Writer createWriter(String filename, String encoding)
		throws IOException
	{
		return createWriter(new File(filename), encoding);
	}

	@Override
	public void done()
		throws IOException
	{
	}
}
