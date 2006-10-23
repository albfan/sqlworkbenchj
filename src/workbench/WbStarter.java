/*
 * WbStarter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.awt.Toolkit;

import javax.swing.JOptionPane;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbStarter
{

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		String version = System.getProperty("java.version", null);
		if (version == null)
		{
			version = System.getProperty("java.runtime.version");
		}
		
		boolean versionIsOk = false;
		int minMinorVersion = 4;
		
		try
		{
			int majorversion = Integer.parseInt(version.substring(0,1));
			int minorversion = Integer.parseInt(version.substring(2,3));
			versionIsOk = (majorversion >= 1) && (minorversion >= minMinorVersion);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			versionIsOk = false;
		}

		if (!versionIsOk)
		{
			String error = "A JVM version 1." + minMinorVersion + " or higher is needed to run SQL Workbench/J (Found: " + version + ")";
			try
			{
				JOptionPane.showMessageDialog(null, error);
				Toolkit.getDefaultToolkit().beep();
				Toolkit.getDefaultToolkit().beep();
				Toolkit.getDefaultToolkit().beep();
			}
			catch (Throwable e)
			{
				// ignore ...
			}
			System.err.println(error);
			System.exit(1);
		}

		try
		{
			WbManager.main(args);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.err);
			System.exit(2);
		}
	}

}
