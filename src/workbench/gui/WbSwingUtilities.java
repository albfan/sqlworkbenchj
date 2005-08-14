/*
 * WbSwingUtilities.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import workbench.gui.components.TextComponentMouseListener;
import workbench.resource.ResourceMgr;


public class WbSwingUtilities
{
	public static final Border BEVEL_BORDER;
	static
	{
		BevelBorder b = new BevelBorder(BevelBorder.LOWERED);
		Color c = Color.LIGHT_GRAY;
		//c.darker();
		BEVEL_BORDER = new BevelBorder(BevelBorder.LOWERED,
					b.getHighlightOuterColor(),
					c,//b.getShadowOuterColor(),
					b.getHighlightInnerColor(),
					b.getShadowInnerColor());
	}

	public static final Border BEVEL_BORDER_RAISED;
	static
	{
		BevelBorder b = new BevelBorder(BevelBorder.RAISED);
		Color c = Color.LIGHT_GRAY;
		//c.darker();
		BEVEL_BORDER_RAISED = new BevelBorder(BevelBorder.RAISED,
					b.getHighlightOuterColor(),
					c,//b.getShadowOuterColor(),
					b.getHighlightInnerColor(),
					b.getShadowInnerColor());
	}

	public static final Border EMPTY_BORDER = new EmptyBorder(0,0,0,0);

	private WbSwingUtilities()
	{
	}

	/**
	 *	Centers the given window either agains anotherone on the screen
	 *	If a second window is passed the first window is centered
	 *	against that one
	 *
	 *	@param 	aWinToCenter the window to be centered
	 *	@param	aReference	center against this window. If null -> center on screen
	 */
	public static void center(Window aWinToCenter, Window aReference)
	{
		if (aWinToCenter == null) return;
		Point location = getLocationToCenter(aWinToCenter, aReference);
		aWinToCenter.setLocation(location);
	}

	public static Point getLocationToCenter(Window aWinToCenter, Window aReference)
	{
		int screenWidth, screenHeight;
		if (aReference == null)
		{
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			screenWidth = (int)screen.getWidth();
			screenHeight = (int)screen.getHeight();
		}
		else
		{
			screenWidth = aReference.getWidth();
			screenHeight = aReference.getHeight();
		}
		int winWidth, winHeight;
		if (aWinToCenter == null)
		{
			winWidth = 0;
			winHeight = 0;
		}
		else
		{
			winWidth = aWinToCenter.getWidth();
			winHeight = aWinToCenter.getHeight();
		}

		int x = 1, y = 1;

		// Get center points
		if (screenWidth > winWidth)
		{
			x = (int)((screenWidth / 2) - (winWidth / 2));
		}
		if (screenHeight > winHeight)
		{
			y = (int)((screenHeight/ 2) - (winHeight / 2));
		}

		if (aReference != null)
		{
			x += aReference.getX();
			y += aReference.getY();
		}

		return new Point(x, y);
	}

	public static void showWaitCursorOnWindow(Component caller)
	{
		showCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR), caller, true);
	}

	public static void showDefaultCursorOnWindow(Component caller)
	{
		showDefaultCursor(caller, true);
	}

	public static void showWaitCursor(final Component caller)
	{
		caller.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	public static void showDefaultCursor(final Component caller)
	{
		showDefaultCursor(caller, false);
	}

	public static void showDefaultCursor(final Component caller, final boolean includeParents)
	{
		showCursor(null, caller, includeParents);
	}

	private static void showCursor(final Cursor cursor, final Component caller, final boolean includeParents)
	{
		if (caller == null) return;
		Runnable r = new Runnable()
		{
			public void run()
			{
				caller.setCursor(cursor);
				if (includeParents)
				{
					final Window w = SwingUtilities.getWindowAncestor(caller);
					if (w != null) w.setCursor(cursor);
				}
			}
		};
		
		if (EventQueue.isDispatchThread())
		{
			r.run();
		}
		else
		{
			//try { EventQueue.invokeAndWait(r); } catch (Throwable ignore) {} 
			EventQueue.invokeLater(r);
		}
	}

	public static void showErrorMessage(Component aCaller, String aMessage)
	{
		JOptionPane.showMessageDialog(aCaller, aMessage, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.ERROR_MESSAGE);
	}

	public static void showMessage(Component aCaller, String aMessage)
	{
		JOptionPane.showMessageDialog(aCaller, aMessage, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.INFORMATION_MESSAGE);
	}

	public static void showMessageKey(Component aCaller, String aKey)
	{
		JOptionPane.showMessageDialog(aCaller, ResourceMgr.getString(aKey), ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.INFORMATION_MESSAGE);
	}

	public static boolean getYesNo(Component aCaller, String aMessage)
	{
		int result = JOptionPane.showConfirmDialog(aCaller, aMessage, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		return (result == JOptionPane.YES_OPTION);
	}

	public static int getYesNoCancel(Component aCaller, String aMessage)
	{
		int result = JOptionPane.showConfirmDialog(aCaller, aMessage, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		return result;
	}

	public static final int IGNORE_ALL = JOptionPane.YES_OPTION + JOptionPane.NO_OPTION + JOptionPane.CANCEL_OPTION + 1;
	public static final int EXECUTE_ALL = JOptionPane.YES_OPTION + JOptionPane.NO_OPTION + JOptionPane.CANCEL_OPTION + 2;

	public static int getYesNoIgnoreAll(Component aCaller, String aMessage)
	{
		String[] options = new String[] { ResourceMgr.getString("LabelYes"), ResourceMgr.getString("LabelNo"), ResourceMgr.getString("LabelIgnoreAll")};
		JOptionPane ignorePane = new JOptionPane(aMessage, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
		JDialog dialog = ignorePane.createDialog(aCaller, ResourceMgr.TXT_PRODUCT_NAME);
		dialog.setResizable(true);
		dialog.pack();
		dialog.show();
		dialog.dispose();
		Object result = ignorePane.getValue();
		if (result == null) return JOptionPane.YES_OPTION;
		else if (result.equals(options[0])) return JOptionPane.YES_OPTION;
		else if (result.equals(options[1])) return JOptionPane.NO_OPTION;
		else if (result.equals(options[2])) return IGNORE_ALL;
		else return JOptionPane.NO_OPTION;
	}

	public static int getYesNoExecuteAll(Component aCaller, String aMessage)
	{
		String[] options = new String[] { ResourceMgr.getString("LabelYes"), ResourceMgr.getString("LabelNo"), ResourceMgr.getString("LabelExecuteAll")};
		JOptionPane ignorePane = new JOptionPane(aMessage, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
		JDialog dialog = ignorePane.createDialog(aCaller, ResourceMgr.TXT_PRODUCT_NAME);
		dialog.setResizable(true);
		dialog.pack();
		dialog.show();
		dialog.dispose();
		Object result = ignorePane.getValue();
		if (result == null) return JOptionPane.YES_OPTION;
		else if (result.equals(options[0])) return JOptionPane.YES_OPTION;
		else if (result.equals(options[1])) return JOptionPane.NO_OPTION;
		else if (result.equals(options[2])) return EXECUTE_ALL;
		else return JOptionPane.NO_OPTION;
	}

	public static int getYesNo(Component aCaller, String aMessage, String[] options)
	{
		JOptionPane ignorePane = new JOptionPane(aMessage, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null, options);
		JDialog dialog = ignorePane.createDialog(aCaller, ResourceMgr.TXT_PRODUCT_NAME);
		dialog.setResizable(true);
		dialog.pack();
		dialog.show();
		dialog.dispose();
		Object result = ignorePane.getValue();
		if (result == null) return JOptionPane.YES_OPTION;
		else if (result.equals(options[0])) return 0;
		else if (result.equals(options[1])) return 1;
		else return -1;
	}

	public static boolean getOKCancel(String title, Component aCaller, Component message)
	{
		JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = pane.createDialog(aCaller, title);
		dialog.setResizable(true);
		dialog.pack();
		dialog.show();
		dialog.dispose();
		Object value = pane.getValue();
		if (value == null) return false;
		if (value instanceof Number)
		{
			int choice = ((Number)value).intValue();
			return choice == 0;
		}
		return false;
	}
	
	public static final int DO_COMMIT = 0;
	public static final int DO_ROLLBACK = 1;

	public static int getCommitRollbackQuestion(Component aCaller, String aMessage)
	{
		String[] options = new String[] { ResourceMgr.getString("LabelCommit"), ResourceMgr.getString("LabelRollback")};
		JOptionPane ignorePane = new JOptionPane(aMessage, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null, options);
		int w = 0;
		try
		{
			Font f = ignorePane.getFont();
			FontMetrics fm = ignorePane.getFontMetrics(f);
			w = fm.stringWidth(aMessage);
		}
		catch (Throwable th)
		{
			th.printStackTrace();
			w = 300;
		}
		JDialog dialog = ignorePane.createDialog(aCaller, ResourceMgr.TXT_PRODUCT_NAME);
		dialog.setSize(w + 130, dialog.getHeight());
		dialog.setResizable(true);
		dialog.show();
		dialog.dispose();
		Object result = ignorePane.getValue();
		if (result == null) return DO_ROLLBACK;
		else if (result.equals(options[0])) return DO_COMMIT;
		else if (result.equals(options[1])) return DO_ROLLBACK;
		else return DO_ROLLBACK;
	}

	public static String getUserInput(Component caller, String aTitle, String initialValue)
	{
		return getUserInput(caller, aTitle, initialValue, false);
	}
	
	public static String getUserInput(Component caller, String aTitle, String initialValue, boolean hideInput)
	{
		Component parent = SwingUtilities.getWindowAncestor(caller);

		final JTextField input;
		if (hideInput)
			input = new JPasswordField();
		else
		 input = new JTextField();

		input.setColumns(40);
		input.setText(initialValue);
		if (initialValue != null)
		{
			input.selectAll();
		}
		input.addMouseListener(new TextComponentMouseListener());
		EventQueue.invokeLater(new Runnable() {
			public void run()
			{
				input.grabFocus();
			}
		});
		int choice = JOptionPane.showConfirmDialog(parent, input, aTitle, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice == JOptionPane.CANCEL_OPTION) return null;
		String value = input.getText();
		return value;
	}

	public static String getKeyName(int keyCode)
	{
		if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9 || keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) 
		{
			return String.valueOf((char)keyCode);
		}
		
		// Check for other ASCII keyCodes.
		int index = ",./;=[\\]".indexOf(keyCode);
		if (index >= 0)
		{
			return String.valueOf((char)keyCode);
		}
		
		switch(keyCode)
		{
			case KeyEvent.VK_ENTER: return "VK_ENTER";
			case KeyEvent.VK_BACK_SPACE: return "VK_BACK_SPACE";
			case KeyEvent.VK_TAB: return "VK_TAB";
			case KeyEvent.VK_CANCEL: return "VK_CANCEL";
			case KeyEvent.VK_CLEAR: return "VK_CLEAR";
			case KeyEvent.VK_SHIFT: return "VK_SHIFT";
			case KeyEvent.VK_CONTROL: return "VK_CONTROL";
			case KeyEvent.VK_ALT: return "VK_ALT";
			case KeyEvent.VK_PAUSE: return "VK_PAUSE";
			case KeyEvent.VK_CAPS_LOCK: return "VK_CAPS_LOCK";
			case KeyEvent.VK_ESCAPE: return "VK_ESCAPE";
			case KeyEvent.VK_SPACE: return "VK_SPACE";
			case KeyEvent.VK_PAGE_UP: return "VK_PAGE_UP";
			case KeyEvent.VK_PAGE_DOWN: return "VK_PAGE_UP";
			case KeyEvent.VK_END: return "VK_END";
			case KeyEvent.VK_HOME: return "VK_HOME";
			
			case KeyEvent.VK_LEFT: 
			case KeyEvent.VK_KP_LEFT:
				return "VK_LEFT";
				
			case KeyEvent.VK_UP: 
			case KeyEvent.VK_KP_UP:
				return "VK_UP";
				
			case KeyEvent.VK_RIGHT: 
			case KeyEvent.VK_KP_RIGHT: 
				return "VK_RIGHT";
			
			case KeyEvent.VK_DOWN: 
			case KeyEvent.VK_KP_DOWN:
				return "VK_DOWN";
			case KeyEvent.VK_F1: return "F1";
			case KeyEvent.VK_F2: return "F2";
			case KeyEvent.VK_F3: return "F3";
			case KeyEvent.VK_F4: return "F4";
			case KeyEvent.VK_F5: return "F5";
			case KeyEvent.VK_F6: return "F6";
			case KeyEvent.VK_F7: return "F7";
			case KeyEvent.VK_F8: return "F8";
			case KeyEvent.VK_F9: return "F9";
			case KeyEvent.VK_F10: return "F10";
			case KeyEvent.VK_F11: return "F11";
			case KeyEvent.VK_F12: return "F12";
			case KeyEvent.VK_F13: return "F13";
			case KeyEvent.VK_F14: return "F14";
			case KeyEvent.VK_F15: return "F15";
			case KeyEvent.VK_F16: return "F16";
			case KeyEvent.VK_F17: return "F17";
			case KeyEvent.VK_F18: return "F18";
			case KeyEvent.VK_F19: return "F19";
			case KeyEvent.VK_F20: return "F20";
			case KeyEvent.VK_F21: return "F21";
			case KeyEvent.VK_F22: return "F22";
			case KeyEvent.VK_F23: return "F23";
			case KeyEvent.VK_F24: return "F24";
		}
		if (keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9)
		{
			String numpad = "NumPad";
			char c = (char)(keyCode - KeyEvent.VK_NUMPAD0 + '0');
			return numpad + "-" + c;
		}
		return "KeyCode: 0x" + Integer.toString(keyCode, 16);
	}
	
	public static void requestFocus(final Window win, final Component comp)
	{
		win.addWindowListener(new WindowAdapter()
		{
			public void windowActivated(WindowEvent evt)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						comp.requestFocus();
					}
				});
				win.removeWindowListener(this);
			}
		});
	} 
	
}
