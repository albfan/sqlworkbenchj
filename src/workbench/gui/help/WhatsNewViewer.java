/*
 * TestFrame.java
 *
 * Created on November 26, 2001, 11:22 PM
 */

package workbench.gui.help;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;


public class WhatsNewViewer extends JDialog 
{
	JTextPane display;
	
	public WhatsNewViewer(JFrame owner)
	{
		
		super(owner, ResourceMgr.getString("TxtWhatsNewWindowTitle"), false);
		display = new JTextPane();
		display.setFont(new Font("Monospaced", Font.PLAIN, 12));
		display.setEditable(false);
		JScrollPane scroll = new JScrollPane(display);
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(scroll, BorderLayout.CENTER);

		if (!WbManager.getSettings().restoreWindowSize(this))
		{
			setSize(800,600);
		}
		
		if (!WbManager.getSettings().restoreWindowPosition(this))
		{
			WbSwingUtilities.center(this, owner);
		}
		
//		setSize(800, 600);
		
		try
		{

			URL file = this.getClass().getClassLoader().getResource("help/history.txt");
			if (file == null)
			{
				file = this.getClass().getClassLoader().getResource("workbench/gui/help/NotFound.html");
			}
			
			if (file != null)
			{
				display.setPage(file);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				saveSettings();
				hide();
				dispose();
				//System.exit(1);
			}
		});
	}

	private void saveSettings()
	{
		WbManager.getSettings().storeWindowPosition(this);
		WbManager.getSettings().storeWindowSize(this);
	}

}
