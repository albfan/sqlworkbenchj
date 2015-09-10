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
package workbench.gui.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.resource.Settings;

import workbench.db.ObjectNameFilter;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectFilterTemplateMgr
{
  private final String prefix = "workbench.gui.sql.filter.template.";
  private List<ObjectFilterTemplate> templates;

  public ObjectFilterTemplateMgr()
  {
    readTemplates();
  }

  private void readTemplates()
  {
    List<String> keys = Settings.getInstance().getKeysLike(prefix + "name");
    templates = new ArrayList<>(keys.size());
    for (String key : keys)
    {
      String name = Settings.getInstance().getProperty(key, null);
      String idx = getTemplateIndex(key);
      if (idx != null && name != null)
      {
        String defKey = prefix + idx + ".definition";
        String def = Settings.getInstance().getProperty(defKey, null);
        ObjectNameFilter filter = new ObjectNameFilter();
        filter.setExpressionList(def);
        ObjectFilterTemplate template = new ObjectFilterTemplate(name, filter);
        templates.add(template);
      }
    }
  }

  public synchronized void saveTemplates()
  {
    if (templates == null) return;

    for (int i=0; i < templates.size(); i++)
    {
      String nameKey = prefix + "name." + Integer.toString(i);
      String defKey = prefix + "." + Integer.toString(i) + ".definition";
      ObjectFilterTemplate template = templates.get(i);
      Settings.getInstance().setProperty(nameKey, template.getName());
      Settings.getInstance().setProperty(defKey, template.getFilter().getFilterString());
    }
  }
  
  public void addTemplate(String name, String definition)
  {
    ObjectNameFilter filter = new ObjectNameFilter();
    filter.setExpressionList(definition);
    ObjectFilterTemplate template = new ObjectFilterTemplate(name, filter);
    templates.add(template);
  }

  public List<ObjectFilterTemplate> getTemplates()
  {
    return Collections.unmodifiableList(templates);
  }

  private String getTemplateIndex(String key)
  {
    if (key == null) return null;
    int pos = key.lastIndexOf('.');
    return key.substring(pos + 1);
  }

}
