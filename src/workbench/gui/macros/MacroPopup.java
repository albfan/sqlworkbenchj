/*
 * MacroPopup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
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
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.RunMacroAction;
import workbench.gui.actions.SaveListFileAction;
import workbench.gui.actions.WbAction;
import workbench.gui.editor.MacroExpander;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.FileActions;
import workbench.interfaces.MacroChangeListener;
import workbench.interfaces.MainPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroManager;
import workbench.util.StringUtil;

/**
 * Display a floating window with the MacroTree.
 * When double clicking a macro in the tree, the macro is executed in the
 * passed MainWindow
 *
 * @author Thomas Kellerer
 */
public class MacroPopup
	extends JDialog
	implements WindowListener, MouseListener, TreeSelectionListener, MacroChangeListener
{
	private MacroTree tree;
	private MainWindow mainWindow;
	private RunMacroAction runAction;
	private boolean isClosing;

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
			setLocation(parent.getX() + parent.getWidth() - getWidth()/2, parent.getY() + 25);
		}
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		String s = Settings.getInstance().getProperty(getClass().getName() + ".expandedgroups", null);
		List<String> groups = StringUtil.stringToList(s, ",", true, true);
		tree.expandGroups(groups);
		tree.addMouseListener(this);
		mainWindow = parent;
		runAction = new RunMacroAction(mainWindow, null, -1);
		tree.addPopupActionAtTop(runAction);
		tree.addTreeSelectionListener(this);
		addWindowListener(this);


		FileActions actions = new FileActions()
		{
			@Override
			public void saveItem()
				throws Exception
			{
				saveMacros(false);
			}

			@Override
			public void deleteItem()
				throws Exception
			{
				tree.deleteSelection();
			}

			@Override
			public void newItem(boolean copyCurrent)
				throws Exception
			{
			}
		};

		DeleteListEntryAction delete = new DeleteListEntryAction(actions);
		tree.addPopupAction(delete, true);
		SaveListFileAction save = new SaveListFileAction(actions, "LblSaveMacros");
		tree.addPopupAction(save, false);

		MacroManager.getInstance().getMacros().addChangeListener(this);
	}

	private void closeWindow()
	{
		setVisible(false);
		MacroManager.getInstance().getMacros().removeChangeListener(this);
		dispose();
	}

	public boolean isClosing()
	{
		return isClosing;
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	protected void saveMacros(boolean restoreListener)
	{
		MacroManager.getInstance().getMacros().removeChangeListener(this);
		if (tree.isModified())
		{
			tree.saveChanges();
		}
		if (restoreListener)
		{
			MacroManager.getInstance().getMacros().addChangeListener(this);
		}
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		isClosing = true;
		if (tree.isModified())
		{
			int result = WbSwingUtilities.getYesNoCancel(this, ResourceMgr.getString("MsgConfirmUnsavedMacros"));
			if (result == JOptionPane.CANCEL_OPTION)
			{
				isClosing = false;
				return;
			}
			if (result == JOptionPane.YES_OPTION)
			{
				saveMacros(false);
			}
		}
		removeWindowListener(this);
		tree.removeTreeSelectionListener(this);

		List<String> groups = tree.getExpandedGroupNames();
		Settings.getInstance().setProperty(getClass().getName() + ".expandedgroups", StringUtil.listToString(groups, ',', true));
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				closeWindow();
			}
		});
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
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
					if (macro.getExpandWhileTyping())
					{
						MacroExpander expander = panel.getEditor().getMacroExpander();
						if (expander != null)
						{
							expander.insertMacroText(macro.getText());
							panel.selectEditorLater();
						}
					}
					else
					{
						MacroRunner runner = new MacroRunner();
						runner.runMacro(macro, panel, WbAction.isShiftPressed(e.getModifiers()));
					}
				}
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	@Override
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

	@Override
	public void macroListChanged()
	{
		List<String> groups = tree.getExpandedGroupNames();
		tree.loadMacros();
		tree.expandGroups(groups);
	}

}
