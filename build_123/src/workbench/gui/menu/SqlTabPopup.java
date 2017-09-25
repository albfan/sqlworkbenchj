/*
 * SqlTabPopup.java
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
package workbench.gui.menu;

import java.util.Optional;

import javax.swing.JPopupMenu;
import workbench.gui.MainWindow;
import workbench.gui.actions.AddTabAction;
import workbench.gui.actions.CloseOtherTabsAction;
import workbench.gui.actions.CopyFileNameAction;
import workbench.gui.actions.FileDiscardAction;
import workbench.gui.actions.NewDbExplorerPanelAction;
import workbench.gui.actions.RemoveTabAction;
import workbench.gui.actions.RenameTabAction;
import workbench.gui.actions.RestoreClosedTabAction;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.SqlPanel;
import workbench.interfaces.MainPanel;
import workbench.gui.actions.InsertTabAction;
import workbench.gui.actions.LockPanelAction;
import workbench.gui.actions.MoveSqlTabLeft;
import workbench.gui.actions.MoveSqlTabRight;
import workbench.gui.actions.OpenFileAction;
import workbench.gui.actions.OpenFileDirAction;
import workbench.gui.actions.ToggleExtraConnection;

/**
 * @author  Thomas Kellerer
 */
public class SqlTabPopup
	extends JPopupMenu
{
	public SqlTabPopup(MainWindow aClient)
	{
		super();
		AddTabAction add = new AddTabAction(aClient);
		this.add(add);
		InsertTabAction insert = new InsertTabAction(aClient);
		this.add(insert);

		NewDbExplorerPanelAction newDbExp = new NewDbExplorerPanelAction(aClient, "MnuTxtAddExplorerPanel");
		newDbExp.removeIcon();
		add(newDbExp);

		addSeparator();

		RemoveTabAction remove = new RemoveTabAction(aClient);
		remove.setEnabled(aClient.canCloseTab());
		this.add(remove);

    Optional<MainPanel> panel = aClient.getCurrentPanel();

		CloseOtherTabsAction closeOthers = new CloseOtherTabsAction(aClient);
		this.add(closeOthers);

    RestoreClosedTabAction restoreClosedTabAction = new RestoreClosedTabAction(aClient);
    this.add(restoreClosedTabAction.getMenuItem());

		if (aClient.canRenameTab())
		{
			RenameTabAction rename = new RenameTabAction(aClient);
			this.add(rename);
		}

		LockPanelAction lock = new LockPanelAction(panel);

		this.add(lock.getMenuItem());
    lock.setSwitchedOn(panel.map(MainPanel::isLocked).orElse(false));

		this.addSeparator();

		int currentIndex = aClient.getCurrentPanelIndex();
		MoveSqlTabLeft moveLeft = new MoveSqlTabLeft(aClient);
		moveLeft.setEnabled(currentIndex > 0);
		this.add(moveLeft);
		int lastIndex = aClient.getTabCount();
		MoveSqlTabRight moveRight = new MoveSqlTabRight(aClient);
		moveRight.setEnabled(currentIndex < lastIndex);
		this.add(moveRight);

		if (aClient.canUseSeparateConnection())
		{
			this.addSeparator();
			ToggleExtraConnection toggle = new ToggleExtraConnection(aClient);
			this.add(toggle.getMenuItem());
		}

		this.addSeparator();

    MainPanel mpanel = panel.get();
    if (mpanel instanceof SqlPanel)
    {
      SqlPanel spanel = (SqlPanel)mpanel;

      EditorPanel editor = spanel.getEditor();

      this.add(editor.getFileSaveAction());
      this.add(editor.getFileSaveAsAction());
      this.add(new OpenFileAction(aClient));

      if (editor.hasFileLoaded())
      {
        this.add(editor.getReloadAction());
        FileDiscardAction discard = new FileDiscardAction(spanel);
        discard.removeIcon();
        this.add(discard);
        this.addSeparator();
        this.add(new CopyFileNameAction(editor, true));
        this.add(new CopyFileNameAction(editor, false));
        this.add(new OpenFileDirAction(editor));
      }
    }
    else
    {
      this.add(new OpenFileAction(aClient));
    }
  }

}
