/*
 * ImportClipboardAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.IOException;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.SqlPanel;

import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;

/**
 * Import data from the clipboard into a table
 *
 * @author Thomas Kellerer
 */
public class ImportClipboardAction
  extends WbAction
{
  private SqlPanel client;

  public ImportClipboardAction(SqlPanel panel)
  {
    super();
    this.initMenuDefinition("MnuTxtImportClip");
    this.setMenuItemName(ResourceMgr.MNU_TXT_DATA);
    client = panel;
    this.setEnabled(false);
  }

  @Override
  public boolean hasCtrlModifier()
  {
    return true;
  }

  @Override
  public void executeAction(ActionEvent evt)
  {
    String content = getClipboardContents();
    if (StringUtil.isBlank(content)) return;
    boolean showOptions = false;
    if (invokedByMouse(evt))
    {
      showOptions = isCtrlPressed(evt);
    }
    client.importString(content, showOptions);
  }

  private String getClipboardContents()
  {
    if (client == null)
    {
      return null;
    }
    Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
    Transferable content = clp.getContents(client);

    DataFlavor[] flavors = content.getTransferDataFlavors();
    StringBuilder info = new StringBuilder();
    if (flavors != null)
    {
      for (int i = 0; i < flavors.length; i++)
      {
        info.append(flavors[i].getHumanPresentableName());
        if (i > 0) info.append(", ");
      }
    }

    LogMgr.logDebug("ImportClipboardAction.getClipboardContents()", "Supported formats of the clipboard: " + info);

    if (!content.isDataFlavorSupported(DataFlavor.stringFlavor))
    {
      LogMgr.logError("ImportClipboardAction.getClipboardContents()", "The clipboard does not contain a format compatible with a string. Clipboard format is: " + info, null);
      WbSwingUtilities.showErrorMessageKey(client, "MsgClipInvalid");
      return null;
    }

    try
    {
      return (String)content.getTransferData(DataFlavor.stringFlavor);
    }
    catch (UnsupportedFlavorException ufe)
    {
      LogMgr.logError("ImportClipboardAction.getClipboardContents()", "The current clipboard content cannot be used as a String", ufe);
      WbSwingUtilities.showErrorMessageKey(client, "MsgClipInvalid");
    }
    catch (IOException io)
    {
      LogMgr.logError("ImportClipboardAction.getClipboardContents()", "The current clipboard content can no longer be accessed", io);
      WbSwingUtilities.showErrorMessage(client, "<html>" + ResourceMgr.getString("MsgClipInvalid") + "<br>(" + ExceptionUtil.getDisplay(io) + ")</html>");
    }
    catch (Throwable e)
    {
      LogMgr.logError("ImportClipboardAction.getClipboardContents()", "Error accessing clipboard", e);
    }
    return null;
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }

}
