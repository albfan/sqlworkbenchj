/*
 * WbFileChooser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;

import workbench.interfaces.EncodingSelector;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;

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
  private FileEncodingAccessoryPanel encodingPanel;
  private EncodingSelector encodingSelector;

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

  public void setEncodingSelector(EncodingSelector select)
  {
    encodingSelector = select;
  }

  public void addEncodingPanel(FileEncodingAccessoryPanel comp)
  {
    encodingPanel = comp;
    encodingSelector = comp;
    super.setAccessory(comp);
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

    if (encodingPanel != null && this.encodingPanel.getAutoDetect() && isSelectionChange(evt.getPropertyName()))
    {
      updateEncoding();
    }
	}

  private boolean isSelectionChange(String propName)
  {
    return propName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY) ||
           propName.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY);
  }

  private void updateEncoding()
  {
    List<String> encodings = getFileEncodings();
    if (encodings.isEmpty())
    {
      encodingPanel.setEncoding(Settings.getInstance().getDefaultFileEncoding());
    }
    else if (encodings.size() == 1)
    {
      encodingPanel.setEncoding(encodings.get(0));
    }
    else
    {
      encodingPanel.setEncoding("");
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

    File[] files = null;

    if (isMultiSelectionEnabled())
    {
      files = getSelectedFiles();
    }
    else
    {
      File file = getSelectedFile();
      if (file != null) files = new File[] { file };
    }

    if (files == null || files.length == 0) return Collections.emptyList();

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
    if (encodingSelector == null) return true;

    String selectedEncoding = encodingSelector.getEncoding();
    File[] files = getSelectedFiles();

    if (files == null || files.length == 0) return true;

    List<String> fileEncodings = getFileEncodings();
    if (CollectionUtil.isEmpty(fileEncodings)) return true;

    if (fileEncodings.size() != 1)
    {
      WbSwingUtilities.showErrorMessage(ResourceMgr.getString("MsgEncodingsMismatch"));
      return false;
    }

    if (StringUtil.equalStringIgnoreCase(fileEncodings.get(0), selectedEncoding) == false)
    {
      String msg = ResourceMgr.getFormattedString("MsgEncodingWrong", selectedEncoding,  fileEncodings.get(0));
      return WbSwingUtilities.getYesNo(this, msg);
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
