package workbench.gui;
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
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import workbench.gui.components.TextComponentMouseListener;
import workbench.resource.ResourceMgr;

public class WbSwingUtilities
{

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
	
	public static void showWaitCursor(JComponent caller)
	{
		//RootPaneContainer rpc = (RootPaneContainer)SwingUtilities.getWindowAncestor(caller);
		//if (rpc == null) return;
		//rpc.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		//rpc.getGlassPane().setVisible(true);
		caller.getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}
	
	public static void showDefaultCursor(JComponent caller)
	{
		//RootPaneContainer rpc = (RootPaneContainer)SwingUtilities.getWindowAncestor(caller);
		//if (rpc == null) return;
		//rpc.getGlassPane().setVisible(false);
		caller.getTopLevelAncestor().setCursor(null);
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