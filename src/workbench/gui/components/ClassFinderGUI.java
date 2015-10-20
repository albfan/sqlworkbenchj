/*
 * ClassFinderGUI.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.gui.WbSwingUtilities;

import workbench.util.ClassFinder;
import workbench.util.StringUtil;
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
		JList<String> list = new JList<>(model);
    list.setVisibleRowCount(Math.min(10, entries.size() + 1));
		if (StringUtil.isNonBlank(className.getText()))
		{
			list.setSelectedValue(className.getText(), true);
		}
		JScrollPane scroll = new JScrollPane(list);
		p.add(scroll, BorderLayout.CENTER);
		Window parent = SwingUtilities.getWindowAncestor(className);
		boolean ok = WbSwingUtilities.getOKCancel(ResourceMgr.getString(selectWindowKey), parent, p);
		String cls = null;
		if (ok)
		{
			cls = list.getSelectedValue();
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
