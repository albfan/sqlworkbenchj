/*
 * TestFrame.java
 *
 * Created on November 26, 2001, 11:22 PM
 */

package workbench.gui.html;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.net.URL;
import javax.swing.*;
import javax.swing.JDialog;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;


public class HtmlViewer 
	extends JDialog 
	implements HyperlinkListener 
{
	JTextPane display;
	
	public HtmlViewer(JFrame owner)
	{
		
		super(owner, ResourceMgr.getString("TxtHelpWindowTitle"), false);
		display = new JTextPane();
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
		
		//setSize(800, 600);
		
		HTMLEditorKit kit = new HTMLEditorKit();
		HTMLDocument htmlDoc = new HTMLDocument();
		display.setEditable(false);
		display.setEditorKit(kit);
		display.setDocument(htmlDoc);
		display.addHyperlinkListener(this);
		try
		{

			URL file = this.getClass().getClassLoader().getResource("help/SQL Workbench Manual.html");
			if (file != null)
			{
				display.setPage(file);
			}
			else
			{
				System.out.println("file not found!");
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
	
	public void hyperlinkUpdate(HyperlinkEvent e)
	{
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
		{
			JEditorPane pane = (JEditorPane) e.getSource();
			String descr=e.getDescription();
			if (descr != null && descr.startsWith("#"))
			{
				//System.out.println("descr=" + descr);
				descr=descr.substring(1).replaceAll(" ", "%20");
				display.scrollToReference(descr);
			}
		}
	}

	public static void main(String[] args)
	{
		new HtmlViewer(null).show();
	}
}
