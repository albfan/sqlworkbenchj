/*
 * ExportOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import workbench.db.exporter.DataExporter;
import workbench.gui.components.DividerBorder;
import workbench.interfaces.EncodingSelector;
import workbench.resource.Settings;

/**
 *
 * @author  info@sql-workbench.net
 *
 */
public class ExportOptionsPanel
	extends JPanel
	implements EncodingSelector, ActionListener
{
	private GeneralExportOptionsPanel generalOptions;
	private JPanel typePanel;
	private CardLayout card;
	private JComboBox typeSelector;
	private TextOptionsPanel textOptions;
	private SqlOptionsPanel sqlOptions;
	private HtmlOptionsPanel htmlOptions;
	private XmlOptionsPanel xmlOptions;
	private int currentType = -1;
	
	public ExportOptionsPanel()
	{
		super();
		this.setLayout(new BorderLayout());

		this.generalOptions = new GeneralExportOptionsPanel();
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(this.generalOptions, BorderLayout.CENTER);
		
		JPanel s = new JPanel(new BorderLayout(2, 2));
		Border b = new CompoundBorder(new DividerBorder(DividerBorder.BOTTOM), new EmptyBorder(0, 0, 5, 0));
		s.setBorder(b);
		typeSelector = new JComboBox();
		typeSelector.addItem("Text");
		typeSelector.addItem("SQL");
		typeSelector.addItem("XML");
		typeSelector.addItem("HTML");
		JLabel type = new JLabel("Type");
		s.add(type, BorderLayout.WEST);
		s.add(typeSelector, BorderLayout.CENTER);
		p.add(s, BorderLayout.SOUTH);
		
		this.add(p, BorderLayout.NORTH);
		
		this.textOptions = new TextOptionsPanel();
		this.typePanel = new JPanel();
		this.card = new CardLayout();
		this.typePanel.setLayout(card);
		this.typePanel.add(this.textOptions, "text");
		
		this.sqlOptions = new SqlOptionsPanel();
		this.typePanel.add(this.sqlOptions, "sql");
		
		xmlOptions = new XmlOptionsPanel();
		this.typePanel.add(xmlOptions, "xml");
		
		htmlOptions = new HtmlOptionsPanel();
		this.typePanel.add(htmlOptions, "html"); 
		
		this.add(typePanel, BorderLayout.CENTER);
		typeSelector.addActionListener(this);
	}
	
	public void setSqlUpdateAvailable(boolean flag)
	{
		this.sqlOptions.setSqlUpdateAvailable(flag);
	}

	public void saveSettings()
	{
		this.generalOptions.saveSettings();
		this.sqlOptions.saveSettings();
		this.textOptions.saveSettings();
		this.htmlOptions.saveSettings();
		this.xmlOptions.saveSettings();
		Settings.getInstance().setProperty("workbench.export.type", Integer.toString(this.currentType));
	}
	
	public void restoreSettings()
	{
		this.generalOptions.restoreSettings();
		this.sqlOptions.restoreSettings();
		this.textOptions.restoreSettings();
		this.htmlOptions.restoreSettings();
		this.xmlOptions.restoreSettings();
		int type = Settings.getInstance().getIntProperty("workbench.export.type", -1);
		this.setExportType(type);
	}
	
	/**
	 *	Sets the displayed options according to 
	 *  DataExporter.EXPRT_XXXX types
	 */
	public void setExportType(int type)
	{
		switch (type)
		{
			case DataExporter.EXPORT_HTML:
				setTypeHtml();
				break;
			case DataExporter.EXPORT_SQL:
				setTypeSql();
				break;
			case DataExporter.EXPORT_TXT:
				setTypeText();
				break;
			case DataExporter.EXPORT_XML:
				setTypeXml();
				break;
		}
	}

	public int getExportType()
	{
		return this.currentType;
	}
	
	public void setTypeText()
	{
		this.card.show(this.typePanel, "text");
		this.currentType = DataExporter.EXPORT_TXT;
		typeSelector.setSelectedIndex(0);
	}
	
	public void setTypeSql()
	{
		this.card.show(this.typePanel, "sql");
		this.currentType = DataExporter.EXPORT_SQL;
		typeSelector.setSelectedIndex(1);
	}
	
	public void setTypeXml()
	{
		this.card.show(this.typePanel, "xml");
		this.currentType = DataExporter.EXPORT_XML;
		typeSelector.setSelectedIndex(2);
	}
	
	public void setTypeHtml()
	{
		this.card.show(this.typePanel, "html");
		this.currentType = DataExporter.EXPORT_HTML;
		typeSelector.setSelectedIndex(3);
	}
	
	public XmlOptions getXmlOptions()
	{
		return xmlOptions;
	}
	
	public HtmlOptions getHtmlOptions()
	{
		return htmlOptions;
	}
	
	public SqlOptions getSqlOptions()
	{
		return sqlOptions;
	}
	
	public ExportOptions getExportOptions()
	{
		return generalOptions;
	}
	
	public TextOptions getTextOptions()
	{
		return textOptions;
	}
	
	public String getEncoding() 
	{
		return generalOptions.getEncoding();
	}
	
	public void setEncoding(String enc)
	{
		generalOptions.setEncoding(enc);
	}

	public void actionPerformed(ActionEvent event)
	{
		String item = typeSelector.getSelectedItem().toString().toLowerCase();
		int type = -1;
		
		this.card.show(this.typePanel, item);
		
		if ("text".equals(item))
			type = DataExporter.EXPORT_TXT;
		else if ("sql".equals(item))
			type = DataExporter.EXPORT_SQL;
		else if ("xml".equals(item))
			type = DataExporter.EXPORT_XML;
		else 
			type = DataExporter.EXPORT_HTML;
		firePropertyChange("exportType", -1, type);
	}
}
