/*
 * JumpToStatement
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.actions;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JComboBox;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;
import workbench.sql.ScriptParser;
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
		initMenuDefinition("MnuTxtJumpToStatementNr");
		setDefaultAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_MASK));
		setMenuItemName(ResourceMgr.MNU_TXT_SQL);
		initializeShortcut();
		client = panel;
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		ScriptParser p = client.createScriptParser();
		p.setScript(client.getText());
		int count = p.getSize();

		JComboBox input = new JComboBox();
		input.setEditable(true);
		for (int i=0; i < count; i++)
		{
			input.addItem(Integer.toString(i + 1));
		}
		input.setSelectedIndex(0);
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
