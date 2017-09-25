/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import workbench.resource.GuiSettings;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TagListModel
  implements ListModel<String>
{
  private final List<String> elements;
  private final List<String> filtered;

  private List<ListDataListener> listeners = new ArrayList<>(1);

  public TagListModel(Collection<String> allElements)
  {
    elements = new ArrayList<>(allElements);
    filtered = new ArrayList<>(elements.size());
    elements.sort(CaseInsensitiveComparator.INSTANCE);
  }

  public void clear()
  {
    elements.clear();
    filtered.clear();
  }

  @Override
  public int getSize()
  {
    return elements.size();
  }

  @Override
  public String getElementAt(int index)
  {
    return elements.get(index);
  }

  @Override
  public void addListDataListener(ListDataListener l)
  {
    listeners.add(l);
  }

  @Override
  public void removeListDataListener(ListDataListener l)
  {
    listeners.remove(l);
  }

  public synchronized void applyFilter(String value)
  {
    elements.addAll(filtered);
    filtered.clear();

    if (StringUtil.isNonEmpty(value))
    {
      value = value.toLowerCase();
      for (String element : elements)
      {
        if (GuiSettings.getTagCompletionUseContainsFilter())
        {
          if (!element.toLowerCase().contains(value))
          {
            filtered.add(element);
          }
        }
        else
        {
          if (!element.toLowerCase().startsWith(value))
          {
            filtered.add(element);
          }
        }
      }
      elements.removeAll(filtered);
    }

    elements.sort(CaseInsensitiveComparator.INSTANCE);
    fireDataChanged();
  }

	private void fireDataChanged()
	{
		ListDataEvent evt = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize() - 1);
		for (ListDataListener l : this.listeners)
		{
			l.contentsChanged(evt);
		}
	}

}
