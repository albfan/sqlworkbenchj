/*
 * TableRowCountPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import workbench.WbManager;
import workbench.interfaces.ToolWindow;
import workbench.interfaces.ToolWindowManager;
import workbench.resource.ResourceMgr;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.TableRowHeader;
import workbench.gui.dbobjects.TableDataPanel;

import workbench.util.StringUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class DetachedResultWindow
	extends JPanel
	implements WindowListener, ToolWindow
{
	private final TableDataPanel data;
	private JFrame window;

	public DetachedResultWindow(DwPanel result, ToolWindowManager registry)
	{
		super(new BorderLayout(0,0));

		this.data = new TableDataPanel();

		this.add(data, BorderLayout.CENTER);
		data.displayData(result.getDataStore(), result.getLastExecutionTime());

		String title = result.getDataStore().getResultName();
		if (StringUtil.isBlank(title))
		{
			title = ResourceMgr.getString("LblTabResult");
		}

		this.window = new JFrame(title);
		this.window.getContentPane().setLayout(new BorderLayout());
		this.window.getContentPane().add(this, BorderLayout.CENTER);

		ResourceMgr.setWindowIcons(window, "data");

		int rWidth = result.getWidth();
		int tWidth = Math.max(result.getTable().getWidth(), data.getToolbarWidth());
		int addWidth = result.getVerticalScrollBarWidth();
		if (addWidth == 0)
		{
			addWidth = UIManager.getInt("ScrollBar.width") + 4;
		}

		TableRowHeader rowHeader = TableRowHeader.getRowHeader(data.getData());
		if (rowHeader != null)
		{
			addWidth += rowHeader.getWidth();
		}

		int width = Math.min(rWidth, tWidth) + addWidth + 16;
		int height = result.getHeight() + data.getAddHeight();

		this.window.setSize(width + 8, height + 8);

		this.window.addWindowListener(this);
		registry.registerToolWindow(this);
	}

	public void showWindow()
	{
		if (this.window != null)
		{
			WbSwingUtilities.center(this.window, WbManager.getInstance().getCurrentWindow());
			this.window.setVisible(true);
		}
	}
	private void doClose()
	{
		this.window.setVisible(false);
		this.window.dispose();
		this.window = null;
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		WbManager.getInstance().unregisterToolWindow(this);
		doClose();
	}


	@Override
	public void windowClosed(WindowEvent e)
	{
		if (data != null)
		{
			data.dispose();
		}
	}

	@Override
	public void windowIconified(WindowEvent e)
	{

	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{

	}

	@Override
	public void windowActivated(WindowEvent e)
	{

	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

	@Override
	public void closeWindow()
	{
		doClose();
	}

	@Override
	public void activate()
	{
		if (window != null)
		{
			window.requestFocus();
		}
	}

	@Override
	public JFrame getWindow()
	{
		return window;
	}

	@Override
	public void disconnect()
	{
		if (this.data != null)
		{
			this.data.detachConnection();
		}
	}

	@Override
	public WbConnection getConnection()
	{
		return null;
	}

}
