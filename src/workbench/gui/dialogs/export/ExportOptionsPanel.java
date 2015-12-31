/*
 * ExportOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import workbench.interfaces.EncodingSelector;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.ExportType;
import workbench.db.exporter.PoiHelper;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ColumnSelectorPanel;
import workbench.gui.components.DividerBorder;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;

import workbench.util.SqlUtil;

import static workbench.db.exporter.ExportType.*;

/**
 *
 * @author  Thomas Kellerer
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
	private SpreadSheetOptionsPanel xlsmOptions;
	private SpreadSheetOptionsPanel xlsxOptions;
	private ExportType currentType;
	private List<ColumnIdentifier> selectedColumns;
	private Object columnSelectEventSource;
	private ColumnSelectorPanel columnSelectorPanel;
	private ResultInfo dataStoreColumns;
	private String query;
	private WbConnection dbConnection;
	private boolean poiAvailable = false;
	private boolean xlsxAvailable = false;

	private final String ODS_ITEM = ResourceMgr.getString("TxtOdsName");
	private final String XLS_ITEM = ResourceMgr.getString("TxtXlsName");
	private final String XLSM_ITEM = "Excel XML Spreadsheet (xml)";
	private final String XLSX_ITEM = "Excel Workbook (xlsx)";

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
		//generalOptions.setBorder(new EmptyBorder(0, 1, 0, 0));

		if (allowColumnSelection)
		{
			generalOptions.showSelectColumnsLabel();
			this.columnSelectEventSource = generalOptions.addColumnSelectListener(this);
		}
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(this.generalOptions, BorderLayout.CENTER);

		JPanel selectorPanel = new JPanel(new BorderLayout(2, 2));
		Border b = new CompoundBorder(DividerBorder.BOTTOM_DIVIDER, new EmptyBorder(0, 2, 2, 0));
		selectorPanel.setBorder(b);

		poiAvailable = PoiHelper.isPoiAvailable();
		xlsxAvailable = PoiHelper.isXLSXAvailable();

		typeSelector = new JComboBox();
		typeSelector.addItem("Text");
		typeSelector.addItem("SQL");
		typeSelector.addItem("XML");
		typeSelector.addItem("JSON");
		typeSelector.addItem(ODS_ITEM);
		typeSelector.addItem("HTML");
		if (poiAvailable)
		{
			typeSelector.addItem(XLS_ITEM);
		}
		typeSelector.addItem(XLSM_ITEM);
		if (xlsxAvailable)
		{
			typeSelector.addItem(XLSX_ITEM);
		}

		JLabel typeLabel = new JLabel(ResourceMgr.getString("LblExportType"));
		selectorPanel.add(typeLabel, BorderLayout.WEST);
		selectorPanel.add(typeSelector, BorderLayout.CENTER);
		p.add(selectorPanel, BorderLayout.SOUTH);

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

		xlsmOptions = new SpreadSheetOptionsPanel("xlsm");
		this.typePanel.add(xlsmOptions, "xlsm");

		this.typePanel.add(new JPanel(), "empty");

		if (poiAvailable)
		{
			xlsOptions = new SpreadSheetOptionsPanel("xls");
			this.typePanel.add(xlsOptions, "xls");
		}

		if (xlsxAvailable)
		{
			xlsxOptions = new SpreadSheetOptionsPanel("xlsx");
			typePanel.add(xlsxOptions, "xlsx");
		}

		this.add(typePanel, BorderLayout.CENTER);
		typeSelector.addActionListener(this);
	}

	public void updateSqlOptions(DataStore source)
	{
		WbConnection conn = source.getOriginalConnection();
		dataStoreColumns = (source == null ? null : source.getResultInfo());
		boolean insert = (source != null && source.canSaveAsSqlInsert());
		boolean update = (source != null && source.hasPkColumns());
		sqlOptions.setIncludeUpdate(update);
		sqlOptions.setIncludeDeleteInsert(insert && update);
		sqlOptions.setIncludeMerge(insert && update);
		if (conn != null)
		{
			sqlOptions.setDbId(conn.getDbId());
		}
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

	public void setIncludeMerge(boolean flag)
	{
		this.sqlOptions.setIncludeMerge(flag);
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
		this.xlsmOptions.saveSettings();
		if (this.xlsOptions != null)
		{
			this.xlsOptions.saveSettings();
		}
		if (this.xlsxOptions != null)
		{
			this.xlsxOptions.saveSettings();
		}
		Settings.getInstance().setProperty("workbench.export.type", this.currentType.getCode());
	}

	public void restoreSettings()
	{
		this.generalOptions.restoreSettings();
		this.sqlOptions.restoreSettings();
		this.textOptions.restoreSettings();
		this.htmlOptions.restoreSettings();
		this.xmlOptions.restoreSettings();
		this.odsOptions.restoreSettings();
		this.xlsmOptions.restoreSettings();
		if (this.xlsOptions != null)
		{
			this.xlsOptions.restoreSettings();
		}
		if (this.xlsxOptions != null)
		{
			this.xlsxOptions.restoreSettings();
		}
		String code = Settings.getInstance().getProperty("workbench.export.type", ExportType.TEXT.getCode());
		ExportType type = ExportType.getTypeFromCode(code);
		this.setExportType(type);
	}

	/**
	 *	Sets the displayed options according to
	 *  DataExporter.EXPORT_XXXX types
	 */
	public void setExportType(ExportType type)
	{
		if (type == null)
		{
			setTypeText();
			return;
		}
		switch (type)
		{
			case HTML:
				setTypeHtml();
				break;
			case SQL_INSERT:
			case SQL_UPDATE:
			case SQL_DELETE_INSERT:
			case SQL_MERGE:
				setTypeSql();
				break;
			case TEXT:
				setTypeText();
				break;
			case XML:
				setTypeXml();
				break;
			case ODS:
				setTypeOds();
				break;
			case JSON:
				setTypeJson();
				break;
			case XLSM:
				setTypeXlsM();
				break;
			case XLSX:
				if (xlsxAvailable) setTypeXlsX();
				break;
			case XLS:
				if (poiAvailable) setTypeXls();
				break;
		}
	}

	public ExportType getExportType()
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
		this.currentType = ExportType.TEXT;
		typeSelector.setSelectedItem("Text");
	}

	private void showSqlOptions()
	{
		this.card.show(this.typePanel, "sql");
	}

	public void setTypeSql()
	{
		showSqlOptions();
		this.currentType = ExportType.SQL_INSERT;
		typeSelector.setSelectedItem("SQL");
	}

	private void showXmlOptions()
	{
		this.card.show(this.typePanel, "xml");
	}

	public void setTypeXml()
	{
		showXmlOptions();
		this.currentType = ExportType.XML;
		typeSelector.setSelectedItem("XML");
	}

	private void showEmptyOptions()
	{
		this.card.show(this.typePanel, "empty");
	}

	private void showHtmlOptions()
	{
		this.card.show(this.typePanel, "html");
	}

	public void setTypeHtml()
	{
		showHtmlOptions();
		this.currentType = ExportType.HTML;
		typeSelector.setSelectedItem("HTML");
	}

	private void showOdsOptions()
	{
		this.card.show(this.typePanel, "ods");
	}

	public void setTypeOds()
	{
		showOdsOptions();
		this.currentType = ExportType.ODS;
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

	private void showXlsMOptions()
	{
		this.card.show(this.typePanel, "xlsm");
	}

	public void setTypeXls()
	{
		showXlsOptions();
		this.currentType = ExportType.XLS;
		typeSelector.setSelectedItem(XLS_ITEM);
	}

	public void setTypeXlsX()
	{
		showXlsXOptions();
		this.currentType = ExportType.XLSX;
		typeSelector.setSelectedItem(XLSX_ITEM);
	}

	public void setTypeJson()
	{
		this.card.show(this.typePanel, "empty");
		this.currentType = ExportType.JSON;
		typeSelector.setSelectedItem("JSON");
	}

	public void setTypeXlsM()
	{
		showXlsMOptions();
		this.currentType = ExportType.XLSM;
		typeSelector.setSelectedItem(XLSM_ITEM);
	}

	public SpreadSheetOptions getXlsMOptions()
	{
		return xlsmOptions;
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

	@Override
	public String getEncoding()
	{
		return generalOptions.getEncoding();
	}

	@Override
	public void setEncoding(String enc)
	{
		generalOptions.setEncoding(enc);
	}

	@Override
	public void actionPerformed(ActionEvent event)
	{
		if (event.getSource() == this.typeSelector)
		{
			Object item = typeSelector.getSelectedItem();
			String itemValue = item.toString();
			ExportType type = null;

			this.card.show(this.typePanel, itemValue);

			if ("text".equalsIgnoreCase(itemValue))
			{
				type = ExportType.TEXT;
				showTextOptions();
			}
			else if ("sql".equalsIgnoreCase(itemValue))
			{
				type = ExportType.SQL_INSERT;
				showSqlOptions();
			}
			else if ("xml".equalsIgnoreCase(itemValue))
			{
				type = ExportType.XML;
				showXmlOptions();
			}
			else if (item == ODS_ITEM)
			{
				type = ExportType.ODS;
				showOdsOptions();
			}
			else if (item == XLSX_ITEM && xlsxAvailable)
			{
				type = ExportType.XLSX;
				showXlsXOptions();
			}
			else if (item == XLS_ITEM && poiAvailable)
			{
				type = ExportType.XLS;
				showXlsOptions();
			}
			else if (item == XLSM_ITEM)
			{
				type = ExportType.XLSM;
				showXlsMOptions();
			}
			else if ("html".equalsIgnoreCase(itemValue))
			{
				type = ExportType.HTML;
				showHtmlOptions();
			}
			else if ("json".equalsIgnoreCase(itemValue))
			{
				type = ExportType.JSON;
				showEmptyOptions();
			}

			this.currentType = type;
			firePropertyChange("exportType", null, type);
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
