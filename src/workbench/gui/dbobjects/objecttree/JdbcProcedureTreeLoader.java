/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.gui.dbobjects.objecttree;

import java.sql.SQLException;
import java.util.List;

import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.ReaderFactory;
import workbench.db.WbConnection;


public class JdbcProcedureTreeLoader
  implements ProcedureTreeLoader
{

  @Override
  public void loadProcedures(ObjectTreeNode procNode, DbObjectTreeModel model, WbConnection connection)
    throws SQLException
  {
    if (procNode == null) return;
    ProcedureReader procReader = ReaderFactory.getProcedureReader(connection.getMetadata());
    ObjectTreeNode schemaNode = procNode.getParent();
    ObjectTreeNode catalogNode = schemaNode.getParent();
    String catalogName = null;
    if (catalogNode != null && catalogNode.getType().equals(TreeLoader.TYPE_CATALOG))
    {
      catalogName = catalogNode.getName();
    }
    String schemaName = schemaNode.getName();
    List<ProcedureDefinition> procedures = procReader.getProcedureList(catalogName, schemaName, null);
    for (ProcedureDefinition proc : procedures)
    {
      ObjectTreeNode node = new ObjectTreeNode(proc);
      node.setAllowsChildren(true); // can have parameters
      node.setChildrenLoaded(false);
      procNode.add(node);
    }
    model.nodeStructureChanged(procNode);
    procNode.setChildrenLoaded(true);
  }
  
}
