/*
 * ExportOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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
import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.db.exporter.PoiHelper;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ColumnSelectorPanel;
import workbench.gui.components.DividerBorder;
import workbench.interfaces.EncodingSelector;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;
import workbench.util.SqlUtil;

/**
 *
 * @author  support@sql-workbench.net
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
	private SpreadSheetOptionsPanel odsOptions;
	private SpreadSheetOptionsPanel xlsOptions;
	private SpreadSheetOptionsPanel xlsxOptions;
	private int currentType = -1;
	private List<ColumnIdentifier> selectedColumns;
	private Object columnSelectEventSource;
	private ColumnSelectorPanel columnSelectorPanel;
	private ResultInfo dataStoreColumns;
	private String query;
	private WbConnection dbConnection;
	private boolean poiAvailable = false;
	
	private final String ODS_ITEM = ResourceMgr.getString("TxtOdsName");
	private final String XLS_ITEM = ResourceMgr.getString("TxtXlsName");
	private final String XLSX_ITEM = "XLS (XML)";
	
	public ExportOptionsPanel()
	{
		this(null);
	}
	
	public ExportOptionsPanel(ResultInfo columns)
	{
		super();
		this.setLayout(new BorderLayout());
		boolean allowColumnSelection = (columns != null);
		this.dataStoreColumns = columns;
		this.generalOptions = new GeneralExportOptionsPanel();
		generalOptions.allowSelectColumns(allowColumnSelection);
		if (allowColumnSelection)
		{
			generalOptions.showSelectColumnsLabel();
			this.columnSelectEventSource = generalOptions.addColumnSelectListener(this);
		}
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(this.generalOptions, BorderLayout.CENTER);
		
		JPanel s = new JPanel(new BorderLayout(2, 2));
		Border b = new CompoundBorder(DividerBorder.BOTTOM_DIVIDER, new EmptyBorder(0, 0, 5, 0));
		s.setBorder(b);
		
		poiAvailable = PoiHelper.isPoiAvailable();
		
		typeSelector = new JComboBox();
		typeSelector.addItem("Text");
		typeSelector.addItem("SQL");
		typeSelector.addItem("XML");
		typeSelector.addItem(ODS_ITEM);
		typeSelector.addItem("HTML");
		typeSelector.addItem(XLSX_ITEM);
		if (poiAvailable)
		{
			typeSelector.addItem(XLS_ITEM);
		}
		
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

		odsOptions = new SpreadSheetOptionsPanel("ods");
		this.typePanel.add(odsOptions, "ods");
		
		htmlOptions = new HtmlOptionsPanel();
		this.typePanel.add(htmlOptions, "html"); 
		
		xlsxOptions = new SpreadSheetOptionsPanel("xlsx");
		this.typePanel.add(xlsxOptions, "xlsx"); 
		
		if (poiAvailable)
		{
			xlsOptions = new SpreadSheetOptionsPanel("xls");
			this.typePanel.add(xlsOptions, "xls");
		}
		
		this.add(typePanel, BorderLayout.CENTER);
		typeSelector.addActionListener(this);
	}
	
	public void setQuerySql(String sql, WbConnection con)
	{
		this.query = sql;
		this.dbConnection = con;
		this.dataStoreColumns = null;
		generalOptions.allowSelectColumns(true);
		generalOptions.showRetrieveColumnsLabel();
		if (this.columnSelectEventSource == null)
		{
			this.columnSelectEventSource = generalOptions.addColumnSelectListener(this);
		}
	}
	
	public void setIncludeSqlUpdate(boolean flag)
	{
		this.sqlOptions.setIncludeUpdate(flag);
	}
	
	public void setIncludeSqlDeleteInsert(boolean flag)
	{
		this.sqlOptions.setIncludeDeleteInsert(flag);
	}

	public List<ColumnIdentifier> getColumnsToExport()
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
		this.odsOptions.saveSettings();
		this.xlsxOptions.saveSettings();
		if (this.xlsOptions != null)
		{
			this.xlsOptions.saveSettings();
		}
		Settings.getInstance().setProperty("workbench.export.type", Integer.toString(this.currentType));
	}
	
	public void restoreSettings()
	{
		this.generalOptions.restoreSettings();
		this.sqlOptions.restoreSettings();
		this.textOptions.restoreSettings();
		this.htmlOptions.restoreSettings();
		this.xmlOptions.restoreSettings();
		this.odsOptions.restoreSettings();
		this.xlsxOptions.restoreSettings();
		if (this.xlsOptions != null)
		{
			this.xlsOptions.restoreSettings();
		}
		int type = Settings.getInstance().getIntProperty("workbench.export.type", -1);
		this.setExportType(type);
	}
	
	/**
	 *	Sets the displayed options according to 
	 *  DataExporter.EXPORT_XXXX types
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
			case DataExporter.EXPORT_ODS:
				setTypeOds();
				break;
			case DataExporter.EXPORT_XLSX:
				setTypeXlsX();
				break;
			case DataExporter.EXPORT_XLS:
				if (poiAvailable) setTypeXls();
				break;
		}
	}

	public int getExportType()
	{
		return this.currentType;
	}

	private void showTextOptions()
	{
		this.card.show(this.typePanel, "text");
	}
	
	public void setTypeText()
	{
		showTextOptions();
		this.currentType = DataExporter.EXPORT_TXT;
		typeSelector.setSelectedItem("Text");
	}

	private void showSqlOptions()
	{
		this.card.show(this.typePanel, "sql");
	}
	
	public void setTypeSql()
	{
		showSqlOptions();
		this.currentType = DataExporter.EXPORT_SQL;
		typeSelector.setSelectedItem("SQL");
	}

	private void showXmlOptions()
	{
		this.card.show(this.typePanel, "xml");
	}
	
	public void setTypeXml()
	{
		showXmlOptions();
		this.currentType = DataExporter.EXPORT_XML;
		typeSelector.setSelectedItem("XML");
	}

	private void showHtmlOptions()
	{
		this.card.show(this.typePanel, "html");
	}
	
	public void setTypeHtml()
	{
		showHtmlOptions();
		this.currentType = DataExporter.EXPORT_HTML;
		typeSelector.setSelectedItem("HTML");
	}

	private void showOdsOptions()
	{
		this.card.show(this.typePanel, "ods");
	}
	
	public void setTypeOds()
	{
		showOdsOptions();
		this.currentType = DataExporter.EXPORT_ODS;
		typeSelector.setSelectedItem(ODS_ITEM);
	}
	
	private void showXlsOptions()
	{
		this.card.show(this.typePanel, "xls");
	}

	private void showXlsXOptions()
	{
		this.card.show(this.typePanel, "xlsx");
	}
	
	public void setTypeXls()
	{
		showXlsOptions();
		this.currentType = DataExporter.EXPORT_XLS;
		typeSelector.setSelectedItem(XLS_ITEM);
	}

	public void setTypeXlsX()
	{
		showXlsXOptions();
		this.currentType = DataExporter.EXPORT_XLSX;
		typeSelector.setSelectedItem(XLSX_ITEM);
	}
	
	public SpreadSheetOptions getXlsOptions()
	{
		return xlsOptions;
	}

	public SpreadSheetOptions getXlsXOptions()
	{
		return xlsxOptions;
	}
	
	public SpreadSheetOptions getOdsOptions()
	{
		return odsOptions;
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
			Object item = typeSelector.getSelectedItem();
			String itemValue = item.toString();
			int type = -1;

			this.card.show(this.typePanel, itemValue);

			if ("text".equalsIgnoreCase(itemValue))
			{
				type = DataExporter.EXPORT_TXT;
				showTextOptions();
			}
			else if ("sql".equalsIgnoreCase(itemValue))
			{
				type = DataExporter.EXPORT_SQL;
				showSqlOptions();
			}
			else if ("xml".equalsIgnoreCase(itemValue))
			{
				type = DataExporter.EXPORT_XML;
				showXmlOptions();
			}
			else if (item == ODS_ITEM)
			{
				type = DataExporter.EXPORT_ODS;
				showOdsOptions();
			}
			else if (item == XLSX_ITEM)
			{
				type = DataExporter.EXPORT_XLSX;
				showXlsXOptions();
			}
			else if (item == XLS_ITEM && poiAvailable)
			{
				type = DataExporter.EXPORT_XLS;
				showXlsOptions();
			}
			else if ("html".equalsIgnoreCase(itemValue))
			{
				type = DataExporter.EXPORT_HTML;
				showHtmlOptions();
			}
			
			this.currentType = type;
			firePropertyChange("exportType", -1, type);
		}
		else if (event.getSource() == this.columnSelectEventSource)
		{
			this.selectColumns();
		}
	}
	
	private void retrieveQueryColumns()
	{
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			this.dataStoreColumns = SqlUtil.getResultInfoFromQuery(this.query, this.dbConnection);
			sqlOptions.setResultInfo(this.dataStoreColumns);
		}
		catch (Exception e)
		{
			this.dataStoreColumns = null;
			LogMgr.logError("ExportOptionsPanel.retrieveQueryColumns()", "Could not retrieve query columns", e);
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(this);
		}
	}
	
	private void selectColumns()
	{
		if (this.dataStoreColumns == null)
		{
			if (this.query != null)
			{
				retrieveQueryColumns();
			}
		}
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
