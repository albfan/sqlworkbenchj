/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
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
package workbench.interfaces;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectionFacade
  implements WbSelectionModel, ListSelectionListener, TreeSelectionListener
{
  private final ListSelectionModel listModel;
  private final TreeSelectionModel treeModel;
  private WbSelectionListener listener;

  public SelectionFacade(ListSelectionModel selection)
  {
    listModel = selection;
    treeModel = null;
  }

  public SelectionFacade(TreeSelectionModel selection)
  {
    treeModel = selection;
    listModel = null;
  }

  @Override
  public void addSelectionListener(WbSelectionListener selectionListener)
  {
    if (treeModel != null)
    {
      treeModel.addTreeSelectionListener(this);
    }
    else
    {
      listModel.addListSelectionListener(this);
    }
    listener = selectionListener;
  }

  @Override
  public void removeSelectionListener(WbSelectionListener listener)
  {
    listener = null;
    if (treeModel != null)
    {
      treeModel.removeTreeSelectionListener(this);
    }
    else
    {
      listModel.removeListSelectionListener(this);
    }
  }

  @Override
  public boolean hasSelection()
  {
    if (treeModel != null)
    {
      return treeModel.getSelectionCount() > 0;
    }
    return listModel.getMinSelectionIndex() >= 0;
  }


  @Override
  public int getSelectionCount()
  {
    if (treeModel != null)
    {
      return treeModel.getSelectionCount();
    }
    int count = 0;
    int min = listModel.getMinSelectionIndex();
    int max = listModel.getMaxSelectionIndex();
    for (int i = min; i <= max; i++)
    {
      if (listModel.isSelectedIndex(i))
      {
        count++;
      }
    }
    return count;
  }

  @Override
  public void valueChanged(ListSelectionEvent e)
  {
    if (listener != null)
    {
      listener.selectionChanged(this);
    }
  }

  @Override
  public void valueChanged(TreeSelectionEvent e)
  {
    if (listener != null)
    {
      listener.selectionChanged(this);
    }
  }

}
