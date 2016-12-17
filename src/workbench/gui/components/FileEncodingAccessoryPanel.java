/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import workbench.interfaces.EncodingSelector;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.MainWindow;

/**
 * A JPanel that can be used for a JFileChooser to select the file's encoding.
 * @author Thomas Kellerer
 */
public class FileEncodingAccessoryPanel
  extends JPanel
  implements EncodingSelector
{
  private final EncodingDropDown encodingDropDown;
  private final JCheckBox autoDetectEncoding;
  private JCheckBox openInNewTab;

  public FileEncodingAccessoryPanel(MainWindow window)
  {
    super(new GridBagLayout());

    encodingDropDown = new EncodingDropDown();
    encodingDropDown.setBorder(new EmptyBorder(0, 5, 0, 0));

    autoDetectEncoding = new JCheckBox(ResourceMgr.getString("LblDetectEncoding"));
    autoDetectEncoding.setSelected(GuiSettings.autoDetectFileEncoding());

    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.anchor = GridBagConstraints.NORTHEAST;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(autoDetectEncoding, c);

    c.gridy++;
    add(encodingDropDown, c);

    if (window != null)
    {
      openInNewTab = new JCheckBox(ResourceMgr.getString("LblOpenNewTab"));
      openInNewTab.setToolTipText(ResourceMgr.getDescription("LblOpenNewTab"));

      if (window.getCurrentSqlPanel() == null)
      {
        // DbExplorer is open, force open in new tab!
        openInNewTab.setSelected(true);
        openInNewTab.setEnabled(false);
      }
      else
      {
        openInNewTab.setSelected(Settings.getInstance().getBoolProperty("workbench.file.newtab", false));
      }

      c.gridy++;
      c.insets = new Insets(5, 0, 0, 0);
      c.weighty = 1.0;
      add(openInNewTab, c);
    }

    encodingDropDown.setEncoding(Settings.getInstance().getDefaultFileEncoding());
  }

  @Override
  public String getEncoding()
  {
    return encodingDropDown.getEncoding();
  }

  @Override
  public void setEncoding(String encoding)
  {
    encodingDropDown.setEncoding(encoding);
  }

  public boolean openInNewTab()
  {
    if (openInNewTab == null) return false;
    return openInNewTab.isSelected();
  }

  public boolean getAutoDetect()
  {
    return autoDetectEncoding.isSelected();
  }
}
