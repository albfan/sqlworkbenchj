/*
 * WbStarter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;

import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * This is a wrapper to kick-off the actual WbManager class. It should run
 * with any JDK >= 1.3 as it does not reference any other classes.
 * <br/>
 * This class is compiled separately in build.xml to allow for a different
 * class file version between this class and the rest of the application.
 * Thus a check for the correct JDK version can be done inside the Java code.
 *
 * @author Thomas Kellerer
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
			String error = "SQL Workbench/J requires Java 6, but only " + version + " was found\n\n" +
				"If you do have Java 6 installed, please point JAVA_HOME to the location of your Java 6 installation.\n" +
				"When using Windows you can also use the -jdk switch.\n" +
				"Please refer to the manual for details on how to specify the Java runtime to be used.";

			System.err.println("*** Cannot run this application ***");
			System.err.println(error);
			try
			{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

				// The dummy Frame is needed for pre Java 5 because otherwise
				// the dialog will not appear in the Windows task bar
				Frame dummy = new Frame("SQL Workbench/J - Wrong Java version");
				dummy.setBounds(-2000, -2000, 0, 0);
				dummy.setVisible(true);

				try
				{
					URL iconUrl = WbStarter.class.getClassLoader().getResource("workbench/resource/images/workbench16.gif");
					ImageIcon icon = new ImageIcon(iconUrl);
					dummy.setIconImage(icon.getImage());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				final JDialog d = new JDialog(dummy, "SQL Workbench/J - Wrong Java version", true);
				d.getContentPane().setLayout(new BorderLayout(5, 5));
				JButton b = new JButton("Close");
				b.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						d.setVisible(false);
						d.dispose();
					}
				});
				JOptionPane pane = new JOptionPane(error, JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION, (Icon)null, new Object[] { b } );
				d.getContentPane().add(pane, BorderLayout.CENTER);
				d.pack();
				d.setLocationRelativeTo(null);
				d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				d.setVisible(true);
			}
			catch (Throwable e)
			{
				e.printStackTrace();
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
