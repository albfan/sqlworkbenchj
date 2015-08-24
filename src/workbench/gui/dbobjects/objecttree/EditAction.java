/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.gui.dbobjects.objecttree;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;

import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.gui.MainWindow;
import workbench.gui.dbobjects.EditorTabSelectMenu;
import workbench.gui.sql.PanelContentSender;
import workbench.gui.sql.PasteType;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class EditAction
  implements ActionListener
{
  private DbTreePanel dbTree;

  public EditAction(DbTreePanel tree)
  {
    this.dbTree = tree;
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (dbTree == null) return;
    ObjectTreeNode node = dbTree.getSelectedNode();

    if (node == null) return;

    final DbObject dbo = node.getDbObject();
    if (dbo == null) return;

    String command = e.getActionCommand();
    final int panelIndex = Integer.parseInt(command.substring(EditorTabSelectMenu.PANEL_CMD_PREFIX.length()));

    WbThread worker = new WbThread("DbTree source retrieval")
    {

      @Override
      public void run()
      {
        retrieveAndShow(dbo, panelIndex);
      }
    };
    worker.start();
  }

  private void retrieveAndShow(DbObject dbo, final int index)
  {
    final String source = retrieveSource(dbo);
    if (source == null) return;

    SwingUtilities.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        Window w = SwingUtilities.getWindowAncestor(dbTree);
        if (w instanceof MainWindow)
        {
          PanelContentSender sender = new PanelContentSender((MainWindow)w, dbo.getObjectName());
          sender.sendContent(source, index, PasteType.overwrite);
        }
      }
    });
  }

  private String retrieveSource(DbObject dbo)
  {
    WbConnection conn = dbTree.getConnection();
    if (conn == null) return null;

    if (conn.isBusy()) return null;
    if (dbo == null) return null;

    try
    {
      dbTree.getStatusBar().setStatusMessage(ResourceMgr.getString("MsgRetrieving"));
      try
      {
        // TODO: this needs to be done in the background
        CharSequence source = dbo.getSource(dbTree.getConnection());
        if (source == null) return null;
        return source.toString();
      }
      catch (Exception ex)
      {
        LogMgr.logError("EditAction.actionPerformed()", "Could not retrieve object source for: " + dbo.getObjectExpression(dbTree.getConnection()), ex);
      }
      return null;
    }
    finally
    {
      dbTree.getStatusBar().clearStatusMessage();
    }
  }
}
