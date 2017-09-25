/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import workbench.log.LogMgr;

import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.sql.SqlPanel;

import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
class DropHandler
  implements DropTargetListener
{
  private final MainWindow client;
  private final DropTarget target;

  DropHandler(MainWindow client, Component dropTarget)
  {
    this.client = client;
    target = new DropTarget(dropTarget, DnDConstants.ACTION_COPY, this);
  }

  public void dispose()
  {
    if (target != null)
    {
      target.removeDropTargetListener(this);
    }
  }

  @Override
  public void dragEnter(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
  {
    dropTargetDragEvent.acceptDrag(DnDConstants.ACTION_COPY);
  }

  @Override
  public void dragExit(java.awt.dnd.DropTargetEvent dropTargetEvent)
  {
  }

  @Override
  public void dragOver(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
  {
  }

  @Override
  public void drop(java.awt.dnd.DropTargetDropEvent dropTargetDropEvent)
  {
    try
    {
      Transferable tr = dropTargetDropEvent.getTransferable();
      if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
      {
        dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY);
        List fileList = (List)tr.getTransferData(DataFlavor.javaFileListFlavor);
        if (isWorkspaceFile(fileList))
        {
          WbFile f = new WbFile((File)fileList.get(0));
          client.loadWorkspace(f.getFullPath(), true);
        }
        else if (fileList != null)
        {
          openFiles(fileList);
        }
      }
      else
      {
        dropTargetDropEvent.rejectDrop();
      }
    }
    catch (IOException | UnsupportedFlavorException io)
    {
      LogMgr.logError("MainWindow.drop()", "Error processing drop event", io);
      dropTargetDropEvent.rejectDrop();
    }
  }

  @Override
  public void dropActionChanged(java.awt.dnd.DropTargetDragEvent dropTargetDragEvent)
  {
  }

  private boolean isWorkspaceFile(final List fileList)
  {
    if (fileList == null) return false;
    if (fileList.size() != 1) return false;
    WbFile f = new WbFile((File)fileList.get(0));
    return (f.getExtension().equalsIgnoreCase(ExtensionFileFilter.WORKSPACE_EXT));
  }

  private void openFiles(final List fileList)
  {
    WbSwingUtilities.invokeLater(() ->
    {
      int count = fileList.size();

      for (int i = 0; i < count; i++)
      {
        File file = (File)fileList.get(i);
        boolean doSelect = (i == count - 1);
        SqlPanel newTab = (SqlPanel)client.addTab(doSelect, doSelect, true, true);
        newTab.readFile(file.getAbsolutePath(), null);
      }
    });
  }

}
