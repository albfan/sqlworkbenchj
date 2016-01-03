/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.dbobjects.objecttree;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Serializable;


/**
 *
 * @author Thomas Kellerer
 */
public class ObjectTreeTransferable
implements Transferable, Serializable
{
	public static final DataFlavor DATA_FLAVOR = new DataFlavor(ObjectTreeNode.class, "DbObjectNode");

  private ObjectTreeNode[] selection;
  private String connectionId;

  public ObjectTreeTransferable(ObjectTreeNode[] nodes, String connId)
  {
    selection = nodes;
    // I can't figure out how to pass a component through the Transferable interface
    // Storing the JTree always results in exceptions when accessing this Transferable in the drop() event.
    connectionId = connId;
  }

  @Override
  public DataFlavor[] getTransferDataFlavors()
  {
    return new DataFlavor[] { DATA_FLAVOR };
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor)
  {
    return DATA_FLAVOR.equals(flavor);
  }

  @Override
  public Object getTransferData(DataFlavor flavor)
    throws UnsupportedFlavorException, IOException
  {
    return this;
  }

  public ObjectTreeNode[] getSelectedNodes()
  {
    return selection;
  }

  public String getConnectionId()
  {
    return connectionId;
  }

}
