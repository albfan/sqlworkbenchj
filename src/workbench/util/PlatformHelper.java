/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import workbench.log.LogMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class PlatformHelper
{

	public static String getDefaultPDFReader()
	{
		String reader = null;
		if (isMacOS())
		{
			reader = "open /Applications/Preview.app";
		}
		else if (isWindows())
		{
			reader = getWindowsFileAssociation(".pdf");
		}

		return reader;
	}

	public static String getWindowsFileAssociation(String extension)
	{
		try
		{
			if (!extension.startsWith("."))
			{
				extension = "." + extension;
			}
			ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "assoc", extension);
			Process assoc = builder.start();
			if (assoc == null) return null;

			BufferedReader in = new BufferedReader(new InputStreamReader(assoc.getInputStream()));
			String doctype = in.readLine();
			in.close();
			if (doctype == null) return null;

			// the association is returned
			doctype = doctype.substring(doctype.indexOf('=') + 1);
			builder.command("cmd.exe", "/c", "ftype", doctype);
			Process ftype = builder.start();
			if (ftype == null) return null;

			BufferedReader in2 = new BufferedReader(new InputStreamReader(ftype.getInputStream()));
			String prg = in2.readLine();
			in.close();
			if (prg == null) return null;

			prg = prg.substring(prg.indexOf("=") + 1);
			WbStringTokenizer tok = new WbStringTokenizer(prg, " ", true, "\"", false);
			String fullpath = tok.nextToken();
			return fullpath;
		}
		catch (Exception e)
		{
			LogMgr.logError("PlatformHelper.getWindowsFileAssociation", "Error retrieving file association", e);
		}
		return null;
	}

	public static boolean isWindows()
	{
		return System.getProperty("os.name").indexOf("Windows") > -1;
	}

	public static boolean isMacOS()
	{
		return MacOSHelper.isMacOS();
	}
}
