/*
 * PanelContentSender.java
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
package workbench.gui.sql;

import java.awt.EventQueue;

import workbench.interfaces.ResultReceiver;

import workbench.gui.MainWindow;

import workbench.util.WbThread;

/**
 * This class sends a SQL statement to one of the
 * panels in the MainWindow
 *
 * @author Thomas Kellerer
 */
public class PanelContentSender
{
	public static final int NEW_PANEL = -1;

	private MainWindow target;
	private String newTabName;

	public PanelContentSender(MainWindow window, String objectName)
	{
		this.target = window;
		newTabName = objectName;
	}

	public void showResult(String sql, String comment, int panelIndex, ResultReceiver.ShowType showHow)
	{
		if (sql == null) return;

		// This should not be done in the background thread
		// to make sure it's running on the EDT (otherwise the new panel will not be initialized correctly)
		final SqlPanel panel = selectPanel(panelIndex);

    if (panel == null) return;

    final ResultReceiver.ShowType type;
    if (panel.hasFileLoaded() && (showHow == ResultReceiver.ShowType.appendText || showHow == ResultReceiver.ShowType.replaceText))
    {
      // if there is a file loaded in the panel, never append or replace the editor's text
      type = ResultReceiver.ShowType.logText;
    }
    else
    {
      type = showHow;
    }

    if (panel == null) return;

		// When adding a new panel, a new connection
		// might be initiated automatically. As that is done in a separate
		// thread, the call to showResult() might occur before the connection is actually established.
		//
		// So we need to wait until the new panel is connected - that's what waitForConnection() is for.

		// As this code might be execute on the EDT we have to make sure
		// we are not blocking the current thread, so a new thread
		// is created that will wait for the connection to succeed.
		WbThread t = new WbThread("ShowFKThread")
    {
      @Override
      public void run()
      {
        target.waitForConnection();
        // the SqlPanel will start a new thread to run the SQL and will display the data on the EDT
        panel.showResult(sql, comment, type);
      }
    };
    t.start();
	}

	public void sendContent(final String text, final int panelIndex, final PasteType type)
	{
		if (text == null) return;

		final SqlPanel panel = selectPanel(panelIndex);
		if (panel == null) return;

		EventQueue.invokeLater(() ->
    {
      if (null != type)
        switch (type)
        {
          case append:
            panel.appendStatementText(text);
            break;
          case overwrite:
            panel.setStatementText(text);
            break;
          case insert:
            panel.addStatement(text);
            break;
          default:
        }
      target.requestFocus();
      panel.selectEditor();
    });
	}

	private SqlPanel selectPanel(int index)
	{
		SqlPanel panel;

    target.requestFocus();

		if (index == NEW_PANEL)
		{
			panel = (SqlPanel)this.target.addTab();
			panel.setTabName(newTabName);
		}
		else
		{
			panel = (SqlPanel)this.target.getSqlPanel(index);
			target.selectTab(index);
		}
		return panel;
	}
}

