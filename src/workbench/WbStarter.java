package workbench;

import javax.swing.JOptionPane;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbStarter
{

	public WbStarter()
	{
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		//System.out.println("WbStarter.main()");
		String version = System.getProperty("java.version", null);
		if (version == null)
		{
			version = System.getProperty("java.runtime.version");
		}
		if (!version.startsWith("1.4"))
		{
			JOptionPane.showMessageDialog(null, "JDK/JRE 1.4 or later is needed to run this application!");
			System.err.println("A JDK or JRE 1.4 or later is needed to run this application!");
			System.exit(1);
		}
		WbManager.startup();
	}

}
