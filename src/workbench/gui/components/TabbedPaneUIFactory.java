/*
 * TabbedPaneUIFactory.java
 *
 * Created on November 26, 2002, 2:01 PM
 */

package workbench.gui.components;

import com.sun.java.swing.plaf.motif.MotifLookAndFeel;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import java.awt.Insets;
import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.TabbedPaneUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 *
 * @author  tkellerer
 */
public class TabbedPaneUIFactory
{
	private static Insets topInsets = new Insets(2,1,1,1);
	private static Insets bottomInsets = new Insets(1,1,2,1);
	private static Insets leftInsets = new Insets(1,3,1,1);
	private static Insets rightInsets = new Insets(1,1,1,3);
	private static Insets defaultInsets = new Insets(1,1,1,1);
	
	static Insets getBorderLessInsets(int tabPlacement)
	{
		switch (tabPlacement)
		{
			case JTabbedPane.TOP:
				return topInsets;
			case JTabbedPane.BOTTOM:
				return bottomInsets;
			case JTabbedPane.LEFT:
				return leftInsets;
			case JTabbedPane.RIGHT:
				return rightInsets;
			default:
				return defaultInsets;
		}
	}
	
	public static TabbedPaneUI getBorderLessUI()
	{
		LookAndFeel lnf = UIManager.getLookAndFeel();
		if (lnf instanceof MetalLookAndFeel)
		{
			return getClassInstance("workbench.gui.components.BorderLessMetalTabbedPaneUI");
		}
		else if (lnf instanceof WindowsLookAndFeel)
		{
			return getClassInstance("workbench.gui.components.BorderLessWindowsTabbedPaneUI");
		}
		else if (lnf instanceof MotifLookAndFeel)
		{
			return getClassInstance("workbench.gui.components.BorderLessMotifTabbedPaneUI");
		}
		else
		{
			JTabbedPane pane = new JTabbedPane();
			return (TabbedPaneUI)UIManager.getUI(pane);
		}
	}
	
	private static TabbedPaneUI getClassInstance(String className)
	{
		TabbedPaneUI ui = null;
		try
		{
			Class cls = Class.forName(className);
			ui = (TabbedPaneUI)cls.newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			JTabbedPane pane = new JTabbedPane();
			ui = (TabbedPaneUI)UIManager.getUI(pane);
		}
		return ui;
	}
}
