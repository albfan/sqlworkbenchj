/*
 * HtmlPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.help;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;


public class HtmlPanel 
	extends JPanel 
	implements HyperlinkListener 
{
	private JEditorPane display;

	public HtmlPanel()
	{
		super();
		this.initHtml();
	}
	
	public HtmlPanel(String startFile)
	{
		super();
		this.initHtml();
		if (!StringUtil.isEmptyString(startFile)) loadHtmlFile(startFile);
	}

	private void initHtml()
	{
		display = new JEditorPane();
		display.setEditable(false);
		display.addHyperlinkListener(this);
		
		JScrollPane scroll = new JScrollPane(display);
		
		setLayout(new BorderLayout());
		add(scroll, BorderLayout.CENTER);
	}
	
	public void showDataPumperHelp()
	{
		this.loadHtmlFile("data-pumper.html");
	}
	
	public void showOptionsHelp()
	{
		this.loadHtmlFile("options.html");
	}
	
	public void showProfileHelp()
	{
		this.loadHtmlFile("profiles.html");
	}
	
	public void showIndex()
	{
		this.loadHtmlFile("workbench-manual.html");
	}
	
	private void loadHtmlFile(String aFile)
	{
		try
		{
			//this.initDocument();
			URL file = this.getClass().getClassLoader().getResource("help/" + aFile);
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
			LogMgr.logError("HtmlPanel.loadHtmlFile()", "Error when loading HTML help", e);
		}
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

}
