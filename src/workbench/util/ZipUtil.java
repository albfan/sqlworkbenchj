/*
 * ZipUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
		ZipFile archive = null;
		try
		{
			archive = new ZipFile(f);
		}
		catch (Exception e)
		{
			return false;
		}
		finally
		{
			try { archive.close(); } catch (Throwable th) {}
		}
		return true;
	}
	
	public static void main(String[] args)
	{
		try
		{
			System.out.println(isZipFile(new File("d:/temp/test.sql")));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
