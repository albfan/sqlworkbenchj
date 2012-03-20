/*
 * ExtensionFileFilter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import javax.swing.filechooser.FileFilter;
import workbench.db.exporter.ExportType;

import workbench.resource.ResourceMgr;
import workbench.util.WbFile;

/**
 *
 * @author  Thomas Kellerer
 */
public class ExtensionFileFilter
	extends FileFilter
{
	// The created FileFilters are stored in variables
	// as in some cases it is necessary to access the
	// instance (e.g. for JFileChooser.setFileFilter()
	private static Map<String, ExtensionFileFilter> filters = new HashMap<String, ExtensionFileFilter>();
	private static FileFilter jarFileFilter;

	private List<String> extensions;
	private String desc;
	public static final String SQL_EXT = "sql";
	public static final String TXT_EXT = "txt";
	public static final String WORKSPACE_EXT = "wksp";
	public static final String XML_EXT = "xml";
	public static final String HTML_EXT = "html";
	public static final String XLS_EXT = "xls";
	public static final String XLSX_EXT = "xlsx";
	public static final String XLSM_EXT = "xml";
	public static final String ODS_EXT = "ods";

	private boolean ignoreCase = true;
	private ExportType exportType;

	public ExtensionFileFilter(String aDescription, String extension, boolean ignore)
	{
		super();
		this.desc = aDescription;
		this.extensions = new ArrayList<String>();
		this.extensions.add(extension);
		this.ignoreCase = ignore;
	}

	public ExtensionFileFilter(String aDescription, List<String> anExtensionList)
	{
		this(aDescription, anExtensionList, true);
	}

	public ExtensionFileFilter(String aDescription, List<String> anExtensionList, boolean ignoreCase)
	{
		super();
		this.desc = aDescription;
		this.extensions = anExtensionList;
		this.ignoreCase = ignoreCase;
	}

	public ExportType getExportType()
	{
		return exportType;
	}

	public boolean hasFilter(String extension)
	{
		return this.extensions.contains(extension);
	}

	public String getDefaultExtension()
	{
		return this.extensions.get(0);
	}

	public static String getExtension(File f)
	{
		return getExtension(f.getName());
	}

	public static String getExtension(String s)
	{
		WbFile f = new WbFile(s);
		return f.getExtension();
	}

	public static boolean hasSqlExtension(String aFilename)
	{
		String ext = getExtension(aFilename);
		return SQL_EXT.equalsIgnoreCase(ext);
	}

	public static boolean hasTxtExtension(String aFilename)
	{
		String ext = getExtension(aFilename);
		return TXT_EXT.equalsIgnoreCase(ext);
	}

	public static boolean hasHtmlExtension(String aFilename)
	{
		String ext = getExtension(aFilename);
		return "html".equalsIgnoreCase(ext);
	}

	public static boolean hasXmlExtension(String aFilename)
	{
		String ext = getExtension(aFilename);
		return XML_EXT.equalsIgnoreCase(ext);
	}

	@Override
	public boolean accept(File f)
	{
		if (f.isDirectory())
		{
			return true;
		}
		if (this.extensions == null || this.extensions.isEmpty()) return true;

		String extension = getExtension(f);
		if (extension == null) return false;

		if (this.extensions.contains(extension))
		{
			return true;
		}
		else if (this.ignoreCase)
		{
			for (int i=0; i < this.extensions.size(); i ++)
			{
				if (extension.equalsIgnoreCase(this.extensions.get(i))) return true;
			}
		}

		return false;
	}

	public static FileFilter getJarFileFilter()
	{
		if (jarFileFilter == null)
		{
			ArrayList<String> ext = new ArrayList<String>();
			ext.add("jar");
			ext.add("zip");
			String desc = ResourceMgr.getString("TxtArchivesFilterName");
			jarFileFilter = new ExtensionFileFilter(desc, ext, true);
		}
		return jarFileFilter;
	}

	public static FileFilter getSqlFileFilter()
	{
		return getFileFilter(SQL_EXT, "TxtFileFilterSql", ExportType.SQL_INSERT);
	}

	public static FileFilter getSqlInsertDeleteFilter()
	{
		return getFileFilter(SQL_EXT, "TxtFileFilterSqlInsDel", ExportType.SQL_DELETE_INSERT);
	}

	public static FileFilter getSqlUpdateFileFilter()
	{
		return getFileFilter(SQL_EXT, "TxtFileFilterSqlInsDel", ExportType.SQL_UPDATE);
	}

	public static FileFilter getTextFileFilter()
	{
		return getFileFilter(TXT_EXT, "TxtFileFilterText", ExportType.TEXT);
	}

	public static FileFilter getXmlFileFilter()
	{
		return getFileFilter(XML_EXT, "TxtFileFilterXml", ExportType.XML);
	}

	public static FileFilter getWorkspaceFileFilter()
	{
		return getFileFilter(WORKSPACE_EXT, "TxtFileFilterWksp", null);
	}

	public static FileFilter getHtmlFileFilter()
	{
		return getFileFilter(HTML_EXT, "TxtFileFilterHtml", ExportType.HTML);
	}

	public static FileFilter getXlsFileFilter()
	{
		return getFileFilter(XLS_EXT, "TxtFileFilterXls", ExportType.XLS);
	}

	public static FileFilter getXlsXFileFilter()
	{
		return getFileFilter(XLSX_EXT, "TxtFileFilterXlsX", ExportType.XLSX);
	}

	public static FileFilter getXlsMFileFilter()
	{
		 return getFileFilter(XLSM_EXT, "TxtFileFilterXlsM", ExportType.XLSM);
	}

	public static FileFilter getOdsFileFilter()
	{
		return getFileFilter(ODS_EXT, "TxtFileFilterOds", ExportType.ODS);
	}

	private static ExtensionFileFilter getFileFilter(String ext, String key, ExportType type)
	{
		ExtensionFileFilter ff = filters.get(key);
		if (ff == null)
		{
			String desc = ResourceMgr.getString(key);
			ff = new ExtensionFileFilter(desc, ext, true);
			ff.exportType = type;
			filters.put(key, ff);
		}
		return ff;
	}

	@Override
	public String getDescription()
	{
		return this.desc;
	}
}
