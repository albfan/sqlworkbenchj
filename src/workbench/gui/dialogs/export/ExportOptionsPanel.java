/*
 * ExportOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import workbench.db.exporter.DataExporter;
import workbench.gui.components.ColumnSelectorPanel;
import workbench.gui.components.DividerBorder;
import workbench.interfaces.EncodingSelector;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;
import workbench.interfaces.TextOptions;
import workbench.interfaces.XmlOptions;
import workbench.gui.dialogs.XmlOptionsPanel;
import workbench.gui.dialogs.TextOptionsPanel;

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
	private boolean allowColumnSelection = false;
	private ResultInfo columns;
	private List selectedColumns;
	private Object columnSelectEventSource;
	private ColumnSelectorPanel columnSelectorPanel;
	private ResultInfo dataStoreColumns;
	
	public ExportOptionsPanel()
	{
		this(null);
	}
	
	public ExportOptionsPanel(ResultInfo columns)
	{
		super();
		this.setLayout(new BorderLayout());
		this.allowColumnSelection = (columns != null);
		this.dataStoreColumns = columns;
		this.generalOptions = new GeneralExportOptionsPanel();
		generalOptions.allowSelectColumns(this.allowColumnSelection );
		if (this.allowColumnSelection )
		{
			this.columnSelectEventSource = generalOptions.addColumnSelectListener(this);
		}
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
		
		this.sqlOptions = new SqlOptionsPanel(columns);
		this.typePanel.add(this.sqlOptions, "sql");
		
		xmlOptions = new XmlOptionsPanel();
		this.typePanel.add(xmlOptions, "xml");
		
		htmlOptions = new HtmlOptionsPanel();
		this.typePanel.add(htmlOptions, "html"); 
		
		this.add(typePanel, BorderLayout.CENTER);
		typeSelector.addActionListener(this);
	}
	
	public void setIncludeSqlUpdate(boolean flag)
	{
		this.sqlOptions.setIncludeUpdate(flag);
	}
	
	public void setIncludeSqlDeleteInsert(boolean flag)
	{
		this.sqlOptions.setIncludeDeleteInsert(flag);
	}

	public List getColumnsToExport()
	{
		return this.selectedColumns;
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
		if (event.getSource() == this.typeSelector)
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
		else if (event.getSource() == this.columnSelectEventSource)
		{
			this.selectColumns();
		}
	}
	
	private void selectColumns()
	{
		if (this.dataStoreColumns == null) return;
		if (this.columnSelectorPanel == null) 
		{
			this.columnSelectorPanel = new ColumnSelectorPanel(this.dataStoreColumns.getColumns());
			this.columnSelectorPanel.selectAll();
		}
		else
		{
			this.columnSelectorPanel.selectColumns(this.selectedColumns);
		}
		
		int choice = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), this.columnSelectorPanel, ResourceMgr.getString("MsgSelectColumnsWindowTitle"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (choice == JOptionPane.OK_OPTION)
		{
			this.selectedColumns = this.columnSelectorPanel.getSelectedColumns();
		}
	}
	
}
