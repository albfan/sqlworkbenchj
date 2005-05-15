/*
 * FileDialogUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import workbench.db.exporter.DataExporter;
import workbench.gui.components.EncodingPanel;

import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.dialogs.export.ExportOptionsPanel;
import workbench.interfaces.EncodingSelector;
import workbench.resource.Settings;
import javax.swing.JComponent;
//import workbench.gui.components.ExportOptionsPanel;

/**
 *
 * @author support@sql-workbench.net
 */
public class FileDialogUtil
{
	public static final int FILE_TYPE_UNKNOWN = -1;
	public static final int FILE_TYPE_TXT = 0;
	public static final int FILE_TYPE_SQL = 1;
	public static final int FILE_TYPE_XML = 2;
	public static final int FILE_TYPE_HTML = 3;
	public static final int FILE_TYPE_SQL_UPDATE = 4;

	private int lastFileType = FILE_TYPE_UNKNOWN;
	private Settings settings = Settings.getInstance();
	private static final String CONFIG_DIR_KEY = "%ConfigDir%";
	private String encoding = null;
	private ExportOptionsPanel exportOptions;
	private JFileChooser chooser;
	private boolean filterChange = false;

	/** Creates a new instance of FileDialogUtil */
	public FileDialogUtil()
	{
	}

	public int getLastSelectedFileType()
	{
		return this.lastFileType;
	}

	public String getEncoding()
	{
		return this.encoding;
	}

	public String getXmlReportFilename(Component caller)
	{
		return this.getXmlReportFilename(caller, null);
	}

	public String getXmlReportFilename(Component caller, JComponent accessory)
	{
		String lastDir = settings.getProperty("workbench.xmlreport.lastdir", null);
		JFileChooser fc = new JFileChooser(lastDir);
		fc.addChoosableFileFilter(ExtensionFileFilter.getXmlFileFilter());
		fc.setFileFilter(ExtensionFileFilter.getXmlFileFilter());

		if (this.encoding == null)
		{
			this.encoding = this.settings.getDefaultDataEncoding();
		}

		EncodingSelector selector = null;
		if (accessory != null)
		{
			fc.setAccessory(accessory);
			if (accessory instanceof EncodingSelector)
			{
				selector = (EncodingSelector)accessory;
			}
			selector.setEncoding(this.encoding);
		}
		else
		{
			EncodingPanel p = new EncodingPanel(this.encoding);
			selector = p;
			fc.setAccessory(p);
		}

		String filename = null;

		Window parent = SwingUtilities.getWindowAncestor(caller);

		int answer = fc.showSaveDialog(parent);
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			this.encoding = selector.getEncoding();

			File fl = fc.getSelectedFile();
			FileFilter ff = fc.getFileFilter();
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
				this.lastFileType = this.getFileFilterType(ff);
			}
			else
			{
				filename = fl.getAbsolutePath();
			}

			lastDir = fc.getCurrentDirectory().getAbsolutePath();
			settings.setProperty("workbench.xmlreport.lastdir", lastDir);
		}

		return filename;
	}

	private int getFileFilterType(FileFilter ff)
	{
		if (ff == ExtensionFileFilter.getSqlFileFilter())
		{
			return FILE_TYPE_SQL;
		}
		else if (ff == ExtensionFileFilter.getSqlUpdateFileFilter())
		{
			return FILE_TYPE_SQL_UPDATE;
		}
		else if (ff == ExtensionFileFilter.getXmlFileFilter())
		{
			return FILE_TYPE_XML;
		}
		else if (ff == ExtensionFileFilter.getTextFileFilter())
		{
			return FILE_TYPE_TXT;
		}
		else if (ff == ExtensionFileFilter.getHtmlFileFilter())
		{
			return FILE_TYPE_HTML;
		}
		return FILE_TYPE_UNKNOWN;
	}

	public String getWorkspaceFilename(Window parent, boolean toSave)
	{
		return this.getWorkspaceFilename(parent, toSave, false);
	}

	public String getWorkspaceFilename(Window parent, boolean toSave, boolean replaceConfigDir)
	{
		String lastDir = settings.getLastWorkspaceDir();
		JFileChooser fc = new JFileChooser(lastDir);
		FileFilter wksp = ExtensionFileFilter.getWorkspaceFileFilter();
		fc.addChoosableFileFilter(wksp);
		String filename = null;

		int answer = JFileChooser.CANCEL_OPTION;
		if (toSave)
		{
			answer = fc.showSaveDialog(parent);
		}
		else
		{
			answer = fc.showOpenDialog(parent);
		}
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			File fl = fc.getSelectedFile();
			FileFilter ff = fc.getFileFilter();
			if (ff == wksp)
			{
				filename = fl.getAbsolutePath();

				String ext = ExtensionFileFilter.getExtension(fl);
				if (ext.length() == 0)
				{
					if (!filename.endsWith(".")) filename = filename + ".";
					filename = filename + ExtensionFileFilter.WORKSPACE_EXT;
				}
			}
			else
			{
				filename = fl.getAbsolutePath();
			}

			lastDir = fc.getCurrentDirectory().getAbsolutePath();
			settings.setLastWorkspaceDir(lastDir);
		}
		if (replaceConfigDir && filename != null)
		{
			filename = this.putConfigDirKey(filename);
		}
		return filename;
	}

	public String putConfigDirKey(String aPathname)
	{
		File f = new File(aPathname);
		String fname = f.getName();
		File dir = f.getParentFile();
		File config = new File(this.settings.getConfigDir());
		if (dir.equals(config))
		{
			return CONFIG_DIR_KEY + StringUtil.FILE_SEPARATOR + fname;
		}
		else
		{
			return aPathname;
		}
	}

	public String replaceConfigDir(String aPathname)
	{
		if (aPathname == null) return null;
		String dir = this.settings.getConfigDir();
		return StringUtil.replace(aPathname, CONFIG_DIR_KEY, dir);
	}

}
