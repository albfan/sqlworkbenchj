/*
 * OpenFileAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import javax.swing.border.EmptyBorder;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.WbFileChooser;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.EncodingSelector;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.WbFile;

/**
 * Open a new file in the main window, with the option to open the
 * file in a new tab
 * @author  Thomas Kellerer
 */
public class OpenFileAction
	extends WbAction
{
	private MainWindow window;

	public OpenFileAction(MainWindow mainWindow)
	{
		super();
		window = mainWindow;
		this.initMenuDefinition("MnuTxtFileOpen", KeyStroke.getKeyStroke(KeyEvent.VK_O, PlatformShortcuts.getDefaultModifier()));
		this.setIcon("Open");
		this.setMenuItemName(ResourceMgr.MNU_TXT_FILE);
		setCreateMenuSeparator(true);
	}

	public void executeAction(ActionEvent e)
	{
		try
		{
			String lastDir = null;
			
			if (GuiSettings.getFollowFileDirectory())
			{
				SqlPanel currentPanel = window.getCurrentSqlPanel();
				if  (currentPanel != null && currentPanel.hasFileLoaded())
				{
					WbFile f = new WbFile(currentPanel.getCurrentFileName());
					if (f.getParent() != null)
					{
						lastDir = f.getParent();
					}
				}
				if (lastDir == null)
				{
					lastDir = GuiSettings.getDefaultFileDir();
				}
			}
			else
			{
				lastDir = Settings.getInstance().getLastSqlDir();
			}
			
			JFileChooser fc = new WbFileChooser(lastDir);
			JPanel acc = new JPanel(new GridBagLayout());
			JComponent p = EncodingUtil.createEncodingPanel();
			p.setBorder(new EmptyBorder(0, 5, 0, 0));
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.anchor = GridBagConstraints.NORTHEAST;
			c.fill = GridBagConstraints.HORIZONTAL;
			acc.add(p, c);

			JCheckBox newTab = new JCheckBox(ResourceMgr.getString("LblOpenNewTab"));
			newTab.setToolTipText(ResourceMgr.getDescription("LblOpenNewTab"));

			boolean forceNewTab = false;
			if (window.getCurrentSqlPanel() == null)
			{
				// DbExplorer is open, force open in new tab!
				newTab.setSelected(true);
				newTab.setEnabled(false);
				forceNewTab = true;
			}
			else
			{
				newTab.setSelected(Settings.getInstance().getBoolProperty("workbench.file.newtab", false));
			}

			c.gridy++;
			c.insets = new Insets(5, 0, 0, 0);
			c.weighty = 1.0;
			acc.add(newTab, c);

			EncodingSelector selector = (EncodingSelector) p;
			selector.setEncoding(Settings.getInstance().getDefaultFileEncoding());

			fc.setAccessory(acc);

			fc.addChoosableFileFilter(ExtensionFileFilter.getSqlFileFilter());
			int answer = fc.showOpenDialog(window);
			if (answer == JFileChooser.APPROVE_OPTION)
			{
				final String encoding = selector.getEncoding();
				boolean openInNewTab = newTab.isSelected();

				final SqlPanel sql;
				if (openInNewTab)
				{
					sql = (SqlPanel) window.addTab();
				}
				else
				{
					sql = window.getCurrentSqlPanel();
				}

				if (!GuiSettings.getFollowFileDirectory())
				{
					lastDir = fc.getCurrentDirectory().getAbsolutePath();
					Settings.getInstance().setLastSqlDir(lastDir);
				}

				Settings.getInstance().setDefaultFileEncoding(encoding);
				if (!forceNewTab)
				{
					Settings.getInstance().setProperty("workbench.file.newtab", openInNewTab);
				}

				if (sql != null)
				{
					WbFile f = new WbFile(fc.getSelectedFile());
					final String fname = f.getFullPath();
					EventQueue.invokeLater(new Runnable()
					{

						@Override
						public void run()
						{
							sql.readFile(fname, encoding);
						}
					});
				}
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("EditorPanel.openFile()", "Error selecting file", th);
			WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(th));
		}
	}
}
