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
package workbench.util;

import java.awt.Component;
import java.awt.Window;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import workbench.gui.components.ExtensionFileFilter;
import workbench.resource.Settings;

/**
 *
 * @author info@sql-workbench.net
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
	
	/** Creates a new instance of FileDialogUtil */
	public FileDialogUtil()
	{
	}
	public String getExportFilename(boolean includeSqlType)
	{
		return this.getExportFilename(null, includeSqlType);
	}

	public int getLastSelectedFileType()
	{
		return this.lastFileType;
	}

	public String getExportFilename(Component caller, boolean includeSqlType)
	{
		FileFilter text = ExtensionFileFilter.getTextFileFilter();
		FileFilter[] filters;
		int index = 0;
		if (includeSqlType)
		{
			filters = new FileFilter[5];
		}
		else
		{
			filters = new FileFilter[3];
		}

		filters[index++] = text;
		filters[index++] = ExtensionFileFilter.getHtmlFileFilter();
		if (includeSqlType)
		{
			filters[index++] = ExtensionFileFilter.getSqlFileFilter();
			filters[index++] = ExtensionFileFilter.getSqlUpdateFileFilter();
		}
		filters[index++] = ExtensionFileFilter.getXmlFileFilter();
		String lastDir = settings.getLastExportDir();
		return getFilename(caller, filters, 0, "workbench.export.lastdir");
	}

	public String getXmlReportFilename(Component caller)
	{
		FileFilter[] filter = new FileFilter[1];
		filter[0] = ExtensionFileFilter.getXmlFileFilter();
		return getFilename(caller, filter, 0, "workbench.xmlreport.lastdir");
	}

	private String getFilename(Component caller, FileFilter[] filters, int defaultFilter, String dirProperty)
	{
		this.lastFileType = FILE_TYPE_UNKNOWN;
		String lastDir = settings.getProperty(dirProperty, null);
		JFileChooser fc = new JFileChooser(lastDir);
		for (int i=0; i < filters.length; i++)
		{
			fc.addChoosableFileFilter(filters[i]);
		}
		fc.setFileFilter(filters[defaultFilter]);
		String filename = null;

		Window parent;
		parent = SwingUtilities.getWindowAncestor(caller);

		int answer = fc.showSaveDialog(parent);
		if (answer == JFileChooser.APPROVE_OPTION)
		{
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
				if (ff == ExtensionFileFilter.getSqlFileFilter())
				{
					this.lastFileType = FILE_TYPE_SQL;
				}
				else if (ff == ExtensionFileFilter.getSqlUpdateFileFilter())
				{
					this.lastFileType = FILE_TYPE_SQL_UPDATE;
				}
				else if (ff == ExtensionFileFilter.getXmlFileFilter())
				{
					this.lastFileType = FILE_TYPE_XML;
				}
				else if (ff == ExtensionFileFilter.getTextFileFilter())
				{
					this.lastFileType = FILE_TYPE_TXT;
				}
				else if (ff == ExtensionFileFilter.getHtmlFileFilter())
				{
					this.lastFileType = FILE_TYPE_HTML;
				}
			}
			else
			{
				filename = fl.getAbsolutePath();
			}

			lastDir = fc.getCurrentDirectory().getAbsolutePath();
			settings.setProperty(dirProperty, lastDir);
		}

		return filename;
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
