package workbench.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import workbench.gui.components.TextComponentMouseListener;
import workbench.resource.ResourceMgr;
import java.awt.Container;


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
	 *	@param 	Window 	the window to be centered
	 *	@param	Window	center against this window. If null -> center on screen
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
		Window parent = SwingUtilities.getWindowAncestor(caller);
		//showWaitCursor(caller);
		if (parent != null) showWaitCursor(parent);
	}
	public static void showDefaultCursorOnWindow(Component caller)
	{
		showDefaultCursor(caller, true);
	}

	public static void showWaitCursor(final Component caller)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			caller.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					caller.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
			});
		}
	}

	public static void showDefaultCursor(final Component caller)
	{
		showDefaultCursor(caller, false);
	}
	public static void showDefaultCursor(final Component caller, final boolean includeParents)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			caller.setCursor(null);
			if (includeParents)
			{
				Container c = caller.getParent();
				while (c != null)
				{
					c.setCursor(null);
					c = c.getParent();
				}
			}
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					caller.setCursor(null);
					if (includeParents)
					{
						Container c = caller.getParent();
						while (c != null)
						{
							c.setCursor(null);
							c = c.getParent();
						}
					}
				}
			});
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

	public static final int DO_COMMIT = 0;
	public static final int DO_ROLLBACK = 1;

	public static int getCommitRollbackQuestion(Component aCaller, String aMessage)
	{
		String[] options = new String[] { ResourceMgr.getString("LabelCommit"), ResourceMgr.getString("LabelRollback")};
		JOptionPane ignorePane = new JOptionPane(aMessage, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null, options);
		JDialog dialog = ignorePane.createDialog(aCaller, ResourceMgr.TXT_PRODUCT_NAME);
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
		Component parent = SwingUtilities.getWindowAncestor(caller);

		final JTextField input = new JTextField();
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

}