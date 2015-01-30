/*
 * KeyboardMapper.java
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
package workbench.gui.settings;

import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import workbench.resource.ResourceMgr;

/**
 *
 * @author  Thomas Kellerer
 */
public class KeyboardMapper
	extends JPanel
	implements KeyListener
{
	private JTextField display;
	private KeyStroke newkey;

	public KeyboardMapper()
	{
		super();
		this.display = new JTextField(20);
		this.display.addKeyListener(this);
		this.display.setEditable(false);
		this.display.setDisabledTextColor(display.getForeground());
		this.display.setBackground(UIManager.getColor("TextArea.background"));
		this.add(display);
	}

	@Override
	public void grabFocus()
	{
		this.display.grabFocus();
		this.display.requestFocusInWindow();
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		int modifier = e.getModifiers();
		int code = e.getKeyCode();

		// only allow action keys without modifier!
		if (modifier == 0 && !e.isActionKey()) return;

		// keyReleased is also called when the Ctrl or Shift keys are release
		// in that case the keycode is 0 --> ignore it
		if (code >= 32
			|| code == KeyEvent.VK_ENTER
			|| code == KeyEvent.VK_BACK_SPACE
			|| code == KeyEvent.VK_TAB
			|| code == KeyEvent.VK_ESCAPE)
		{
			String key = KeyEvent.getKeyText(code);
			if (modifier > 0) key = KeyEvent.getKeyModifiersText(modifier) + "-" + key;
			this.newkey = KeyStroke.getKeyStroke(code, modifier);
			this.display.setText(key);
		}
	}

	public KeyStroke getKeyStroke()
	{
		return this.newkey;
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	public static KeyStroke getKeyStroke(JComponent parent)
	{
		final KeyboardMapper mapper = new KeyboardMapper();
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				mapper.grabFocus();
			}
		});

		String[] options = new String[] {
			ResourceMgr.getPlainString("LblOK"),
			ResourceMgr.getPlainString("LblCancel")
		};

		JOptionPane overwritePane = new JOptionPane(mapper, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options);
		JDialog dialog = overwritePane.createDialog(parent, ResourceMgr.getString("LblEnterKeyWindowTitle"));

		dialog.setResizable(true);
		dialog.setVisible(true);
		Object result = overwritePane.getValue();
		dialog.dispose();

		KeyStroke key = null;
		if (options[0].equals(result))
		{
			key = mapper.getKeyStroke();
		}
		return key;
	}

}
