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
package workbench.gui.toolbar;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import workbench.gui.actions.WbAction;

/**
 *
 * @author Thomas Kellerer
 */
public class ActionListModel
  extends AbstractListModel
{
  private List objects;

  public ActionListModel(List values)
  {
    objects = new ArrayList(values);
  }

  @Override
  public int getSize()
  {
    return objects.size();
  }

  public void set(int index, Object newValue)
  {
    objects.set(index, newValue);
    fireContentsChanged(this, index, index);
  }

  public void addItem(int index, Object value)
  {
    objects.add(index, value);
    fireContentsChanged(this, index, index);
  }

  public void addItem(Object value)
  {
    boolean added = objects.add(value);
    if (added)
    {
      int lastIndex = objects.size() - 1;
      fireContentsChanged(this, lastIndex, lastIndex);
    }
  }

  public void removeItem(int index)
  {
    objects.remove(index);
    fireContentsChanged(this, index, index);
  }

  @Override
  public Object getElementAt(int index)
  {
    return objects.get(index);
  }

  public List<WbAction> getActions()
  {
    List<WbAction> result = new ArrayList<>();
    for (Object obj : objects)
    {
      if (obj instanceof WbAction)
      {
        WbAction action = (WbAction)obj;
        result.add(action);
      }
    }
    return result;
  }

  public String getToolbarCommands()
  {
    StringBuilder result = new StringBuilder(objects.size() * 20);
    for (Object obj : objects)
    {
      if (result.length() > 0) result.append(',');
      if (obj instanceof String)
      {
        result.append(ToolbarBuilder.SEPARATOR_KEY);
      }
      else if (obj instanceof WbAction)
      {
        WbAction action = (WbAction)obj;
        result.append(action.getActionCommand());
      }
    }
    return result.toString();
  }
}
