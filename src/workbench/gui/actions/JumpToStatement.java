/*
 * JumpToStatement.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComboBox;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import workbench.resource.ResourceMgr;

import workbench.gui.components.ValidatingDialog;
import workbench.gui.sql.SqlPanel;

import workbench.sql.parser.ScriptParser;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class JumpToStatement
	extends WbAction
{
	private SqlPanel client;

	public JumpToStatement(SqlPanel panel)
	{
		super();
		initMenuDefinition("MnuTxtJumpToStatementNr", KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_MASK));
		setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		client = panel;
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		ScriptParser p = ScriptParser.createScriptParser(client.getConnection());
		p.setScript(client.getText());
		int count = p.getSize();

		JComboBox input = new JComboBox();
		input.setEditable(true);
		for (int i=0; i < count; i++)
		{
			input.addItem(Integer.toString(i + 1));
		}

		int current = p.getCommandIndexAtCursorPos(client.getEditor().getCaretPosition());
		input.setSelectedItem(Integer.toString(current+1));

		input.setMinimumSize(new Dimension(50, 24));
		boolean ok = ValidatingDialog.showConfirmDialog(SwingUtilities.getWindowAncestor(client), input, ResourceMgr.getString("TxtJumpTo"));
		if (!ok)
		{
			return;
		}
		String value = input.getSelectedItem().toString();
		if (StringUtil.isBlank(value)) return;

		int stmt = Integer.valueOf(value);
		final int pos = p.getStartPosForCommand(stmt - 1);
		EventQueue.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				client.getEditor().setCaretPosition(pos);
			}
		});

	}

}
