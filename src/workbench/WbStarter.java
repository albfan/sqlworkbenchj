/*
 * WbStarter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.lang.reflect.Method;

import javax.swing.JOptionPane;

/**
 * This is a wrapper to kick-off the actual WbManager class. It should run 
 * with any JDK >= 1.3 as it does not reference any other classes.
 * This class is compiled separately in build.xml to allow for a different 
 * class file version between this class and the rest of the application.
 * Thus a check for the correct JDK version can be done inside the Java code.
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
		final int minMinorVersion = 6;
		
		int minorversion = -1;
		
		try
		{
			int majorversion = Integer.parseInt(version.substring(0,1));
			minorversion = Integer.parseInt(version.substring(2,3));
			versionIsOk = (majorversion >= 1) && (minorversion >= minMinorVersion);
		}
		catch (Exception e)
		{
			versionIsOk = false;
		}

		if (!versionIsOk)
		{
			String error = "Sorry, SQL Workbench/J requires Java 6, but " + version + " was found\n" + 
				"Please upgrade to a recent Java version\n\n" +
				"If you have Java 6 installed, please use the -jdk switch of the Windows launcher\n" +
				"to specify the correct base directory or point the shell scripts to the correct binaries";
			
			System.err.println("*** Cannot run this application ***");
			System.err.println(error);
			try
			{
				JOptionPane.showMessageDialog(null, error);
			}
			catch (Throwable e)
			{
				// Ignore
			}
			System.exit(1);
		}

		try
		{
			// Do not reference WbManager directly, otherwise a compile
			// of this class will trigger a compile of the other classes, but they
			// should be compiled with a different class file version (see build.xml)
			Class mgr = Class.forName("workbench.WbManager");
			Method main = mgr.getDeclaredMethod("main", new Class[] { String[].class });
			main.invoke(null, new Object[] { args });
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}

}
