/*
 * WbSwingUtilities.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import workbench.WbManager;
import workbench.db.WbConnection;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.ValidatingDialog;
import workbench.interfaces.SimplePropertyEditor;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * Some helper functions to deal with Swing stuff
 *
 * @author support@sql-workbench.net
 */

public class WbSwingUtilities
{
	public static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);
	public static final LineBorder FOCUSED_CELL_BORDER = new LineBorder(Color.YELLOW);
	public static final Border EMPTY_BORDER = new EmptyBorder(0, 0, 0, 0);
	public static final Border FLAT_BUTTON_BORDER = new CompoundBorder(new EtchedBorder(), new EmptyBorder(1, 6, 1, 6));
	public static final KeyStroke CTRL_TAB = KeyStroke.getKeyStroke("control TAB");
	public static final KeyStroke TAB = KeyStroke.getKeyStroke("TAB");
	public static final KeyStroke ENTER = KeyStroke.getKeyStroke("ENTER");
	public static final KeyStroke CTRL_ENTER = KeyStroke.getKeyStroke("control ENTER");
	public static final KeyStroke ALT_ENTER = KeyStroke.getKeyStroke("alt ENTER");

	public static Border getBevelBorder()
	{
		return createBevelBorder(BevelBorder.LOWERED);
	}

	public static Border getBevelBorderRaised()
	{
		return createBevelBorder(BevelBorder.RAISED);
	}

	private static final Border createBevelBorder(int type)
	{
		BevelBorder b = new BevelBorder(type);

		Color c = Color.LIGHT_GRAY;
		return new BevelBorder(type,
			b.getHighlightOuterColor(),
			c,
			b.getHighlightInnerColor(),
			b.getShadowInnerColor());
	}

	public static final void waitForEmptyQueue()
	{
		if (EventQueue.isDispatchThread())
		{
			return;
		}
		EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		int counter = 0;
		while (queue.peekEvent() != null)
		{
			WbThread.sleepSilently(25);
			counter++;
			if (counter > 20)
			{
				break;
			}
		}
	}

	public static final void setLabel(final JLabel label, final String text, final String tooltip)
	{
		invoke(new Runnable()
		{
			public void run()
			{
				label.setText(text);
				label.setToolTipText(tooltip);
				callRepaint(label);
			}
		});
	}

	/**
	 * Synchronously execute code on the EDT.
	 * If the current thread is the EDT, this merely calls r.run()
	 * otherwise EventQueue.invokeAndWait() is called with the passed runnable.
	 *
	 * Exceptions that can be thrown by EventQueue.invokeAndWait() are
	 * caught and logged.
	 */
	public static final void invoke(Runnable r)
	{
		if (EventQueue.isDispatchThread())
		{
			r.run();
		}
		else
		{
			try
			{
				EventQueue.invokeAndWait(r);
			}
			catch (Exception e)
			{
				LogMgr.logError("WbSwingUtilities.invoke()", "Error executing on EventQueue", e);
			}
		}
	}

	/**
	 * Centers the given window against another one on the screen.
	 * If aReference is not null, the first window is centered relative to the
	 * reference. If aReference is null, the window is centered on screen.
	 *
	 * @param aWinToCenter the window to be centered
	 * @param aReference	center against this window. If null -> center on screen
	 */
	public static void center(Window aWinToCenter, Component aReference)
	{
		Point location = getLocationToCenter(aWinToCenter, aReference);
		aWinToCenter.setLocation(location);
	}

	public static Window getWindowAncestor(Component caller)
	{
		if (caller instanceof Window)
		{
			return (Window) caller;
		}
		return SwingUtilities.getWindowAncestor(caller);
	}

	public static Point getLocationToCenter(Window aWinToCenter, Component aReference)
	{
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		boolean centerOnScreen = false;

		int referenceWidth,  referenceHeight;
		if (aReference == null)
		{
			referenceWidth = (int) screen.getWidth();
			referenceHeight = (int) screen.getHeight();
			centerOnScreen = true;
		}
		else
		{
			referenceWidth = aReference.getWidth();
			referenceHeight = aReference.getHeight();
		}
		int winWidth,  winHeight;
		if (aWinToCenter == null)
		{
			winWidth = 0;
			winHeight = 0;
		}
		else
		{
			winWidth = aWinToCenter.getWidth();
			winHeight = aWinToCenter.getHeight();
			if (winWidth > referenceWidth || winHeight > referenceHeight)
			{
				referenceWidth = (int) screen.getWidth();
				referenceHeight = (int) screen.getHeight();
				centerOnScreen = true;
			}
		}

		int x = 1,  y = 1;

		// Get center points
		if (referenceWidth > winWidth)
		{
			x = ((referenceWidth / 2) - (winWidth / 2));
		}
		if (referenceHeight > winHeight)
		{
			y = ((referenceHeight / 2) - (winHeight / 2));
		}

		if (aReference != null && aReference.isVisible() && !centerOnScreen)
		{
			try
			{
				Point p = aReference.getLocationOnScreen();
				x += p.getX();
				y += p.getY();
			}
			catch (Exception e)
			{
				LogMgr.logWarning("WbSwingUtilities.getLocationToCenter()", "Error getting parent location!", e);
			}
		}

		return new Point(x, y);
	}

	public static void showWaitCursorOnWindow(Component caller)
	{
		showCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR), caller, true, false);
	}

	public static void showDefaultCursorOnWindow(Component caller)
	{
		showDefaultCursor(caller, true);
	}

	public static void showWaitCursor(final Component caller)
	{
		showCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR), caller, false, true);
	}

	public static void showDefaultCursor(final Component caller)
	{
		showDefaultCursor(caller, false);
	}

	public static void showDefaultCursor(final Component caller, final boolean includeParents)
	{
		showCursor(null, caller, includeParents, true);
	}

	private static void showCursor(final Cursor cursor, final Component caller, final boolean includeParent, boolean immediate)
	{
		if (caller == null) return;

		Runnable r = new Runnable()
		{
			public void run()
			{
				caller.setCursor(cursor);
				if (includeParent)
				{
					final Window w = SwingUtilities.getWindowAncestor(caller);
					if (w != null)
					{
						w.setCursor(cursor);
					}
				}
			}
		};

		if (EventQueue.isDispatchThread())
		{
			r.run();
		}
		else
		{
			if (immediate)
			{
				try
				{
					EventQueue.invokeAndWait(r);
				}
				catch (Throwable th)
				{
				}
			}
			else
			{
				EventQueue.invokeLater(r);
			}
		}
	}

	public static void showErrorMessageKey(Component aCaller, String key)
	{
		showErrorMessage(aCaller, ResourceMgr.TXT_PRODUCT_NAME, ResourceMgr.getString(key));
	}

	public static void showErrorMessage(String aMessage)
	{
		showErrorMessage(null, ResourceMgr.TXT_PRODUCT_NAME, aMessage);
	}

	public static void showErrorMessage(Component aCaller, String aMessage)
	{
		showErrorMessage(aCaller, ResourceMgr.TXT_PRODUCT_NAME, aMessage);
	}

	public static void showErrorMessage(Component aCaller, final String title, final String message)
	{
		if (WbManager.getInstance().isBatchMode())
		{
			LogMgr.logError("showErrorMessage() - " + title, message, null);
			return;
		}

		final Component caller;

		if (aCaller == null)
		{
			caller = WbManager.getInstance().getCurrentWindow();
		}
		else if (!(aCaller instanceof Window))
		{
			caller = SwingUtilities.getWindowAncestor(aCaller);
		}
		else
		{
			caller = aCaller;
		}

		JOptionPane.showMessageDialog(caller, message, title, JOptionPane.ERROR_MESSAGE);
	}

	public static void showMessage(final Component aCaller, final Object aMessage)
	{
		invoke(new Runnable()
		{
			public void run()
			{
				JOptionPane.showMessageDialog(aCaller, aMessage, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.INFORMATION_MESSAGE);
			}
		});
	}

	public static void showMessage(final Component aCaller, final String title, final Object aMessage)
	{
		invoke(new Runnable()
		{
			public void run()
			{
				JOptionPane.showMessageDialog(aCaller, aMessage, title, JOptionPane.PLAIN_MESSAGE);
			}
		});
	}

	public static void showMessageKey(final Component aCaller, final String aKey)
	{
		invoke(new Runnable()
		{
			public void run()
			{
				JOptionPane.showMessageDialog(aCaller, ResourceMgr.getString(aKey), ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.INFORMATION_MESSAGE);
			}
		});
	}

	public static boolean getYesNo(Component aCaller, String aMessage)
	{
		int result = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(aCaller), aMessage, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		return (result == JOptionPane.YES_OPTION);
	}

	/**
	 * Present a Yes/No/Cancel message to the user.
	 *
	 * @param aCaller the calling component
	 * @param aMessage the message to display.
	 * @return JOptionPane.YES_OPTION  or JOptionPane.NO_OPTION or JOptionPane.CANCEL_OPTION
	 */
	public static int getYesNoCancel(Component aCaller, String aMessage)
	{
		int result = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(aCaller), aMessage, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		return result;
	}
	public static final int IGNORE_ALL = JOptionPane.YES_OPTION + JOptionPane.NO_OPTION + JOptionPane.CANCEL_OPTION + 1;
	public static final int EXECUTE_ALL = JOptionPane.YES_OPTION + JOptionPane.NO_OPTION + JOptionPane.CANCEL_OPTION + 2;

	public static boolean getProceedCancel(Component aCaller, String resourceKey, Object ... params)
	{
		String[] options = new String[]
		{
			ResourceMgr.getPlainString("LblProceed"), ResourceMgr.getPlainString("LblCancel")
		};


		String msg = ResourceMgr.getFormattedString(resourceKey, params);
		final JOptionPane ignorePane = new JOptionPane(msg, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
		final JDialog dialog = ignorePane.createDialog(SwingUtilities.getWindowAncestor(aCaller), ResourceMgr.TXT_PRODUCT_NAME);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		try
		{
			invoke(new Runnable()
			{
				public void run()
				{
					dialog.setResizable(false);
					dialog.pack();
					dialog.setVisible(true);
				}
			});
		}
		finally
		{
			dialog.dispose();
		}
		Object result = ignorePane.getValue();
		if (result == null) return false;
		return result.equals(options[0]);
	}

	public static int getYesNoIgnoreAll(Component aCaller, String aMessage)
	{
		String[] options = new String[]
		{
			ResourceMgr.getString("LblYes"), ResourceMgr.getString("LblNo"), ResourceMgr.getString("LblIgnoreAll")
		};
		JOptionPane ignorePane = new JOptionPane(aMessage, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
		JDialog dialog = ignorePane.createDialog(SwingUtilities.getWindowAncestor(aCaller), ResourceMgr.TXT_PRODUCT_NAME);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		int rvalue = -1;
		try
		{
			dialog.setResizable(false);
			dialog.pack();
			dialog.setVisible(true);
			Object result = ignorePane.getValue();
			if (result == null)
			{
				rvalue = JOptionPane.YES_OPTION;
			}
			else if (result.equals(options[0]))
			{
				rvalue = JOptionPane.YES_OPTION;
			}
			else if (result.equals(options[1]))
			{
				rvalue = JOptionPane.NO_OPTION;
			}
			else if (result.equals(options[2]))
			{
				rvalue = IGNORE_ALL;
			}
			else
			{
				rvalue = JOptionPane.NO_OPTION;
			}
		}
		finally
		{
			dialog.dispose();
		}
		return rvalue;
	}

	public static int getYesNoExecuteAll(Component aCaller, String aMessage)
	{
		String[] options = new String[]
		{
			ResourceMgr.getPlainString("LblYes"),
			ResourceMgr.getPlainString("LblExecuteAll"),
			ResourceMgr.getPlainString("LblNo"),
			ResourceMgr.getPlainString("LblCancel")
		};
		JOptionPane ignorePane = new JOptionPane(aMessage, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
		JDialog dialog = ignorePane.createDialog(aCaller, ResourceMgr.TXT_PRODUCT_NAME);
		try
		{
			dialog.setResizable(true);
			dialog.pack();
			dialog.setVisible(true);
			Object result = ignorePane.getValue();
			if (result == null)
			{
				return JOptionPane.YES_OPTION;
			}
			else if (result.equals(options[0]))
			{
				return JOptionPane.YES_OPTION;
			}
			else if (result.equals(options[1]))
			{
				return EXECUTE_ALL;
			}
			else if (result.equals(options[2]))
			{
				return JOptionPane.NO_OPTION;
			}
			else if (result.equals(options[3]))
			{
				return JOptionPane.CANCEL_OPTION;
			}
			else
			{
				return JOptionPane.NO_OPTION;
			}
		}
		finally
		{
			dialog.dispose();
		}
	}

	public static int getYesNo(Component aCaller, String aMessage, String[] options)
	{
		return getYesNo(aCaller, aMessage, options, JOptionPane.PLAIN_MESSAGE);
	}

	public static int getYesNo(Component aCaller, String aMessage, String[] options, int type)
	{
		JOptionPane ignorePane = new JOptionPane(aMessage, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null, options, options[1]);
		ignorePane.setMessageType(type);
		JDialog dialog = ignorePane.createDialog(aCaller, ResourceMgr.TXT_PRODUCT_NAME);
		try
		{
			dialog.setResizable(true);
			dialog.pack();
			dialog.setVisible(true);
			Object result = ignorePane.getValue();
			if (result == null)
			{
				return JOptionPane.YES_OPTION;
			}
			else if (result.equals(options[0]))
			{
				return 0;
			}
			else if (result.equals(options[1]))
			{
				return 1;
			}
			else
			{
				return -1;
			}
		}
		finally
		{
			dialog.dispose();
		}
	}

	public static boolean getOKCancel(String title, Component aCaller, Component message)
	{
		return getOKCancel(title, aCaller, message, null);
	}

	public static boolean getOKCancel(String title, Component aCaller, Component message, final Runnable doLater)
	{
		JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = pane.createDialog(aCaller, title);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		if (doLater != null)
		{
			WindowAdapter w = new WindowAdapter()
			{
				public void windowOpened(WindowEvent evt)
				{
					EventQueue.invokeLater(doLater);
				}
			};
			dialog.addWindowListener(w);
		}

		dialog.setResizable(false);
		dialog.pack();
		dialog.setVisible(true);
		dialog.dispose();
		Object value = pane.getValue();
		if (value == null)
		{
			return false;
		}
		if (value instanceof Number)
		{
			int choice = ((Number) value).intValue();
			return choice == 0;
		}
		return false;
	}

	public enum TransactionEnd
	{
		Commit,
		Rollback
	}
	
	public static TransactionEnd getCommitRollbackQuestion(Component aCaller, String aMessage)
	{
		String[] options = new String[]
		{
			ResourceMgr.getString("LblCommit"), ResourceMgr.getString("LblRollback")
		};
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
		try
		{
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setSize(w + 130, dialog.getHeight());
			dialog.setResizable(true);
			dialog.setVisible(true);
			Object result = ignorePane.getValue();
			if (result != null && result.equals(options[0]))
			{
				return TransactionEnd.Commit;
			}
			else
			{
				return TransactionEnd.Rollback;
			}
		}
		finally
		{
			dialog.dispose();
		}
	}

	public static String getUserInput(Component caller, String aTitle, String initialValue)
	{
		return getUserInput(caller, aTitle, initialValue, false);
	}

	public static String getUserInput(Component caller, String aTitle, String initialValue, boolean hideInput)
	{
		Window parent = SwingUtilities.getWindowAncestor(caller);

		final JTextField input;
		if (hideInput)
		{
			input = new JPasswordField();
		}
		else
		{
			input = new JTextField();
		}
		input.setColumns(40);
		input.setText(initialValue);
		if (initialValue != null)
		{
			input.selectAll();
		}
		input.addMouseListener(new TextComponentMouseListener());

		boolean ok = ValidatingDialog.showConfirmDialog(parent, input, aTitle);
		if (!ok)
		{
			return null;
		}
		String value = input.getText();
		return value;
	}

	public static String getKeyName(int keyCode)
	{
		if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9 || keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z)
		{
			return String.valueOf((char) keyCode);
		}

		// Check for other ASCII keyCodes.
		int index = ",./;=[\\]".indexOf(keyCode);
		if (index >= 0)
		{
			return String.valueOf((char) keyCode);
		}

		switch (keyCode)
		{
			case KeyEvent.VK_ENTER:
				return "VK_ENTER";
			case KeyEvent.VK_BACK_SPACE:
				return "VK_BACK_SPACE";
			case KeyEvent.VK_TAB:
				return "VK_TAB";
			case KeyEvent.VK_CANCEL:
				return "VK_CANCEL";
			case KeyEvent.VK_CLEAR:
				return "VK_CLEAR";
			case KeyEvent.VK_SHIFT:
				return "VK_SHIFT";
			case KeyEvent.VK_CONTROL:
				return "VK_CONTROL";
			case KeyEvent.VK_ALT:
				return "VK_ALT";
			case KeyEvent.VK_PAUSE:
				return "VK_PAUSE";
			case KeyEvent.VK_CAPS_LOCK:
				return "VK_CAPS_LOCK";
			case KeyEvent.VK_ESCAPE:
				return "VK_ESCAPE";
			case KeyEvent.VK_SPACE:
				return "VK_SPACE";
			case KeyEvent.VK_PAGE_UP:
				return "VK_PAGE_UP";
			case KeyEvent.VK_PAGE_DOWN:
				return "VK_PAGE_DOWN";
			case KeyEvent.VK_END:
				return "VK_END";
			case KeyEvent.VK_HOME:
				return "VK_HOME";

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
			case KeyEvent.VK_F1:
				return "F1";
			case KeyEvent.VK_F2:
				return "F2";
			case KeyEvent.VK_F3:
				return "F3";
			case KeyEvent.VK_F4:
				return "F4";
			case KeyEvent.VK_F5:
				return "F5";
			case KeyEvent.VK_F6:
				return "F6";
			case KeyEvent.VK_F7:
				return "F7";
			case KeyEvent.VK_F8:
				return "F8";
			case KeyEvent.VK_F9:
				return "F9";
			case KeyEvent.VK_F10:
				return "F10";
			case KeyEvent.VK_F11:
				return "F11";
			case KeyEvent.VK_F12:
				return "F12";
			case KeyEvent.VK_F13:
				return "F13";
			case KeyEvent.VK_F14:
				return "F14";
			case KeyEvent.VK_F15:
				return "F15";
			case KeyEvent.VK_F16:
				return "F16";
			case KeyEvent.VK_F17:
				return "F17";
			case KeyEvent.VK_F18:
				return "F18";
			case KeyEvent.VK_F19:
				return "F19";
			case KeyEvent.VK_F20:
				return "F20";
			case KeyEvent.VK_F21:
				return "F21";
			case KeyEvent.VK_F22:
				return "F22";
			case KeyEvent.VK_F23:
				return "F23";
			case KeyEvent.VK_F24:
				return "F24";
		}
		if (keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9)
		{
			char c = (char) (keyCode - KeyEvent.VK_NUMPAD0 + '0');
			return "NumPad-" + c;
		}
		return "KeyCode: 0x" + Integer.toString(keyCode, 16);
	}

	public static void callRepaint(final Component c)
	{
		c.validate();
		c.repaint();
	}

	public static void repaintNow(final Component c)
	{
		invoke(new Runnable()
		{
			public void run()
			{
				callRepaint(c);
			}
		});
	}

	public static void repaintLater(final Component c)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				callRepaint(c);
			}
		});
	}

	public static void requestFocus(final Window win, final JComponent comp)
	{
		win.addWindowListener(new WindowAdapter()
		{
			public void windowActivated(WindowEvent evt)
			{
				EventQueue.invokeLater(new Runnable()
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

	public static void requestFocus(final JComponent comp)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				comp.requestFocus();
			}
		});
	}

	public static void initPropertyEditors(Object bean, JComponent root)
	{
		for (int i = 0; i < root.getComponentCount(); i++)
		{
			Component c = root.getComponent(i);
			if (c instanceof SimplePropertyEditor)
			{
				SimplePropertyEditor editor = (SimplePropertyEditor) c;
				String property = c.getName();
				if (!StringUtil.isEmptyString(property))
				{
					editor.setSourceObject(bean, property);
					editor.setImmediateUpdate(true);
				}
			}
			else if (c instanceof JComponent)
			{
				initPropertyEditors(bean, (JComponent) c);
			}
		}
	}

	public static boolean checkConnection(Component parent, WbConnection dbConnection)
	{
		if (dbConnection.isBusy())
		{
			showMessageKey(parent, "ErrConnectionBusy");
			return false;
		}
		return true;
	}

	public static void dumpActionMap(JComponent comp)
	{
		System.out.println("InputMap for WHEN_ANCESTOR_OF_FOCUSED_COMPONENT");
		System.out.println("-----------------------------------------------");
		dumpInputMap(comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));

		System.out.println("\nInputMap for WHEN_FOCUSED");
		System.out.println("-------------------------");
		dumpInputMap(comp.getInputMap(JComponent.WHEN_FOCUSED));

		System.out.println("\nInputMap for WHEN_IN_FOCUSED_WINDOW");
		System.out.println("-----------------------------------");
		dumpInputMap(comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW));
	}

	private static void dumpInputMap(InputMap im)
	{
		if (im != null && im.allKeys() != null)
		{
			for (KeyStroke key : im.allKeys())
			{
				if (key == null) continue;
				Object o = im.get(key);
				System.out.println("key: " + key + " mapped to " + o.toString());
			}
		}
		else
		{
			System.out.println("Nothing mapped");
		}
	}
}
