package workbench.gui.sql;

import javax.swing.SwingUtilities;
import java.awt.Component;


public class FocusSetter
	implements Runnable
{
	Component comp = null;
	
	public FocusSetter(Component c)
	{
		this.comp = c;
	}
	
	public void run()
	{
		if (comp != null) comp.requestFocus();
	}
	
	public static void setFocus(Component c)
	{
		SwingUtilities.invokeLater(new FocusSetter(c));
	}
}




