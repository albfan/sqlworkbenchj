/*
 * HtmlViewer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.help;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


public class HtmlViewer 
	extends JDialog 
	implements HyperlinkListener 
{
	JEditorPane display;
	
	public HtmlViewer(JFrame owner)
	{
		this(owner, "index.html");
	}
	
	public HtmlViewer(JFrame owner, String aStartFile)
	{
		super(owner, ResourceMgr.getString("TxtHelpWindowTitle"), false);
		this.initHtml(aStartFile);
		this.restoreSettings(owner);
	}

	public HtmlViewer(JDialog owner)
	{
		super(owner, ResourceMgr.getString("TxtHelpWindowTitle"), false);
		this.initHtml(null);
		this.restoreSettings(owner);
	}
	
	private void restoreSettings(Window owner)
	{
		if (!Settings.getInstance().restoreWindowSize(this))
		{
			setSize(800,600);
		}
		
		if (!Settings.getInstance().restoreWindowPosition(this))
		{
			WbSwingUtilities.center(this, owner);
		}
	}
	
	private void initHtml(String aStartFile)
	{
		display = new JEditorPane();
		display.setFont(new Font("SansSerif", Font.PLAIN, 14));
		JScrollPane scroll = new JScrollPane(display);
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(scroll, BorderLayout.CENTER);
		
		this.initDocument();
		
		display.addHyperlinkListener(this);
		
		if (aStartFile != null) this.showHtmlFile(aStartFile);
		
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
	
	private void initDocument()
	{
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
	}

	public void showDataPumperHelp()
	{
		this.showHtmlFile("data-pumper.html");
	}
	
	public void showProfileHelp()
	{
		this.showHtmlFile("profiles.html");
	}
	
	public void showIndex()
	{
		this.showHtmlFile("index.html");
	}
	
	private void showHtmlFile(String aFile)
	{
		try
		{
			this.initDocument();
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
			this.setVisible(true);
			this.requestFocus();
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					repaint();
					getRootPane().updateUI();
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void saveSettings()
	{
		Settings.getInstance().storeWindowPosition(this);
		Settings.getInstance().storeWindowSize(this);
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

}
