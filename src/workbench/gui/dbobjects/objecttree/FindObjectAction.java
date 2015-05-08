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
package workbench.gui.dbobjects.objecttree;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import workbench.db.DbMetadata;
import workbench.interfaces.TextContainer;

import workbench.db.TableIdentifier;

import workbench.gui.actions.WbAction;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class FindObjectAction
  extends WbAction
{
  private ObjectFinder finder;
  private TextContainer textContainer;

  public FindObjectAction(TextContainer container)
  {
    super();
    initMenuDefinition("MnuTxtFindObjInTree");
    textContainer = container;
    setEnabled(false);
  }

  public void setFinder(ObjectFinder objFinder)
  {
    finder = objFinder;
    setEnabled(finder != null);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    if (textContainer == null) return;
    if (finder == null) return;

    String text = SqlUtil.getIdentifierAtCursor(textContainer, finder.getConnection());
    if (StringUtil.isBlank(text)) return;

    final TableIdentifier tbl = new TableIdentifier(text);

    DbMetadata meta = finder.getConnection().getMetadata();

    if (tbl.getCatalog() == null && !finder.getConnection().isBusy())
    {
      tbl.setCatalog(meta.getCurrentCatalog());
    }
    if (tbl.getSchema() == null && !finder.getConnection().isBusy())
    {
      tbl.setSchema(meta.getCurrentSchema());
    }

    EventQueue.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        finder.selectObject(tbl);
      }
    });
  }

}
