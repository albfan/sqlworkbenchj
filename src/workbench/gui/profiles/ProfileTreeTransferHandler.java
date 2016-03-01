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
package workbench.gui.profiles;

import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import workbench.log.LogMgr;

import workbench.db.ConnectionProfile;

/**
 *
 * @author Thomas Kellerer
 */
public class ProfileTreeTransferHandler
  extends TransferHandler
{
  private int lastClipboardAction = NONE;

  public ProfileTreeTransferHandler()
  {
    super();
  }

  @Override
  public int getSourceActions(JComponent c)
  {
    return COPY_OR_MOVE;
  }

  @Override
  public Transferable createTransferable(JComponent c)
  {
    if (c instanceof ProfileTree)
    {
      TreePath[] paths = ((ProfileTree)c).getSelectionPaths();
      return new ProfileTreeTransferable(paths, c.getName());
    }
    return null;
  }

  @Override
  public void exportDone(JComponent c, Transferable transferable, int action)
  {
    if (transferable == null) return;
    if (action != MOVE) return;

    try
    {
      TransferableProfileNode profileNode = (TransferableProfileNode)transferable.getTransferData(ProfileFlavor.FLAVOR);
      TreePath[] dropPath = profileNode.getPath();
      if (dropPath == null) return;

      ProfileTree tree = (ProfileTree)c;
      for (TreePath treePath : dropPath)
      {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
        tree.getModel().removeNodeFromParent(node);
      }
    }
    catch (Exception ex)
    {
      LogMgr.logError("ProfileTreeTransferHandler.importData()", "Could not process drop event", ex);
    }
  }

  @Override
  public boolean canImport(TransferSupport support)
  {
    if (!support.isDataFlavorSupported(ProfileFlavor.FLAVOR))
    {
      return false;
    }

    JTree.DropLocation location = (JTree.DropLocation)support.getDropLocation();
    TreePath path = location.getPath();
    if (path == null) return false;
    TreeNode node = (TreeNode)path.getLastPathComponent();
    return node.getAllowsChildren();
  }

  @Override
  public boolean importData(TransferSupport support)
  {
    ProfileTree tree = (ProfileTree)support.getComponent();
    if (tree == null) return false;

    DefaultMutableTreeNode parentNode = null;
    if (support.isDrop())
    {
      JTree.DropLocation location = (JTree.DropLocation)support.getDropLocation();
      TreePath path = location.getPath();
      if (path == null) return false;

      parentNode = (DefaultMutableTreeNode)path.getLastPathComponent();
    }
    else
    {
      parentNode = tree.getSelectedGroupNode();
    }

    if (parentNode == null) return false;

    int action = DnDConstants.ACTION_COPY;

    if (support.isDrop())
    {
      action = support.getDropAction();
    }
    else
    {
      action = lastClipboardAction;
    }

    try
    {
      Transferable transferable = support.getTransferable();
      if (transferable == null) return false;

      TransferableProfileNode profileNode = (TransferableProfileNode)transferable.getTransferData(ProfileFlavor.FLAVOR);
      TreePath[] dropPath = profileNode.getPath();
      if (dropPath == null) return false;


      List<ConnectionProfile> profiles = new ArrayList<>(dropPath.length);
      for (TreePath treePath : dropPath)
      {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
        ConnectionProfile profile = (ConnectionProfile)node.getUserObject();
        profiles.add(profile);
      }
      tree.handleDroppedNodes(profiles, parentNode, action);

      if (!support.isDrop() && action == MOVE)
      {
        exportDone(tree, transferable, action);
        lastClipboardAction = NONE;
      }

      return true;
    }
    catch (Exception ex)
    {
      LogMgr.logError("ProfileTreeTransferHandler.importData()", "Could not process drop event", ex);
      return false;
    }
  }

//  @Override
//  public void exportToClipboard(JComponent comp, Clipboard clip, int action)
//    throws IllegalStateException
//  {
//    lastClipboardAction = action;
//    super.exportToClipboard(comp, clip, action);
//  }

}
