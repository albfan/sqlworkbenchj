/*
 * CreateDropScriptAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.WbManager;

import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DropScriptGenerator;
import workbench.db.TableIdentifier;

import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ObjectScripterUI;

import workbench.util.CollectionUtil;

import static workbench.gui.actions.WbAction.isCtrlPressed;

/**
 * @author Thomas Kellerer
 */
public class CreateDropScriptAction
	extends WbAction
	implements ListSelectionListener
{
	private DbObjectList source;

	public CreateDropScriptAction(DbObjectList client, ListSelectionModel list)
	{
		super();
		this.initMenuDefinition("MnuTxtGenerateDrop");
		this.source = client;
		setEnabled(false);
		list.addListSelectionListener(this);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		List<? extends DbObject> objects = source.getSelectedObjects();
		List<TableIdentifier> tables = new ArrayList<TableIdentifier>(objects.size());
		DbMetadata meta = source.getConnection().getMetadata();
		Set<String> types = CollectionUtil.caseInsensitiveSet();
		types.addAll(meta.getTableTypes());

		for (DbObject dbo : objects)
		{
			if (types.contains(dbo.getObjectType()))
			{
				tables.add((TableIdentifier)dbo);
			}
		}
		DropScriptGenerator generator = new DropScriptGenerator(source.getConnection());
		if (isCtrlPressed(e))
		{
			generator.setIncludeRecreateStatements(false);
		}

		if (isAltPressed(e))
		{
			generator.setIncludeDropTable(false);
		}

		generator.setTables(tables);
		ObjectScripterUI ui = new ObjectScripterUI(generator);
		ui.show(WbManager.getInstance().getCurrentWindow());
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		List<? extends DbObject> objects = source.getSelectedObjects();

		if (CollectionUtil.isEmpty(objects))
		{
			setEnabled(false);
		}
		else
		{
			DbMetadata meta = source.getConnection().getMetadata();
			Set<String> types = CollectionUtil.caseInsensitiveSet();
			types.addAll(meta.getTableTypes());
			for (DbObject dbo : objects)
			{
				if (!types.contains(dbo.getObjectType()))
				{
					setEnabled(false);
					return;
				}
				setEnabled(true);
			}
		}
	}

}
