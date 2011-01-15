/*
 * CloseOtherTabsAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import workbench.gui.MainWindow;
import workbench.interfaces.MainPanel;

/**
 *
 * @author Thomas Kellerer
 */
public class CloseOtherTabsAction
	extends WbAction
{
	private MainPanel keepOpen;
	private MainWindow client;
	public CloseOtherTabsAction(MainWindow window)
	{
		keepOpen = window.getCurrentPanel();
		client = window;
		initMenuDefinition("MnuTxtCloseOthers");
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		client.closeOtherPanels(keepOpen);
	}

}
