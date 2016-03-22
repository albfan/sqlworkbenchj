/*
 * DerbyColumnEnhancer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.derby;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DerbyColumnEnhancer
  implements ColumnDefinitionEnhancer
{

  @Override
  public void updateColumnDefinition(TableDefinition table, WbConnection conn)
  {
    for (ColumnIdentifier col : table.getColumns())
    {
      String defaultValue = col.getDefaultValue();
      if (StringUtil.isNonBlank(defaultValue))
      {
        if (defaultValue.startsWith("GENERATED ALWAYS AS"))
        {
          col.setComputedColumnExpression(defaultValue);
        }
        if (defaultValue.startsWith("AUTOINCREMENT:") || defaultValue.equals("GENERATED_BY_DEFAULT"))
        {
          col.setIsAutoincrement(true);
        }
      }
    }
  }

}
