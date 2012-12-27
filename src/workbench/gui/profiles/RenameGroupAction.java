/*
 * RenameGroupAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.gui.profiles;

import java.awt.event.ActionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import workbench.gui.actions.WbAction;

/**
 * @author Thomas Kellerer
 */
public class RenameGroupAction
	extends WbAction
	implements TreeSelectionListener
{
	private ProfileTree client;

	public RenameGroupAction(ProfileTree panel)
	{
		super();
		this.client = panel;
		this.client.addTreeSelectionListener(this);
		this.initMenuDefinition("LblRenameProfileGroup");
	}

	public void executeAction(ActionEvent e)
	{
		client.renameGroup();
	}

	public void valueChanged(TreeSelectionEvent e)
	{
		this.setEnabled(client.onlyGroupSelected());
	}

}
