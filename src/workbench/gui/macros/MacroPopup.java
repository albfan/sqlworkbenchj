/*
 * MacroPopup.java
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
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import workbench.interfaces.FileActions;
import workbench.interfaces.MacroChangeListener;
import workbench.interfaces.MainPanel;
import workbench.interfaces.PropertyStorage;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.RunMacroAction;
import workbench.gui.actions.SaveListFileAction;
import workbench.gui.actions.WbAction;
import workbench.gui.editor.MacroExpander;
import workbench.gui.sql.SqlPanel;

import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroManager;

import workbench.util.StringUtil;

import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;

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
	private final String propkey = getClass().getName() + ".expandedgroups";
	private final String toolkey = "macropopup";

	public MacroPopup(MainWindow parent)
	{
		super(parent, false);
		mainWindow = parent;
		setLayout(new BorderLayout(0, 0));
		setTitle(ResourceMgr.getString("TxtMacroManagerWindowTitle"));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

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

		List<String> groups = getExpanedGroups();
		tree.expandGroups(groups);
		tree.addMouseListener(this);

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
		ToolTipManager.sharedInstance().registerComponent(tree);
	}

	private List<String> getExpanedGroups()
	{
		PropertyStorage config = getConfig();
		String groups = config.getProperty(propkey, null);
		return StringUtil.stringToList(groups, ",", true, true);
	}

	private void saveExpandedGroups(List<String> groups)
	{
		String grouplist = StringUtil.listToString(groups, ',', true);
		PropertyStorage config = getConfig();
		config.setProperty(propkey, grouplist);
	}

	private PropertyStorage getConfig()
	{
		if (GuiSettings.getStoreMacroPopupInWorkspace() && mainWindow != null)
		{
			return mainWindow.getToolProperties(toolkey);
		}
		else
		{
			return Settings.getInstance();
		}
	}

	private void closeWindow()
	{
		ToolTipManager.sharedInstance().unregisterComponent(tree);
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
		saveExpandedGroups(groups);
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
						}
					}
					else
					{
						MacroRunner runner = new MacroRunner();
						runner.runMacro(macro, panel, WbAction.isShiftPressed(e.getModifiers()));
					}
					WbSwingUtilities.requestFocus(mainWindow, panel);
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
