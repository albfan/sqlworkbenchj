/*
 * WbStarter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.awt.Toolkit;
import java.lang.reflect.Method;

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
		boolean is14 = false;
		//System.out.println("Workbench: Using Java version=" + version);
		try
		{
			int majorversion = Integer.parseInt(version.substring(0,1));
			int minorversion = Integer.parseInt(version.substring(2,3));
			is14 = (majorversion >= 1) && (minorversion >= 4);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			is14 = false;
		}

		if (!is14)
		{
			String error = "A JVM version 1.4 or higher is needed to run SQL Workbench/J (Found: " + version + ")";
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
			// Create an instance using reflection, so that this class can be
			// loaded even with JDK < 1.4 (because no reference to WbManager is
			// in the class)
			Class manager = Class.forName("workbench.WbManager");
			Class[] parms = {String[].class};
			Method main = manager.getDeclaredMethod("main", parms);
			Object[] para = new Object[] { args };
			main.invoke(null, para);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

}
