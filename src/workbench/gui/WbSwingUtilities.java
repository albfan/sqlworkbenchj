/*
 * WbSwingUtilities.java
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
package workbench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import workbench.WbManager;
import workbench.interfaces.SimplePropertyEditor;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.gui.components.PlainEditor;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbOptionPane;

import workbench.util.NumberStringCache;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 * Some helper functions to deal with Swing stuff
 *
 * @author Thomas Kellerer
 */

public class WbSwingUtilities
{
	public static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);
	public static final LineBorder FOCUSED_CELL_BORDER = new LineBorder(Color.YELLOW);
	public static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder();
	public static final Border FLAT_BUTTON_BORDER = new CompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(1, 6, 1, 6));
	public static final KeyStroke CTRL_TAB = KeyStroke.getKeyStroke("control TAB");
	public static final KeyStroke TAB = KeyStroke.getKeyStroke("TAB");
	public static final KeyStroke SHIFT_TAB = KeyStroke.getKeyStroke("shift TAB");
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

	private static Border createBevelBorder(int type)
	{
		BevelBorder b = new BevelBorder(type);
		return new BevelBorder(type, b.getHighlightOuterColor(), Color.LIGHT_GRAY, b.getHighlightInnerColor(), b.getShadowInnerColor());
	}

	public static boolean containsComponent(JComponent container, JComponent toCheck)
	{
		Component[] children = container.getComponents();
		if (children == null || children.length == 0) return false;
		for (Component c : children)
		{
			if (c == toCheck) return true;
		}
		return false;
	}

	public static void waitForEmptyQueue()
	{
		if (EventQueue.isDispatchThread())
		{
			return;
		}
		EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		int counter = 0;
		final int tries = 20;
		while (queue.peekEvent() != null)
		{
			WbThread.sleepSilently(25);
			counter++;
			if (counter > tries)
			{
				LogMgr.logDebug("WbSwingUtilities.waitForEmptyQueue()", "Queue still not empty after " + tries + " tries!");
				break;
			}
		}
	}

	public static void setLabel(final JLabel label, final String text, final String tooltip)
	{
		invoke(new Runnable()
		{
			@Override
			public void run()
			{
				label.setText(text);
				label.setToolTipText(tooltip);
				callRepaint(label);
			}
		});
	}

	/**
	 * A simple wrapper around EventQueue.invokeLater().
	 *
	 * @param runner the task to execute
	 */
	public static void invokeLater(Runnable runner)
	{
		EventQueue.invokeLater(runner);
	}

	/**
	 * Synchronously execute code on the EDT.
	 * If the current thread is the EDT, this merely calls r.run()
	 * otherwise EventQueue.invokeAndWait() is called with the passed runnable.
	 *
	 * Exceptions that can be thrown by EventQueue.invokeAndWait() are
	 * caught and logged.
	 */
	public static void invoke(Runnable r)
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

	public static String displayString(Dimension d)
	{
		if (d == null) return "";
		return "[w:" + (int)d.getWidth() + ",h:" + (int)d.getHeight() + "]";
	}

	public static String displayString(Rectangle d)
	{
		if (d == null) return "";
		return "[x: " + d.x + ", y: " + d.y + ", w:" + (int)d.getWidth() + ",h:" + (int)d.getHeight() + "]";
	}

	public static String displayString(int x, int y)
	{
		return "[x:" + x + ",y:" + y + "]";
	}

	public static boolean isOutsideOfScreen(Rectangle toDisplay)
	{
		return !isFullyVisible(toDisplay);
	}

  public static Rectangle getVirtualBounds()
  {
    Rectangle virtualBounds = new Rectangle();
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] devices = env.getScreenDevices();
    for (GraphicsDevice gd : devices)
    {
      GraphicsConfiguration[] configs = gd.getConfigurations();
      for (GraphicsConfiguration gc : configs)
      {
        virtualBounds = virtualBounds.union(gc.getBounds());
      }
    }
    return virtualBounds;
  }

	public static boolean isFullyVisible(int x, int y, Dimension size)
  {
    return isFullyVisible(new Rectangle(x,y,size.width, size.height));
  }

	public static boolean isFullyVisible(Point p, Dimension size)
  {
    return isFullyVisible(new Rectangle(p, size));
  }

	public static boolean isFullyVisible(Rectangle toDisplay)
	{
    Rectangle screen = getVirtualBounds();

    // make sure at least something of the window is visible
    return screen.intersects(toDisplay);
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
		if (!isFullyVisible(location, aWinToCenter.getSize()))
		{
			// centering against the reference would move the dialog outside
			// the current screen --> display the window centered on the current screen
			location = getLocationToCenter(aWinToCenter, null);
		}
		aWinToCenter.setLocation(location);
	}

	public static Window getWindowAncestor(Component caller)
	{
		if (caller == null) return null;
		if (caller instanceof Window)
		{
			return (Window) caller;
		}
		return SwingUtilities.getWindowAncestor(caller);
	}

  public static Insets getScreenOffset()
  {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsConfiguration conf = ge.getDefaultScreenDevice().getDefaultConfiguration();

    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(conf);
    return insets;
  }

  public static Dimension getScreenSize()
  {
    return getScreenSize(getScreenOffset());
  }

  public static Dimension getScreenSize(Insets insets)
  {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsConfiguration conf = ge.getDefaultScreenDevice().getDefaultConfiguration();

    screen.width -= (insets.left + insets.right);
    screen.height -= (insets.bottom + insets.top);
    return screen;
  }

	public static Point getLocationToCenter(Window aWinToCenter, Component aReference)
	{
    Insets offset = getScreenOffset();
		Dimension screen = getScreenSize(offset);

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

    if (centerOnScreen && offset != null)
    {
      x += offset.left;
      x += offset.top;
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

	/**
	 * Display an hourglass (wait) mouse cursor on the passed component and the window containing the component.
	 *
	 * @param caller  the component on which to display the wait cursor
	 */
	public static void showWaitCursorOnWindow(Component caller)
	{
		showCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR), caller, true, false);
	}

	public static void showDefaultCursorOnWindow(Component caller)
	{
		showDefaultCursor(caller, true);
	}

	/**
	 * Display an hourglass (wait) mouse cursor on the passed component.
	 *
	 * @param caller  the component on which to display the wait cursor
	 */
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
			@Override
			public void run()
			{
				caller.setCursor(cursor);
				if (includeParent)
				{
					final Window w = getWindowAncestor(caller);
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

	public static void showFriendlyErrorMessage(Component caller, final String error)
	{
		showFriendlyErrorMessage(caller, ResourceMgr.TXT_PRODUCT_NAME, error);
	}

	/**
	 * Displays an error message and checks for the length of the message.
	 * For messages that contain newlines or are longer than 100 characters,
	 * a multiline display is used.
	 */
	public static void showFriendlyErrorMessage(Component caller, final String title, final String error)
	{
		int maxLen = Settings.getInstance().getIntProperty("workbench.gui.message.maxlength", 100);
		if (error.indexOf('\n') > 0 || error.indexOf('\r') > 0 || error.length() > maxLen)
		{
			showMultiLineError(caller, title, error);
		}
		else
		{
			showErrorMessage(caller, title, error);
		}
	}

	public static void showErrorMessage(Component component, final String title, final String message)
	{
		if (WbManager.getInstance().isBatchMode())
		{
			LogMgr.logError("showErrorMessage() - " + title, message, null);
			return;
		}

		final Component caller;

		if (component == null)
		{
			caller = WbManager.getInstance().getCurrentWindow();
		}
		else
		{
			caller = getWindowAncestor(component);
		}

		invoke(new Runnable()
		{
			@Override
			public void run()
			{
				JOptionPane.showMessageDialog(caller, message, title, JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	public static void showMultiLineError(final Component caller, final String title, final String message)
	{
		if (WbManager.getInstance().isBatchMode())
		{
			LogMgr.logError("showMultiLineError()", message, null);
			return;
		}

		final Component realCaller;

		if (caller == null)
		{
			realCaller = WbManager.getInstance().getCurrentWindow();
		}
		else
		{
			realCaller = getWindowAncestor(caller);
		}

		invoke(new Runnable()
		{
			@Override
			public void run()
			{
        final JComponent pane = createErrorMessagePanel(message, true);
				JOptionPane.showMessageDialog(realCaller, pane, title, JOptionPane.ERROR_MESSAGE);
			}
		});
	}

  public static JComponent createErrorMessagePanel(String message, boolean allowWordWrap)
  {
    final PlainEditor msg = new PlainEditor(allowWordWrap);
    msg.removeBorders();
    msg.setText(message);
    msg.setEditable(false);
    msg.setFont(Settings.getInstance().getEditorFont());
    msg.setBorder(new EmptyBorder(0, 0, 0, 0));

    JScrollPane pane = new JScrollPane(msg)
    {
      @Override
      public Dimension getPreferredSize()
      {
        FontMetrics fm = msg.getFontMetrics(msg.getFont());
        int charSize = fm.getMaxAdvance();
        int lineHeight = fm.getHeight();
        int minLineCount = 6;
        int prefHeight = lineHeight * minLineCount;
        int lineCount = msg.getLineCount() + 2;
        if (lineCount > 0)
        {
          prefHeight = lineHeight * Math.min(lineCount, 15);
        }
        System.out.println("LineCount: " + lineCount);
        return new Dimension(charSize * 60, prefHeight);
      }
    };

    FontMetrics fm = msg.getFontMetrics(msg.getFont());
    int charSize = fm.getMaxAdvance();
    int lineHeight = fm.getHeight();
    int maxWidth = charSize * 80;
    int maxHeight = lineHeight * 15;

    pane.setMaximumSize(new Dimension(maxWidth, maxHeight));
    return pane;
  }

	public static void showMessage(final Component aCaller, final Object aMessage)
	{
		invoke(new Runnable()
		{
			@Override
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
			@Override
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
			@Override
			public void run()
			{
				JOptionPane.showMessageDialog(aCaller, ResourceMgr.getString(aKey), ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.INFORMATION_MESSAGE);
			}
		});
	}

	public static boolean getYesNo(Component aCaller, String aMessage)
	{
		return getYesNo(null, aCaller, aMessage);
	}

	public static boolean getYesNo(String windowTitle, Component aCaller, String aMessage)
	{
		int result = JOptionPane.showConfirmDialog(getWindowAncestor(aCaller), aMessage, (windowTitle == null ? ResourceMgr.TXT_PRODUCT_NAME : windowTitle), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
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
		int result = JOptionPane.showConfirmDialog(getWindowAncestor(aCaller), aMessage, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
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
		final JOptionPane ignorePane = new WbOptionPane(msg, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
		final JDialog dialog = ignorePane.createDialog(getWindowAncestor(aCaller), ResourceMgr.TXT_PRODUCT_NAME);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		try
		{
			invoke(new Runnable()
			{
				@Override
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

  public static JComponent buildErrorQuestion(String message, String errorMessage)
  {
    List<String> lines = StringUtil.getLines(message);
    JPanel messagePanel = new JPanel(new GridLayout(lines.size(), 1, 0, 2));
    Color color = UIManager.getColor("OptionPane.messageForeground");
    Font messageFont = UIManager.getFont("OptionPane.messageFont");
    for (String line : lines)
    {
      JLabel label = new JLabel(line, JLabel.LEFT);
      if (color != null)
      {
        label.setForeground(color);
      }
      if (messageFont != null)
      {
        label.setFont(messageFont);
      }
      messagePanel.add(label);
    }

    int vgap = 16;

    if (messageFont != null)
    {
      FontMetrics fm = messagePanel.getFontMetrics(messageFont);
      if (fm != null)
      {
        vgap = fm.getHeight();
      }
    }
    JPanel panel = new JPanel(new BorderLayout(0, vgap));
    panel.add(messagePanel, BorderLayout.PAGE_START);
    JComponent errorPanel = createErrorMessagePanel(errorMessage, false);
    panel.add(errorPanel, BorderLayout.CENTER);
    return panel;
  }

	public static int getYesNoIgnoreAll(Component aCaller, Object message)
	{
		String[] options = new String[]
		{
			ResourceMgr.getString("LblYes"), ResourceMgr.getString("LblNo"), ResourceMgr.getString("LblIgnoreAll")
		};

		JOptionPane ignorePane = new WbOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);

		JDialog dialog = ignorePane.createDialog(getWindowAncestor(aCaller), ResourceMgr.TXT_PRODUCT_NAME);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		int rvalue = -1;
		try
		{
			dialog.setResizable(true);
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
		JOptionPane ignorePane = new WbOptionPane(aMessage, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
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


	public static int getYesNo(Component caller, String title, String message, String yesOption, String noOption)
	{
		if (StringUtil.isEmptyString(yesOption))
		{
			yesOption = ResourceMgr.getString("MsgConfirmYes");
		}
		if (StringUtil.isEmptyString(noOption))
		{
			noOption = ResourceMgr.getString("MsgConfirmNo");
		}
		return getYesNo(caller, title, message, new String[] { yesOption, noOption }, JOptionPane.PLAIN_MESSAGE);
	}

	public static int getYesNo(Component aCaller, String aMessage, String[] options)
	{
		return getYesNo(aCaller, ResourceMgr.TXT_PRODUCT_NAME, aMessage, options, JOptionPane.PLAIN_MESSAGE);
	}

	public static int getYesNo(Component aCaller, String title, String aMessage, String[] options, int type)
	{
		JOptionPane ignorePane = new WbOptionPane(aMessage, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null, options, options[1]);
		ignorePane.setMessageType(type);
		JDialog dialog = ignorePane.createDialog(aCaller, title);
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
		JOptionPane pane = new WbOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = pane.createDialog(aCaller, title);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		if (doLater != null)
		{
			WindowAdapter w = new WindowAdapter()
			{
				@Override
				public void windowOpened(WindowEvent evt)
				{
					EventQueue.invokeLater(doLater);
				}
			};
			dialog.addWindowListener(w);
		}

		dialog.setResizable(true);
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
		JOptionPane ignorePane = new WbOptionPane(aMessage, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null, options);
		int w = 0;
		try
		{
			Font f = ignorePane.getFont();
			FontMetrics fm = ignorePane.getFontMetrics(f);
			w = fm.stringWidth(aMessage);
		}
		catch (Throwable th)
		{
			LogMgr.logError("WbSwingUtilities.getCommitRollbackQuestion()", "Could not detect string width", th);
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

   private enum UserInputType
   {
      TEXT,
      NUMBER,
      HIDDEN
   }

	public static String getUserInput(Component caller, String title, String initialValue)
	{
		return getUserInput(caller, title, initialValue, UserInputType.TEXT, 40);
	}

	public static String getUserInputHidden(Component caller, String title, String initialValue)
	{
		return getUserInput(caller, title, initialValue, UserInputType.HIDDEN, 40);
	}

	public static String getUserInputNumber(Component caller, String title, String initialValue)
	{
	   return getUserInput(caller, title, initialValue, UserInputType.NUMBER, 40);
	}

	private static String getUserInput(Component caller, String title, String initialValue, UserInputType inputType, int textSize)
	{
		Window parent = getWindowAncestor(caller);

		final JTextField input;
		switch (inputType)
		{
			case NUMBER:
				input = new JTextField();
				Document document = input.getDocument();
				if (document instanceof AbstractDocument)
				{
					AbstractDocument abDocument = (AbstractDocument) document;
					abDocument.setDocumentFilter(new DocumentFilter()
					{
						@Override
						public void insertString(FilterBypass fb, int offset,String text,AttributeSet attr)
							throws BadLocationException
						{
							int len = text.length();
							if (Character.isDigit(text.charAt(len - 1)))
							{
								super.insertString(fb, offset, text, attr);
							}
						}

						@Override
						public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
							throws BadLocationException
						{
							int len = text.length();
							if (Character.isDigit(text.charAt(len - 1)))
							{
								super.replace(fb, offset, length, text, attrs);
							}
						}
					});
				}
				break;
			case HIDDEN:
				input = new JPasswordField();
				break;
			case TEXT:
			default:
				input = new JTextField();
				break;
		}

		input.setColumns(textSize);
		input.setText(initialValue);
		if (initialValue != null)
		{
			input.selectAll();
		}
		input.addMouseListener(new TextComponentMouseListener());

		boolean ok = ValidatingDialog.showConfirmDialog(parent, input, title);
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
		c.invalidate();
		c.repaint();
	}

	public static void repaintNow(final Component c)
	{
		invoke(new Runnable()
		{
			@Override
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
			@Override
			public void run()
			{
				callRepaint(c);
			}
		});
	}

	/**
	 * Registers a (temporary) WindowListener with the window that will activate the passed component
	 * as soon as the windowActivated() event is received.
	 *
	 * @param win     the parent window
	 * @param comp    the component to focus in the window
	 *
	 */
	public static void requestComponentFocus(final Window win, final JComponent comp)
	{
		win.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowActivated(WindowEvent evt)
			{
				EventQueue.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						comp.requestFocus();
					}
				});
				win.removeWindowListener(this);
			}
		});
	}

	/**
	 * Schedules a requestFocus() call in the AWT event queue.
	 *
	 * @param comp  the component to focus
	 *
	 * @see java.awt.EventQueue#invokeLater(java.lang.Runnable)
	 */
	public static void requestFocus(final JComponent comp)
	{
		if (comp == null) return;

		EventQueue.invokeLater(new Runnable()
		{
			@Override
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

	/**
	 * Checks if the connection is busy, and displays an error message if it is.
	 *
	 * @param parent       the reference component
	 * @param dbConnection the connection to check
	 * @return true if the connection can be used (not busy)
	 *         false if the connection can not be used (because it's busy)
	 */
	public static boolean isConnectionIdle(Component parent, WbConnection dbConnection)
	{
		if (dbConnection.isBusy())
		{
			showMessageKey(SwingUtilities.getWindowAncestor(parent), "ErrConnectionBusy");
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

	public static void setMinimumSize(JComponent component, int lines, int columns)
	{
		Font font = component.getFont();
		if (font != null)
		{
			FontMetrics fm = component.getFontMetrics(font);
			int width = fm.stringWidth("M");
			int height = fm.getHeight();
			Dimension d = new Dimension(width * columns, height * lines);
			component.setPreferredSize(d);
		}
	}

	public static void setMinimumSizeFromCols(JTextField component)
	{
		int columns = component.getColumns();
		component.setColumns(0);
		setMinimumSize(component, columns);
	}

	public static void setMinimumSize(JComponent component, int numChars)
	{
		int width = calculateCharWidth(component, numChars);
		if (width > 0)
		{
			Dimension current = component.getPreferredSize();
			Dimension d = new Dimension(width, current.height);
			component.setPreferredSize(d);
		}
	}

	public static int calculateCharWidth(JComponent component, int numChars)
	{
		if (numChars < 0) return -1;
		if (component == null) return -1;

		int width = -1;
		Font font = component.getFont();
		if (font != null)
		{
			FontMetrics fm = component.getFontMetrics(font);
			int charWidth = fm.stringWidth("M");
			width = charWidth * numChars;
		}
		return width;
	}

	public static String choiceToString(int choice)
	{
		switch (choice)
		{
			case JOptionPane.OK_OPTION:
				return "OK_OPTION";
			case JOptionPane.CANCEL_OPTION:
				return "CANCEL_OPTION";
			case JOptionPane.NO_OPTION:
				return "NO_OPTION";
			case JOptionPane.CLOSED_OPTION:
				return "CLOSED_OPTION";
			default:
				return NumberStringCache.getNumberString(choice);
		}
	}

	public static void showToolTip(final JComponent component, String tip)
	{
		if (component == null) return;
		if (!component.isShowing()) return;

		Point pos = null;
		try
		{
			pos = component.getLocationOnScreen();
		}
		catch (Exception ex)
		{
			// this can happen if the component is hidden for some reason
			// but isShowing() could not detect that.
			pos = null;
		}
		if (pos == null) return;

		JToolTip tooltip = component.createToolTip();
		PopupFactory popupFactory = PopupFactory.getSharedInstance();
		tooltip.setTipText(tip);

		final Popup tooltipContainer = popupFactory.getPopup(component, tooltip, (int)pos.getX(), (int)pos.getY() - (int)tooltip.getPreferredSize().getHeight());

		final int timeout = ToolTipManager.sharedInstance().getDismissDelay();

		WbThread hideThread = new WbThread("Tooltip Hider")
		{
			@Override
			public void run()
			{
				WbThread.sleepSilently(timeout);
				tooltipContainer.hide();
			}
		};
		hideThread.start();

		tooltipContainer.show();
		tooltip.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				tooltipContainer.hide();
			}
		});
}

	public static int calculateMaxMenuItems(JFrame window)
	{
		UIDefaults def = UIManager.getDefaults();
		Font itemFont = def.getFont("MenuItem.font");
		Font barFont = def.getFont("MenuBar.font");

		int itemHeight = 16;
		int barHeight = 24;

		FontMetrics fm = window.getFontMetrics(itemFont);
		if (fm != null)
		{
			itemHeight = fm.getHeight();
		}
		else
		{
			itemHeight = convertPointSizeToPixel(itemFont.getSize());
		}
		FontMetrics barMetrics = window.getFontMetrics(barFont);
		if (barMetrics != null)
		{
			barHeight = fm.getHeight();
		}
		else
		{
			barHeight = convertPointSizeToPixel(barFont.getSize());
		}

		Insets itemInsets = def.getInsets("MenuItem.margin");
		if (itemInsets != null)
		{
			itemHeight += itemInsets.top + itemInsets.bottom;
		}

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = (int)screen.getHeight();
		int usableHeight = screenHeight - barHeight * 3;
		int numItems = usableHeight / itemHeight;
		return numItems;
	}

	public static int convertPointSizeToPixel(int points)
	{
		int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		int pixel = points * dpi / 72;
		return pixel;
	}

	public static void adjustButtonWidth(JButton button, int minWidth, int minHeight)
	{
		Font f = button.getFont();
		if (f != null)
		{
			FontMetrics fm = button.getFontMetrics(f);
			if (fm != null)
			{
				Rectangle2D bounds = f.getStringBounds(button.getText(), fm.getFontRenderContext());
				int width = (int)bounds.getWidth() + 2;
				int height = Math.max((int)bounds.getHeight(), minHeight);
				Dimension pref = button.getPreferredSize();
				Dimension dim = new Dimension(Math.max(width,minWidth), Math.max(height, pref.height));
				button.setPreferredSize(dim);
				button.setMinimumSize(dim);
			}
		}
	}

	public static void makeEqualHeight(JComponent reference, JComponent ... others)
	{
		if (reference == null) return;
		if (others == null) return;

		int height = reference.getPreferredSize().height;

		for (JComponent comp : others)
		{
			Dimension size = comp.getPreferredSize();
			size.height = height;
			comp.setPreferredSize(size);
		}
	}

	public static void makeEqualSize(JComponent reference, JComponent ... others)
	{
		if (reference == null) return;
		if (others == null) return;

		Dimension size = reference.getPreferredSize();

		for (JComponent comp : others)
		{
			comp.setPreferredSize(size);
		}
	}

	/**
	 * Sets the minimum width of all components to the width of the widest component.
	 * <p/>
	 * @param components the components to change
	 * @see #setPreferredWidth(javax.swing.JComponent, int)
	 */
	public static void makeEqualWidth(JComponent ... components)
	{
		int maxSize = 0;
		for (JComponent comp : components)
		{
			Dimension size = comp.getPreferredSize();
			if (size.width > maxSize)
			{
				maxSize = size.width;
			}
		}
		for (JComponent comp : components)
		{
			setPreferredWidth(comp, maxSize);
		}
	}

	/**
	 * Sets the minimum width of a {@link JButton}.
	 * <p/>
	 * @param button    the button to change
	 * @param minWidth  the minimum width to apply
	 */
	public static void setPreferredWidth(JComponent button, int minWidth)
	{
		Dimension size = button.getPreferredSize();
		if (size.width < minWidth)
		{
			size.width = minWidth;
			button.setPreferredSize(size);
		}
	}

	public static void removeAllListeners(JComponent comp)
	{
		if (comp == null) return;

		PropertyChangeListener[] listeners = comp.getPropertyChangeListeners();
		for (PropertyChangeListener l : listeners)
		{
			comp.removePropertyChangeListener(l);
		}

		FocusListener[] focusListeners = comp.getFocusListeners();
		if (focusListeners != null)
		{
			for (FocusListener l : focusListeners)
			{
				comp.removeFocusListener(l);
			}
		}

		MouseListener[] mouseListeners = comp.getMouseListeners();
		if (mouseListeners != null)
		{
			for (MouseListener l : mouseListeners)
			{
				comp.removeMouseListener(l);
			}
		}
	}

	public static int getFontHeight(JComponent comp)
	{
		if (comp == null) return 0;
		Font font = comp.getFont();
		if (font == null) return 0;
		FontMetrics fm = comp.getFontMetrics(font);
		if (fm == null)
		{
			return 16;
		}
		return fm.getHeight();
	}

	public static void scale(Window toScale, double factorWidth, double factorHeight)
	{
		Dimension size = toScale.getSize();
		size.width = (int)(size.width * factorWidth);
		size.height = (int)(size.height * factorHeight);
		toScale.setSize(size);
	}
}
