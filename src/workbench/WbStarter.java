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
		boolean is14 = false;
		
		try
		{
			int majorversion = Integer.parseInt(version.substring(0,1));
			int minorversion = Integer.parseInt(version.substring(2,3));
			System.out.println(majorversion);
			System.out.println(minorversion);
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
			System.exit(1);
		}
		WbManager.startup(args);
	}

}
