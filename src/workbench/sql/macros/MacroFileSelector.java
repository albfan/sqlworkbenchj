/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.sql.macros;

import java.awt.Window;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import workbench.WbManager;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.WbFileChooser;

import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class MacroFileSelector
{
	public static final String LAST_DIR_PROPERTY = "workbench.macros.lastdir";

	public boolean canLoadMacros(int clientId)
	{
		if (!MacroManager.getInstance().getMacros(clientId).isModified()) return true;

		int result = WbSwingUtilities.getYesNoCancel(WbManager.getInstance().getCurrentWindow(), ResourceMgr.getString("MsgConfirmUnsavedMacros"));
		if (result == JOptionPane.CANCEL_OPTION)
		{
			return false;
		}
		if (result == JOptionPane.YES_OPTION)
		{
			MacroManager.getInstance().save();
		}
		return true;
	}

	public WbFile selectMacroFile()
	{
		return selectStorageFile(false, null);
	}

	public WbFile selectStorageForLoad(int clientId)
	{
		if (!canLoadMacros(clientId)) return null;
		return selectStorageFile(false, null);
	}

	public WbFile selectStorageForSave(int clientId)
	{
		return selectStorageFile(true, MacroManager.getInstance().getMacros(clientId).getCurrentFile());
	}

	private WbFile selectStorageFile(boolean forSave, File currentFile)
	{
		String lastDir = Settings.getInstance().getProperty(LAST_DIR_PROPERTY, Settings.getInstance().getConfigDir().getAbsolutePath());

		JFileChooser fc = new WbFileChooser(lastDir);
		fc.addChoosableFileFilter(ExtensionFileFilter.getXmlFileFilter());
		fc.setFileFilter(ExtensionFileFilter.getXmlFileFilter());

		Window parent = SwingUtilities.getWindowAncestor(WbManager.getInstance().getCurrentWindow());

		int answer = JFileChooser.CANCEL_OPTION;

		if (forSave)
		{
			if (currentFile != null)
			{
				fc.setSelectedFile(currentFile);
			}
			answer = fc.showSaveDialog(parent);
		}
		else
		{
			answer = fc.showOpenDialog(parent);
		}

		File selectedFile = null;

		if (answer == JFileChooser.APPROVE_OPTION)
		{
			selectedFile = fc.getSelectedFile();
			if (forSave)
			{
				WbFile wb = new WbFile(selectedFile);
				String ext = wb.getExtension();
				if (!ext.equalsIgnoreCase("xml"))
				{
					String fullname = wb.getFullPath();
					fullname += ".xml";
					selectedFile = new File(fullname);
				}
			}
			lastDir = fc.getCurrentDirectory().getAbsolutePath();
			Settings.getInstance().setProperty(LAST_DIR_PROPERTY, lastDir);
		}
		if (selectedFile == null)
		{
			return null;
		}
		return new WbFile(selectedFile);
	}


}
