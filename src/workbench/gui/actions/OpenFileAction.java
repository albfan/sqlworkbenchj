/*
 * OpenFileAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import workbench.interfaces.EncodingSelector;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.WbFileChooser;
import workbench.gui.sql.SqlPanel;

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

	@Override
	public void executeAction(ActionEvent e)
	{
		try
		{
			File lastDir = new File(Settings.getInstance().getLastSqlDir());
			if (GuiSettings.getFollowFileDirectory())
			{
				SqlPanel currentPanel = window.getCurrentSqlPanel();
				if  (currentPanel != null && currentPanel.hasFileLoaded())
				{
					WbFile f = new WbFile(currentPanel.getCurrentFileName());
					if (f.getParent() != null)
					{
						lastDir = f.getParentFile();
					}
				}
				if (lastDir == null)
				{
					lastDir = GuiSettings.getDefaultFileDir();
				}
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

			boolean rememberNewTabSetting = true;
			if (window.getCurrentSqlPanel() == null)
			{
				// DbExplorer is open, force open in new tab!
				newTab.setSelected(true);
				newTab.setEnabled(false);
				rememberNewTabSetting = false;
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
				final boolean openInNewTab = newTab.isSelected();

				if (!GuiSettings.getFollowFileDirectory())
				{
					lastDir = fc.getCurrentDirectory();
					Settings.getInstance().setLastSqlDir(lastDir.getAbsolutePath());
				}

				Settings.getInstance().setDefaultFileEncoding(encoding);
				if (rememberNewTabSetting)
				{
					Settings.getInstance().setProperty("workbench.file.newtab", openInNewTab);
				}

				WbFile f = new WbFile(fc.getSelectedFile());
				final String fname = f.getFullPath();
				EventQueue.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						SqlPanel sql;
						if (openInNewTab)
						{
							sql = (SqlPanel) window.addTab();
							window.invalidate();
						}
						else
						{
							sql = window.getCurrentSqlPanel();
						}
						if (sql != null)
						{
							sql.readFile(fname, encoding);
							sql.invalidate();
						}
						window.currentTabChanged();
					}
				});
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("EditorPanel.openFile()", "Error selecting file", th);
			WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(th));
		}
	}
}
