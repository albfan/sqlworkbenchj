/*
 * KeyboardMapper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 *
 * @author  info@sql-workbench.net
 */
public class KeyboardMapper
	extends JPanel
	implements KeyListener
{
	private JTextField display;
	private KeyStroke newkey;
	
	public KeyboardMapper()
	{
		this.display = new JTextField(20);
		this.display.addKeyListener(this);
		this.display.setEditable(false);
		this.display.setDisabledTextColor(Color.BLACK);
		this.display.setBackground(Color.WHITE);
		this.add(display);
	}
	
	public static void main(String args[])
	{
		try
		{
			JFrame w = new JFrame("Testing");
			w.getContentPane().setLayout(new BorderLayout());
			w.getContentPane().add(new KeyboardMapper(), BorderLayout.CENTER);
			w.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt)
				{
					System.exit(0);
				}
			});
			w.pack();
			w.show();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void grabFocus()
	{
		this.display.grabFocus();
		this.display.requestFocusInWindow();
	}
	public void keyPressed(KeyEvent e)
	{
	}
	
	public void keyReleased(KeyEvent e)
	{
		int modifier = e.getModifiers();
		int code = e.getKeyCode();

		// only allow function keys without modifier!
		if (modifier == 0) 
		{
			if (code < KeyEvent.VK_F1 || code > KeyEvent.VK_F12) return;
		}
		
		// keyReleased is also called when the Ctrl or Shift keys are release
		// in that case the keycode is 0 --> ignore it
		if (code > 32)
		{
			String key = "";
			if (modifier > 0) key = KeyEvent.getKeyModifiersText(modifier) + "-";
			key = key + KeyEvent.getKeyText(code);
			this.newkey = KeyStroke.getKeyStroke(code, modifier);
			this.display.setText(key);
		}
	}

	public KeyStroke getKeyStroke()
	{
		return this.newkey;
	}
	
	public void keyTyped(KeyEvent e)
	{
		//int modifier = e.getModifiers();
		//int code = e.getKeyCode();
		//char c = e.getKeyChar();
		//System.out.println("c=" + (int)c);
		//System.out.println("code = " + code);
		//String key = KeyEvent.getKeyModifiersText(modifier) + "-" + c;
		//System.out.println("Typed " + e.toString());
		//this.display.setText(e.toString());
	}
	
}
