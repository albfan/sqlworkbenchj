/*
 * ExportFileDialog.java
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

import java.awt.Component;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import workbench.db.exporter.DataExporter;
import workbench.db.exporter.PoiHelper;
import workbench.gui.components.ExtensionFileFilter;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;
import workbench.util.StringUtil;


/**
 * @author support@sql-workbench.net
 */
public class ExportFileDialog
	implements PropertyChangeListener
{
	private int exportType = -1;
	private String selectedFilename = null;
	private boolean isCancelled = false;
	private Settings settings = Settings.getInstance();
	private ExportOptionsPanel exportOptions;
	private JFileChooser chooser;
	private boolean filterChange = false;
	private boolean selectDirectory = false;
	private boolean includeSqlUpdate = true;
	private boolean includeSqlInsert = true;
	private boolean includeSqlDeleteInsert = true;
	private String lastDirConfigKey = "workbench.export.lastdir";
	
	private Component parentComponent;
	
	public ExportFileDialog(Component caller)
	{
		this(caller, null);
	}
	
	public ExportFileDialog(Component caller, ResultInfo columns)
	{
		this.exportOptions = new ExportOptionsPanel(columns);
		this.parentComponent = caller;
	}

	public void setQuerySql(String sql, WbConnection con)
	{
		this.exportOptions.setQuerySql(sql, con);
	}
	
	public List<ColumnIdentifier> getColumnsToExport()
	{
		return this.exportOptions.getColumnsToExport();
	}
	
	public void saveSettings()
	{
		exportOptions.saveSettings();
	}
	
	public void restoreSettings()
	{
		exportOptions.restoreSettings();
	}

	public SpreadSheetOptions getOdsOptions()
	{
		return exportOptions.getOdsOptions();
	}

	public SpreadSheetOptions getXlsOptions()
	{
		return exportOptions.getXlsOptions();
	}

	public SpreadSheetOptions getXlsXOptions()
	{
		return exportOptions.getXlsXOptions();
	}
	
	public SqlOptions getSqlOptions()
	{
		return exportOptions.getSqlOptions();
	}
	
	public HtmlOptions getHtmlOptions()
	{
		return exportOptions.getHtmlOptions();
	}
	
	public TextOptions getTextOptions()
	{
		return exportOptions.getTextOptions();
	}
	
	public XmlOptions getXmlOptions()
	{
		return exportOptions.getXmlOptions();
	}
	
	public ExportOptions getBasicExportOptions()
	{
		return exportOptions.getExportOptions();
	}
	
	public String getSelectedFilename()
	{
		return this.selectedFilename;
	}
	
	public int getExportType()
	{
		return this.exportType;
	}

	public boolean isCancelled()
	{
		return this.isCancelled;
	}
	
	public void setIncludeSqlUpdate(boolean flag)
	{
		this.includeSqlUpdate = flag;
	}
	
	public void setIncludeSqlInsert(boolean flag)
	{
		this.includeSqlInsert = flag;
	}
	
	public void setIncludeSqlDeleteInsert(boolean flag)
	{
		this.includeSqlDeleteInsert = flag;
	}
	
	/**
	 *	Set the config key for the Settings object
	 *  where the selected directory should be stored
	 */
	public void setConfigKey(String key)
	{
		this.lastDirConfigKey = key;
	}
	
	private void setupFileFilters(JFileChooser fc)
	{
		fc.addChoosableFileFilter(ExtensionFileFilter.getTextFileFilter());
		fc.addChoosableFileFilter(ExtensionFileFilter.getHtmlFileFilter());
		if (includeSqlInsert)
		{
			fc.addChoosableFileFilter(ExtensionFileFilter.getSqlFileFilter());
		}
		if (includeSqlUpdate) 
		{
			fc.addChoosableFileFilter(ExtensionFileFilter.getSqlUpdateFileFilter());
		}
		fc.addChoosableFileFilter(ExtensionFileFilter.getXmlFileFilter());
		fc.addChoosableFileFilter(ExtensionFileFilter.getOdsFileFilter());
		if (PoiHelper.isPoiAvailable())
		{
			fc.addChoosableFileFilter(ExtensionFileFilter.getXlsFileFilter());
		}
	}

	public void setSelectDirectoryOnly(boolean flag)
	{
		this.selectDirectory = flag;
	}
	
	public boolean selectOutput()
	{
		return this.selectOutput(null);
	}
	
	public boolean selectOutput(String title)
	{
		this.exportType = -1;
		this.selectedFilename = null;
		boolean result = false;
		
		String lastDir = settings.getProperty(lastDirConfigKey, null);
		this.chooser = new JFileChooser(lastDir);
		if (title != null) this.chooser.setDialogTitle(title);
		
		if (this.selectDirectory)
		{
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		else
		{
			setupFileFilters(chooser);
			chooser.addPropertyChangeListener("fileFilterChanged", this);
			chooser.setFileFilter(ExtensionFileFilter.getTextFileFilter());
		}
		this.exportOptions.addPropertyChangeListener("exportType", this);
		this.restoreSettings();
			
		chooser.setAccessory(this.exportOptions);
		
		Window parentWindow = SwingUtilities.getWindowAncestor(this.parentComponent);

		int answer = chooser.showSaveDialog(parentWindow);
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			String filename = null;
			this.isCancelled = false;
			File fl = chooser.getSelectedFile();
			
			if (this.selectDirectory)
			{
				filename = fl.getAbsolutePath();
				this.exportType = this.exportOptions.getExportType();
				lastDir = filename;
			}
			else
			{
				FileFilter ff = chooser.getFileFilter();
				if (ff instanceof ExtensionFileFilter)
				{
					ExtensionFileFilter eff = (ExtensionFileFilter)ff;
					filename = fl.getAbsolutePath();

					String ext = ExtensionFileFilter.getExtension(fl);
					if (StringUtil.isEmptyString(ext))
					{
						if (!filename.endsWith(".")) filename = filename + ".";
						filename = filename + eff.getDefaultExtension();
					}
					this.exportType = this.getExportType(eff);
				}
				else
				{
					filename = fl.getAbsolutePath();
					this.exportType = this.exportOptions.getExportType();
				}
				lastDir = chooser.getCurrentDirectory().getAbsolutePath();
			}
			settings.setProperty(this.lastDirConfigKey, lastDir);
			this.saveSettings();
			this.selectedFilename = filename;
			result = true;
		}
		else
		{
			this.isCancelled = true;
			result = false;
		}
		return result;
	}

	public void setExporterOptions(DataExporter exporter)
	{
		exporter.setOptions(this.getBasicExportOptions());
		exporter.setOutputFilename(this.getSelectedFilename());
		
		switch (this.exportType)
		{
			case DataExporter.EXPORT_SQL:
				exporter.setSqlOptions(this.getSqlOptions());
				break;
			case DataExporter.EXPORT_TXT:
				exporter.setTextOptions(this.getTextOptions());
				break;
			case DataExporter.EXPORT_HTML:
				exporter.setHtmlOptions(this.getHtmlOptions());
				break;
			case DataExporter.EXPORT_XML:
				exporter.setXmlOptions(this.getXmlOptions());
				break;
			case DataExporter.EXPORT_ODS:
				exporter.setOdsOptions(getOdsOptions());
				break;
			case DataExporter.EXPORT_XLSX:
				exporter.setXlsXOptions(getXlsXOptions());
				break;
			case DataExporter.EXPORT_XLS:
				exporter.setXlsOptions(getXlsOptions());
				break;
			default:
				exporter.setTextOptions(this.getTextOptions());
				LogMgr.logWarning("ExportFileDialog.setExporterOptions()", "Unknown file type selected", null);
				break;
		}
	}
	
	private int getExportType(ExtensionFileFilter ff)
	{
		if (ff.hasFilter(ExtensionFileFilter.SQL_EXT))
		{
			return DataExporter.EXPORT_SQL;
		}
		else if (ff.hasFilter(ExtensionFileFilter.XML_EXT))
		{
			return DataExporter.EXPORT_XML;
		}
		else if (ff.hasFilter(ExtensionFileFilter.TXT_EXT))
		{
			return DataExporter.EXPORT_TXT;
		}
		else if (ff.hasFilter(ExtensionFileFilter.HTML_EXT))
		{
			return DataExporter.EXPORT_HTML;
		}
		else if (ff.hasFilter(ExtensionFileFilter.XLS_EXT))
		{
			return DataExporter.EXPORT_XLS;
		}
		else if (ff.hasFilter(ExtensionFileFilter.XLSX_EXT))
		{
			return DataExporter.EXPORT_XLSX;
		}
		else if (ff.hasFilter(ExtensionFileFilter.ODS_EXT))
		{
			return DataExporter.EXPORT_ODS;
		}
		return -1;
	}
	
	public void propertyChange(PropertyChangeEvent evt) 
	{
		if (this.exportOptions == null) return;
		
		if (evt.getSource() instanceof JFileChooser && !filterChange && !this.selectDirectory)
		{
			JFileChooser fc = (JFileChooser)evt.getSource();
			FileFilter ff = fc.getFileFilter();
			if (ff instanceof ExtensionFileFilter)
			{
				ExtensionFileFilter eff = (ExtensionFileFilter)ff;
				int type = this.getExportType(eff);
				this.exportOptions.setExportType(type);
			}
		}
		else if (evt.getSource() == this.exportOptions && this.chooser != null && !this.selectDirectory)
		{
			try
			{
				FileFilter ff = this.chooser.getFileFilter();
				// check for All file (*.*) filter. In that
				// case we do not change the current filter.
				if (!(ff instanceof ExtensionFileFilter)) return;
				
				Integer newvalue = (Integer)evt.getNewValue();
				int type = (newvalue == null ? -1 : newvalue.intValue());
				this.filterChange = true;
				
				switch (type)
				{
					case DataExporter.EXPORT_SQL:
						this.chooser.setFileFilter(ExtensionFileFilter.getSqlFileFilter());
						break;
					case DataExporter.EXPORT_HTML:
						this.chooser.setFileFilter(ExtensionFileFilter.getHtmlFileFilter());
						break;
					case DataExporter.EXPORT_XML:
						this.chooser.setFileFilter(ExtensionFileFilter.getXmlFileFilter());
						break;
					case DataExporter.EXPORT_TXT:
						this.chooser.setFileFilter(ExtensionFileFilter.getTextFileFilter());
						break;
					case DataExporter.EXPORT_XLS:
						this.chooser.setFileFilter(ExtensionFileFilter.getXlsFileFilter());
						break;
					case DataExporter.EXPORT_XLSX:
						this.chooser.setFileFilter(ExtensionFileFilter.getXlsXFileFilter());
						break;
					case DataExporter.EXPORT_ODS:
						this.chooser.setFileFilter(ExtensionFileFilter.getOdsFileFilter());
						break;
				}
			}
			catch (Throwable th)
			{
				th.printStackTrace();
			}
			finally 
			{
				this.filterChange = false;
			}
		}
	}

}
