/*
 * TestFrame.java
 *
 * Created on November 26, 2001, 11:22 PM
 */

package workbench.gui.help;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Enumeration;
import javax.swing.*;
import javax.swing.JDialog;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.text.html.StyleSheet;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;


public class HtmlViewer 
	extends JDialog 
	implements HyperlinkListener 
{
	JEditorPane display;
	
	public HtmlViewer(JFrame owner)
	{
		
		super(owner, ResourceMgr.getString("TxtHelpWindowTitle"), false);
		display = new JEditorPane();
		display.setFont(new Font("SansSerif", Font.PLAIN, 14));
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
		
		HTMLEditorKit kit = new HTMLEditorKit();
		StyleSheet style = new StyleSheet();
		HTMLDocument htmlDoc = null;
		try
		{
			URL file = this.getClass().getClassLoader().getResource("help/html-internal.css");
			if (file != null)
			{
				style.importStyleSheet(file);
				htmlDoc = new HTMLDocument(style);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("HtmlViewer", "Error loading style sheet html-internal.css", e);
		}

		if (htmlDoc == null)
		{
			htmlDoc = new HTMLDocument();
		}
		
		display.setEditable(false);
		display.setEditorKit(kit);
		display.setDocument(htmlDoc);
		display.addHyperlinkListener(this);
		try
		{

			URL file = this.getClass().getClassLoader().getResource("help/index.html");
			if (file == null)
			{
				file = this.getClass().getClassLoader().getResource("workbench/gui/help/NotFound.html");
			}
			
			if (file != null)
			{
				display.setPage(file);
			}
			else
			{
				display.setContentType("text/html");
				display.setText("<h2>Help file not found!</h2>"); 
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

	public void scrollToReference(HTMLDocument doc, String reference)
	{
		HTMLDocument.Iterator iter = doc.getIterator(HTML.Tag.A);
		String ref2 = reference.replaceAll(" ", "%20");
		for (; iter.isValid(); iter.next())
		{
			AttributeSet a = iter.getAttributes();
			String nm = (String) a.getAttribute(HTML.Attribute.NAME);
			if ((nm != null) && ( nm.equals(reference)  || nm.equals(ref2) ))
			{
				// found a matching reference in the document.
				try
				{
					Rectangle r = display.modelToView(iter.getStartOffset());
					if (r != null)
					{
						// the view is visible, scroll it to the
						// center of the current visible area.
						Rectangle vis = display.getVisibleRect();
						r.height = vis.height;
						display.scrollRectToVisible(r);
					}
				} 
				catch (Exception ble)
				{
					ble.printStackTrace();
				}
			}
		}
	}
	
	
	public void hyperlinkUpdate(HyperlinkEvent e)
	{
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
		{
			JEditorPane pane = (JEditorPane) e.getSource();
			String descr=e.getDescription();
			if (descr != null && descr.startsWith("#"))
			{
				descr=descr.substring(1);
				HTMLDocument html = (HTMLDocument)display.getDocument();
				this.scrollToReference(html, descr);
			}
			else
			{
				try
				{
					display.setPage(e.getURL());
				}
				catch (Exception ex)
				{
					LogMgr.logError("HtmlViewer.hyperlinkUpdate()", "Could not open URL=" + e.getURL(), ex);
				}
			}
		}
	}

	public static void main(String[] args)
	{
		new HtmlViewer(null).show();
	}
}
