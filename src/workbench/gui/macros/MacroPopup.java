/*
 * MacroPopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import workbench.gui.MainWindow;
import workbench.gui.actions.RunMacroAction;
import workbench.gui.actions.WbAction;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.MainPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.macros.MacroDefinition;
import workbench.util.StringUtil;

/**
 * Display a floating window with the MacroTree.
 * When double clicking a macro in the tree, the macro is executed in the
 * passed MainWindow
 *
 * @author support@sql-workbench.net
 */
public class MacroPopup
	extends JDialog
	implements WindowListener, MouseListener, TreeSelectionListener
{
	private MacroTree tree;
	private MainWindow mainWindow;
	private RunMacroAction runAction;

	public MacroPopup(MainWindow parent)
	{
		super(parent, false);
		setLayout(new BorderLayout(0, 0));
		setTitle(ResourceMgr.getString("TxtMacroManagerWindowTitle"));
		tree = new MacroTree();
		JScrollPane p = new JScrollPane(tree);
		add(p, BorderLayout.CENTER);
		if (!Settings.getInstance().restoreWindowSize(this))
		{
			setSize(200,400);
		}

		if (!Settings.getInstance().restoreWindowPosition(this))
		{
			setLocation((int)(parent.getX() + parent.getWidth() - getWidth()/2), parent.getY() + 25);
		}
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		String s = Settings.getInstance().getProperty(getClass().getName() + ".expandedgroups", null);
		List<String> groups = StringUtil.stringToList(s, ",", true, true);
		tree.expandGroups(groups);
		tree.addMouseListener(this);
		mainWindow = parent;
		runAction = new RunMacroAction(mainWindow, null, -1);
		tree.addActionToPopup(runAction);
		tree.addTreeSelectionListener(this);
		addWindowListener(this);
	}

	private void closeWindow()
	{
		setVisible(false);
		dispose();
	}

	public void windowOpened(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		removeWindowListener(this);
		tree.removeTreeSelectionListener(this);
		if (tree.isModified())
		{
			tree.saveChanges();
		}
		List<String> groups = tree.getExpandedGroupNames();
		Settings.getInstance().setProperty(getClass().getName() + ".expandedgroups", StringUtil.listToString(groups, ',', true));
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				closeWindow();
			}
		});
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2)
		{
			MacroDefinition macro = tree.getSelectedMacro();
			if (mainWindow != null && macro != null)
			{
				SqlPanel panel = mainWindow.getCurrentSqlPanel();
				if (panel != null)
				{
					MacroRunner runner = new MacroRunner();
					runner.runMacro(macro, panel, WbAction.isShiftPressed(e.getModifiers()));
				}
			}
		}
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void valueChanged(TreeSelectionEvent e)
	{
		MacroDefinition macro = tree.getSelectedMacro();
		if (mainWindow != null && macro != null)
		{
			MainPanel panel = mainWindow.getCurrentPanel();
			if (panel instanceof MacroClient)
			{
				runAction.setEnabled(true);
				runAction.setMacro(macro);
			}
		}
		else
		{
			runAction.setEnabled(false);
		}
	}

}
