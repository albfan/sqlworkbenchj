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

import workbench.gui.components.ConnectionInfo;
import workbench.gui.components.WbToolbar;

/**
 * The toolbar of the main window.
 *
 * @author Thomas Kellerer
 */
public class MainToolbar
  extends WbToolbar
{
  private ConnectionInfo connectionInfo;
  public MainToolbar()
  {
    super();
    addDefaultBorder();
  }

  public void addConnectionInfo()
  {
    if (connectionInfo == null)
    {
      connectionInfo = new ConnectionInfo(getBackground());
    }
    add(connectionInfo);
  }

  public ConnectionInfo getConnectionInfo()
  {
    return connectionInfo;
  }

}
