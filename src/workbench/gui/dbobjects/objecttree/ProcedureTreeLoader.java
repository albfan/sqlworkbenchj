/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.gui.dbobjects.objecttree;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import workbench.db.BaseObjectType;
import workbench.db.PackageDefinition;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.ReaderFactory;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class ProcedureTreeLoader
{

  public void loadProcedures(ObjectTreeNode procNode, DbObjectTreeModel model, WbConnection connection)
    throws SQLException
  {
    if (procNode == null) return;
    ProcedureReader procReader = ReaderFactory.getProcedureReader(connection.getMetadata());
    ObjectTreeNode schemaNode = procNode.getParent();
    String schemaName = schemaNode.getName();
    List<ProcedureDefinition> procedures = procReader.getProcedureList(null, schemaName, null);

    Map<String, List<ProcedureDefinition>> procs = getPackageProcedures(procedures);
    for (Map.Entry<String, List<ProcedureDefinition>> entry : procs.entrySet())
    {
      PackageDefinition pkg = new PackageDefinition(schemaName, entry.getKey());
      ObjectTreeNode pkgNode = new ObjectTreeNode(pkg);
      pkgNode.setNodeType(TreeLoader.TYPE_PACKAGE_NODE);
      pkgNode.setAllowsChildren(true);
      for (ProcedureDefinition proc : entry.getValue())
      {
        ObjectTreeNode node = new ObjectTreeNode(proc);
        node.setAllowsChildren(true); // can have parameters
        node.setChildrenLoaded(false);
        pkgNode.add(node);
      }
      pkgNode.setChildrenLoaded(true);
      procNode.add(pkgNode);
    }

    Map<String, List<ProcedureDefinition>> types = getTypeMethods(procedures);
    for (Map.Entry<String, List<ProcedureDefinition>> entry : types.entrySet())
    {
      BaseObjectType typeDbo = new BaseObjectType(schemaName, entry.getKey());
      ObjectTreeNode pkgNode = new ObjectTreeNode(typeDbo);
      pkgNode.setAllowsChildren(true);
      for (ProcedureDefinition proc : entry.getValue())
      {
        ObjectTreeNode node = new ObjectTreeNode(proc);
        node.setAllowsChildren(true); // can have parameters
        node.setChildrenLoaded(false);
        pkgNode.add(node);
      }
      pkgNode.setChildrenLoaded(true);
      procNode.add(pkgNode);
    }

    for (ProcedureDefinition proc : procedures)
    {
      if (!proc.isPackageProcedure() && !proc.isOracleObjectType())
      {
        ObjectTreeNode node = new ObjectTreeNode(proc);
        node.setAllowsChildren(true); // can have parameters
        node.setChildrenLoaded(false);
        procNode.add(node);
      }
    }
    procNode.setChildrenLoaded(true);
    model.nodeStructureChanged(procNode);
  }

  private Map<String, List<ProcedureDefinition>> getTypeMethods(List<ProcedureDefinition> allProcs)
  {
     Map<String, List<ProcedureDefinition>> packages = new TreeMap<>();
     for (ProcedureDefinition def : allProcs)
     {
       if (def.isOracleObjectType())
       {
         List<ProcedureDefinition> procList = packages.get(def.getPackageName());
         if (procList == null)
         {
           procList = new ArrayList<>();
           packages.put(def.getPackageName(), procList);
         }
         procList.add(def);
       }
     }
     return packages;
  }

  private Map<String, List<ProcedureDefinition>> getPackageProcedures(List<ProcedureDefinition> allProcs)
  {
     Map<String, List<ProcedureDefinition>> packages = new TreeMap<>();
     for (ProcedureDefinition def : allProcs)
     {
       if (def.isPackageProcedure())
       {
         List<ProcedureDefinition> procList = packages.get(def.getPackageName());
         if (procList == null)
         {
           procList = new ArrayList<>();
           packages.put(def.getPackageName(), procList);
         }
         procList.add(def);
       }
     }
     return packages;
  }
}
