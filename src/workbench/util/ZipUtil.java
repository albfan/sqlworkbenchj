/*
 * ZipUtil.java
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

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipFile;

/**
 * @author support@sql-workbench.net
 */
public class ZipUtil
{
	

	public static boolean isZipFile(File f)
	{
		// The JVM crashes (sometimes) if I pass my "fake" ClipboardFile object
		// to the ZipFile constructor, so this is checked beforehand
		if (f instanceof ClipboardFile) return false;

		ZipFile archive = null;
		try
		{
			archive = new ZipFile(f);
		}
		catch (Throwable e)
		{
			return false;
		}
		finally
		{
			try { archive.close(); } catch (Throwable th) {}
		}
		return true;
	}
	
}
