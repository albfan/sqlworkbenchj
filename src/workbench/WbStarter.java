/*
 * WbStarter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
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
 * with any JDK >= 1.3 as it does no reference any other classes. 
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
		// This property should be set as early as possible to 
		// ensure that it is defined before any AWT class is loaded
		// this will make the application menu appear at the correct
		// location when running on with Aqua look and feel on a Mac
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
		System.setProperty("apple.awt.showGrowBox", "true");
		System.setProperty("apple.awt.rendering", "speed");
		System.setProperty("swing.boldMetal", "false");
		
		String version = System.getProperty("java.version", null);
		if (version == null)
		{
			version = System.getProperty("java.runtime.version");
		}
		
		boolean versionIsOk = false;
		final int minMinorVersion = 5;
		
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
			String error = "A JVM version 1." + minMinorVersion + " or higher is needed to run SQL Workbench/J (Found: " + version + ")";
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
