/*
 * ExpandResultAction.java
 *
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.resource.ResourceMgr;

import workbench.gui.sql.SplitPaneExpander;

/**
 * Expand the result panel in the editor to the full window size.
 *
 * @author Thomas Kellerer
 */
public class ExpandResultAction
  extends WbAction
{
  private SplitPaneExpander client;

  public ExpandResultAction(SplitPaneExpander expander)
  {
    super();
    this.client = expander;
    this.initMenuDefinition("MnuTxtExpandResult");
    this.setMenuItemName(ResourceMgr.MNU_TXT_VIEW);
    this.setIcon(null);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    this.client.toggleLowerComponentExpand();
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
