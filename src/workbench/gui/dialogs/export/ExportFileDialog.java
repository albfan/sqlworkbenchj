/*
 * FileDialogUtil.java
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

import java.awt.Component;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import workbench.db.exporter.DataExporter;

import workbench.gui.components.ExtensionFileFilter;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;


/**
 * @author info@sql-workbench.net
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

	public List getColumnsToExport()
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
			this.exportOptions.addPropertyChangeListener("exportType", this);
		}
		this.restoreSettings();
		this.exportOptions.setTypeText();
		this.exportOptions.setIncludeSqlUpdate(includeSqlUpdate);
		this.exportOptions.setIncludeSqlDeleteInsert(includeSqlDeleteInsert);
			
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
					if (ext.length() == 0)
					{
						if (!filename.endsWith(".")) filename = filename + ".";
						filename = filename + eff.getDefaultExtension();
					}
					this.exportType = this.getExportType(ff);
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
				exporter.setOutputTypeXml();
				exporter.setXmlOptions(this.getXmlOptions());
				break;
			default:
				exporter.setOutputTypeText();
				exporter.setTextOptions(this.getTextOptions());
				LogMgr.logWarning("ExportFileDialog.setExporterOptions()", "Unknown file type selected", null);
				break;
		}
	}
	
	private int getExportType(FileFilter ff)
	{
		if (ff == ExtensionFileFilter.getSqlFileFilter())
		{
			return DataExporter.EXPORT_SQL;
		}
		else if (ff == ExtensionFileFilter.getXmlFileFilter())
		{
			return DataExporter.EXPORT_XML;
		}
		else if (ff == ExtensionFileFilter.getTextFileFilter())
		{
			return DataExporter.EXPORT_TXT;
		}
		else if (ff == ExtensionFileFilter.getHtmlFileFilter())
		{
			return DataExporter.EXPORT_HTML;
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