/*
 * ShowSourceQueryAction.java
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
package workbench.gui.actions;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ShowSourceQueryAction
	extends WbAction
{
	private SqlPanel panel;

	public ShowSourceQueryAction(SqlPanel handler)
	{
		panel = handler;
		isConfigurable = false;
		initMenuDefinition("MnuTxtShowQuery");
	}

	@Override
	public boolean isEnabled()
	{
		return (panel != null && panel.getSourceQuery() != null);
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		showQuery();
	}

	public void showQuery()
	{
		final EditorPanel editor = EditorPanel.createSqlEditor();
		String sql = panel.getSourceQuery();

		JPanel display = new JPanel(new BorderLayout());

		editor.setText(sql);
		editor.setCaretPosition(0);
		editor.setEditable(false);
		Window w = SwingUtilities.getWindowAncestor(panel);
		Frame f = null;
		if (w instanceof Frame)
		{
			f = (Frame)w;
		}
		else
		{
			f = WbManager.getInstance().getCurrentWindow();
		}

		String loadedAt = StringUtil.formatIsoTimestamp(panel.getLoadedAt());
		String msg = ResourceMgr.getFormattedString("TxtLastExec", loadedAt);
		JLabel lbl = new JLabel(msg);
		lbl.setBorder(new EmptyBorder(0, 2, 3, 0));

		display.add(editor, BorderLayout.CENTER);
		display.add(lbl, BorderLayout.NORTH);

		ValidatingDialog d = new ValidatingDialog(f, "SQL", display, false);
		if (!Settings.getInstance().restoreWindowSize(d, "workbench.resultquery.display"))
		{
			d.setSize(500,350);
		}
		WbSwingUtilities.center(d, f);
		WbSwingUtilities.repaintLater(editor);
		d.setVisible(true);
		Settings.getInstance().storeWindowSize(d,"workbench.resultquery.display");
	}
}
