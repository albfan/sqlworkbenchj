/*
 * WbFileChooser.java
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

import java.awt.Component;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;

import workbench.interfaces.ValidatingComponent;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.interfaces.EncodingSelector;
import workbench.util.CollectionUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class WbFileChooser
		extends JFileChooser
		implements PropertyChangeListener
{
	private String windowSettingsId;
	private JDialog dialog;
  private EncodingSelector selector;
  private final Set<String> changeEvents = CollectionUtil.treeSet(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, JFileChooser.SELECTED_FILES_CHANGED_PROPERTY);

	public WbFileChooser()
	{
		super();
		init();
	}

	public WbFileChooser(File currentDirectoryPath)
	{
		super(currentDirectoryPath);
		init();
	}

	public WbFileChooser(String currentDirectoryPath)
	{
		super(currentDirectoryPath);
		init();
	}

	private void init()
	{
		addPropertyChangeListener(this);
		putClientProperty("FileChooser.useShellFolder", GuiSettings.getUseShellFolders());
	}

	public void setSettingsID(String id)
	{
		this.windowSettingsId = id;
	}

	public JDialog getCurrentDialog()
	{
		return dialog;
	}

  public void setEncodingSelector(EncodingSelector component)
  {
    selector = component;
  }

	@Override
	public JDialog createDialog(Component parent)
		throws HeadlessException
	{
		this.dialog = super.createDialog(parent);
		ResourceMgr.setWindowIcons(dialog, "workbench");
		if (Settings.getInstance().restoreWindowSize(dialog, windowSettingsId))
		{
			dialog.setLocationRelativeTo(parent);
		}
		return dialog;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("JFileChooserDialogIsClosingProperty") && windowSettingsId != null)
		{
			try
			{
				JDialog d = (JDialog)evt.getOldValue();
				Settings.getInstance().storeWindowSize(d, windowSettingsId);
			}
			catch (Throwable th)
			{
				// ignore
			}
		}
    else if (selector != null && changeEvents.contains(evt.getPropertyName()))
    {
      // synchronize encoding selector with selected file(s)
      List<String> fileEncodings = getFileEncodings();
      if (CollectionUtil.isNonEmpty(fileEncodings))
      {
        String selectedEncoding = selector.getEncoding();

        if (fileEncodings.size() == 1 && StringUtil.stringsAreNotEqual(selectedEncoding, fileEncodings.get(0)))
        {
          selector.setEncoding(fileEncodings.get(0));
          WbSwingUtilities.showToolTip(getAccessory(), "Encoding was changed!");
        }
        else if (fileEncodings.size() > 1)
        {
          WbSwingUtilities.showErrorMessage("The selected files have different encodings!");
        }
      }
    }
	}

	public boolean validateInput()
	{
		JComponent accessory = getAccessory();
		if (accessory instanceof ValidatingComponent)
		{
			ValidatingComponent vc = (ValidatingComponent)accessory;
			return vc.validateInput();
		}

		if (isFileSelectionEnabled())
    {
      return encodingMatches();
    }
    else
		{
			File f = getSelectedFile();
			String errKey = null;
			if (!f.isDirectory())
			{
				errKey = "ErrExportOutputDirNotDir";
			}

			if (!f.exists())
			{
				errKey = "ErrOutputDirNotFound";
			}

			if (errKey != null)
			{
				String msg = ResourceMgr.getFormattedString(errKey, f.getAbsolutePath());
				WbSwingUtilities.showErrorMessage(this.dialog, msg);
				return false;
			}
		}
		return true;
	}

  private List<String> getFileEncodings()
  {
    Set<String> fileEncodings = CollectionUtil.caseInsensitiveSet();
    File[] files = getSelectedFiles();
    for (File f : files)
    {
      String fileEncoding = FileUtil.detectFileEncoding(f);
      if (fileEncoding != null)
      {
        fileEncodings.add(fileEncoding);
      }
    }
    return new ArrayList<>(fileEncodings);
  }

  private boolean encodingMatches()
  {
    if (selector == null) return true;

    String selectedEncoding = selector.getEncoding();
    File[] files = getSelectedFiles();

    if (files == null || files.length == 0) return true;

    List<String> fileEncodings = getFileEncodings();
    if (CollectionUtil.isEmpty(fileEncodings)) return true;

    if (fileEncodings.size() != 1)
    {
      WbSwingUtilities.showErrorMessage("The selected files have different encodings!");
      return false;
    }

    if (StringUtil.stringsAreNotEqual(fileEncodings.get(0), selectedEncoding))
    {
      WbSwingUtilities.showErrorMessage("The selected encoding does not match the detected file encoding: " + fileEncodings.get(0));
    }
    return true;
  }

	@Override
	public void approveSelection()
	{
		if (validateInput())
		{
			super.approveSelection();
		}
	}

}
