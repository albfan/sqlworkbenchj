/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.gui.dbobjects.objecttree;

import java.sql.SQLException;

import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public interface ProcedureTreeLoader
{
  void loadProcedures(ObjectTreeNode procNode, DbObjectTreeModel model, WbConnection connection)
    throws SQLException;

}
