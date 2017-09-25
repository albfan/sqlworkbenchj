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
package workbench.gui.dbobjects.objecttree;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 *
 * @author Thomas Kellerer
 */
public class PlainTextTransferable
  implements Transferable
{
  private final String transferData;

  public PlainTextTransferable(String data)
  {
    transferData = data;
  }

  @Override
  public DataFlavor[] getTransferDataFlavors()
  {
    return new DataFlavor[] {DataFlavor.stringFlavor};
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor)
  {
    return flavor.isFlavorTextType();
  }

  @Override
  public Object getTransferData(DataFlavor flavor)
    throws UnsupportedFlavorException, IOException
  {
    return transferData;
  }

}
