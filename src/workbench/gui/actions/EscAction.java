/*
 * EscAction.java
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

/**
 *	An action mapped to the ESC key
 *	@author  Thomas Kellerer
 */
public class EscAction 
	extends WbAction
{
	private ActionListener client;

	public EscAction(JDialog d, ActionListener aClient)
	{
		super();
		client = aClient;
		isConfigurable = false;
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0));
		addToInputMap(d.getRootPane());
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		e.setSource(this);
		this.client.actionPerformed(e);
	}

	@Override
	public void addToInputMap(JComponent c)
	{
		super.addToInputMap(c, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

}
