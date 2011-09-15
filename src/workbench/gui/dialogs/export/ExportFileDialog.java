/*
 * ExportFileDialog.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

import workbench.gui.components.WbFileChooser;
import java.awt.Component;
import java.awt.EventQueue;
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
import workbench.db.exporter.ExportType;
import workbench.db.exporter.PoiHelper;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.FeedbackWindow;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;


/**
 * @author Thomas Kellerer
 */
public class ExportFileDialog
	implements PropertyChangeListener
{
	private ExportType exportType = null;
	private String selectedFilename = null;
	private boolean isCancelled = false;
	private Settings settings = Settings.getInstance();
	private ExportOptionsPanel exportOptions;
	private WbFileChooser chooser;
	private boolean filterChange = false;
	private boolean selectDirectory = false;
	private boolean includeSqlUpdate = true;
	private boolean includeSqlInsert = true;
	private boolean includeSqlDeleteInsert = true;
	private String lastDirConfigKey = "workbench.export.lastdir";
	private final DataStore source;
	private boolean sqlChecked = false;

	private Component parentComponent;

	public ExportFileDialog(Component caller)
	{
		source = null;
		exportOptions = new ExportOptionsPanel(null);
		parentComponent = caller;
	}

	public ExportFileDialog(Component caller, DataStore ds)
	{
		source = ds;
		exportOptions = new ExportOptionsPanel(source == null ? null : source.getResultInfo());
		parentComponent = caller;
	}

	public ExportFileDialog(Component caller, ResultInfo info)
	{
		source = null;
		exportOptions = new ExportOptionsPanel(info);
		parentComponent = caller;
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

	public SpreadSheetOptions getXlsMOptions()
	{
		return exportOptions.getXlsMOptions();
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

	public ExportType getExportType()
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

		if (includeSqlDeleteInsert)
		{
			fc.addChoosableFileFilter(ExtensionFileFilter.getSqlInsertDeleteFilter());
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
		this.exportType = null;
		this.selectedFilename = null;
		boolean result = false;

		String lastDir = settings.getProperty(lastDirConfigKey, null);
		this.chooser = new WbFileChooser(lastDir);
		chooser.setSettingsID("workbench.export.saveas");
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

		if (exportOptions.getExportType().isSqlType())
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					checkSqlOptions();
				}
			});
		}
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

		switch (this.exportType)
		{
			case SQL_INSERT:
			case SQL_UPDATE:
			case SQL_DELETE_INSERT:
				exporter.setSqlOptions(this.getSqlOptions());
				break;
			case TEXT:
				exporter.setTextOptions(this.getTextOptions());
				break;
			case HTML:
				exporter.setHtmlOptions(this.getHtmlOptions());
				break;
			case XML:
				exporter.setXmlOptions(this.getXmlOptions());
				break;
			case ODS:
				exporter.setOdsOptions(getOdsOptions());
				break;
			case XLSX:
				exporter.setXlsXOptions(getXlsXOptions());
				break;
			case XLSM:
				exporter.setXlsMOptions(getXlsMOptions());
				break;
			case XLS:
				exporter.setXlsOptions(getXlsOptions());
				break;
			default:
				exporter.setTextOptions(this.getTextOptions());
				LogMgr.logWarning("ExportFileDialog.setExporterOptions()", "Unknown file type selected", null);
				break;
		}
	}

	private ExportType getExportType(ExtensionFileFilter ff)
	{
		if (ff.hasFilter(ExtensionFileFilter.SQL_EXT))
		{
			return ExportType.SQL_INSERT;
		}
		else if (ff.hasFilter(ExtensionFileFilter.XML_EXT))
		{
			return ExportType.XML;
		}
		else if (ff.hasFilter(ExtensionFileFilter.TXT_EXT))
		{
			return ExportType.TEXT;
		}
		else if (ff.hasFilter(ExtensionFileFilter.HTML_EXT))
		{
			return ExportType.HTML;
		}
		else if (ff.hasFilter(ExtensionFileFilter.XLS_EXT))
		{
			return ExportType.XLS;
		}
		else if (ff.hasFilter(ExtensionFileFilter.XLSX_EXT))
		{
			return ExportType.XLSX;
		}
		else if (ff.hasFilter(ExtensionFileFilter.XLSM_EXT))
		{
			return ExportType.XLSM;
		}
		else if (ff.hasFilter(ExtensionFileFilter.ODS_EXT))
		{
			return ExportType.ODS;
		}
		return null;
	}

	private FeedbackWindow checkWindow;

	protected void checkSqlOptions()
	{
		if (sqlChecked) return;
		if (source == null) return;
		if (source.hasPkColumns()) return;

		if (checkWindow != null) return;
		if (chooser == null) return;

		Runnable task = new Runnable()
		{
			@Override
			public void run()
			{
				_checkSqlOptions();
			}
		};

		checkWindow = new FeedbackWindow(chooser.getCurrentDialog(), ResourceMgr.getString("MsgRetrievingKeyColumns"));
		WbSwingUtilities.center(checkWindow, chooser.getCurrentDialog());
		WbSwingUtilities.showWaitCursor(chooser.getCurrentDialog());
		WbSwingUtilities.showWaitCursor(exportOptions);
		checkWindow.showAndStart(task);
	}

	private void updateSqlFlags()
	{
		exportOptions.updateSqlOptions(source);
		includeSqlInsert = (source != null && source.canSaveAsSqlInsert());
		includeSqlUpdate = (source != null && source.hasPkColumns());
		includeSqlDeleteInsert = (includeSqlInsert && includeSqlUpdate);

		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (!includeSqlUpdate)
				{
					chooser.removeChoosableFileFilter(ExtensionFileFilter.getSqlUpdateFileFilter());
				}

				if (!includeSqlDeleteInsert)
				{
					chooser.removeChoosableFileFilter(ExtensionFileFilter.getSqlInsertDeleteFilter());
				}
			}
		});
	}

	protected void _checkSqlOptions()
	{
		if (source == null) return;

		try
		{
			sqlChecked = true;
			source.updatePkInformation();
			updateSqlFlags();
		}
		catch (Exception sql)
		{
			LogMgr.logError("ExportFileDialog.checkSqlOptions()", "Error checking SQL Options", sql);
			WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(sql));
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(exportOptions);
			WbSwingUtilities.showDefaultCursor(chooser.getCurrentDialog());
			if (checkWindow != null)
			{
				checkWindow.setVisible(false);
				checkWindow.dispose();
				checkWindow = null;
			}
		}
	}

	@Override
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
				ExportType type = this.getExportType(eff);
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

				ExportType type = (ExportType)evt.getNewValue();
				this.filterChange = true;

				switch (type)
				{
					case SQL_INSERT:
					case SQL_UPDATE:
					case SQL_DELETE_INSERT:
						checkSqlOptions();
						this.chooser.setFileFilter(ExtensionFileFilter.getSqlFileFilter());
						break;
					case HTML:
						this.chooser.setFileFilter(ExtensionFileFilter.getHtmlFileFilter());
						break;
					case XML:
						this.chooser.setFileFilter(ExtensionFileFilter.getXmlFileFilter());
						break;
					case TEXT:
						this.chooser.setFileFilter(ExtensionFileFilter.getTextFileFilter());
						break;
					case XLS:
						this.chooser.setFileFilter(ExtensionFileFilter.getXlsFileFilter());
						break;
					case XLSX:
						this.chooser.setFileFilter(ExtensionFileFilter.getXlsXFileFilter());
						break;
					case XLSM:
						this.chooser.setFileFilter(ExtensionFileFilter.getXlsMFileFilter());
						break;
					case ODS:
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
