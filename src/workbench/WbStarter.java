package workbench;

import java.awt.Toolkit;
import java.lang.reflect.Method;
import javax.swing.JOptionPane;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbStarter
{

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		String version = System.getProperty("java.version", null);
		if (version == null)
		{
			version = System.getProperty("java.runtime.version");
		}
		boolean is14 = false;
		System.out.println("Workbench: Using Java version=" + version);
		try
		{
			int majorversion = Integer.parseInt(version.substring(0,1));
			int minorversion = Integer.parseInt(version.substring(2,3));
			is14 = (majorversion >= 1) && (minorversion >= 4);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			is14 = false;
		}
	
		if (!is14)
		{
			JOptionPane.showMessageDialog(null, "JDK/JRE 1.4 or later is needed to run this application!");
			System.err.println("A JDK or JRE 1.4 or later is needed to run this application!");
			Toolkit.getDefaultToolkit().beep();
			Toolkit.getDefaultToolkit().beep();
			Toolkit.getDefaultToolkit().beep();
			System.exit(1);
		}
		try
		{
			// Make this class can be loaded even with JDK < 1.4
			// so no compile time reference to WbManager should occur here.
			Class manager = Class.forName("workbench.WbManager");
			Class[] parms = {String[].class};
			Method main = manager.getDeclaredMethod("main", parms);
			Object[] para = new Object[] { args };
			main.invoke(null, para);
			//WbManager.main(args);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

}
