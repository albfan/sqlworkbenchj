package workbench.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import javax.swing.RootPaneContainer;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
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
		showWaitCursor(caller);
		showWaitCursor(parent);
	}
	public static void showDefaultCursorOnWindow(Component caller)
	{
		Window parent = SwingUtilities.getWindowAncestor(caller);
		showDefaultCursor(caller);
		showDefaultCursor(parent);
	}
	
	public static void showWaitCursor(Component caller)
	{
		caller.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}
	
	public static void showDefaultCursor(Component caller)
	{
		caller.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	public static void showErrorMessage(Component aCaller, String aMessage)
	{
		JOptionPane.showMessageDialog(aCaller, aMessage, ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.ERROR_MESSAGE);
	}
	
	public static String getUserInput(Component caller, String aTitle, String initialValue)
	{
		Window parent = SwingUtilities.getWindowAncestor(caller);
		final JTextField input = new JTextField();
		input.setColumns(40);
		input.setText(initialValue);
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