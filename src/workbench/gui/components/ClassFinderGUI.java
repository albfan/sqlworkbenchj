/*
 * ClassFinderGUI.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Window;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.ClassFinder;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class ClassFinderGUI
{
	private ClassFinder finder;
	private JTextComponent className;
	private List<String> classPath;
	private JLabel statusBar;
	private String statusKey;
	private String selectWindowKey;

	public ClassFinderGUI(ClassFinder clsFinder, JTextComponent target, JLabel status)
	{
		finder = clsFinder;
		className = target;
		statusBar = status;
	}

	public void setClassPath(List<String> libraries)
	{
		classPath= libraries;
	}

	public void setStatusBarKey(String key)
	{
		statusKey = key;
	}

	public void setWindowTitleKey(String key)
	{
		selectWindowKey = key;
	}

	protected String selectEntry(List<String> entries)
	{
		JPanel p = new JPanel(new BorderLayout());
		DefaultListModel model = new DefaultListModel();
		for (String s : entries)
		{
			model.addElement(s);
		}
		JList list = new JList(model);
		list.setVisibleRowCount(4);
		JScrollPane scroll = new JScrollPane(list);
		p.add(scroll, BorderLayout.CENTER);
		Window parent = SwingUtilities.getWindowAncestor(className);
		boolean ok = WbSwingUtilities.getOKCancel(ResourceMgr.getString(selectWindowKey), parent, p);
		String cls = null;
		if (ok)
		{
			cls = (String) list.getSelectedValue();
		}
		return cls;
	}

	protected void checkFinished(final List<String> drivers)
	{
		if (drivers == null) return;
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				statusBar.setText("");
				if (drivers.size() == 1)
				{
					className.setText(drivers.get(0));
				}
				else if (drivers.size() > 0)
				{
					String cls = selectEntry(drivers);
					if (cls != null)
					{
						className.setText(cls);
					}
				}
			}
		});
	}

	public void startCheck()
	{
		Thread t = new WbThread("CheckDriver")
		{
			@Override
			public void run()
			{
				statusBar.setText(ResourceMgr.getString(statusKey));
				try
				{
					List<String> drivers = finder.findImplementations(classPath);
					checkFinished(drivers);
				}
				catch (Exception e)
				{
					LogMgr.logError("DriverEditorPanel.propertyChange()", "Could not find JDBC driver class", e);
				}
			}
		};
		t.start();
	}

}
