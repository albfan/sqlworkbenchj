/*
 * ExtensionFileFilter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
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
	private static Map<String, FileFilter> filters = new HashMap<String, FileFilter>();
	private static FileFilter jarFileFilter;
	private static FileFilter sqlUpdateFileFilter;
	private static FileFilter sqlInsertDeleteFilter;

	private List<String> extensions;
	private String desc;
	public static final String SQL_EXT = "sql";
	public static final String TXT_EXT = "txt";
	public static final String WORKSPACE_EXT = "wksp";
	public static final String XML_EXT = "xml";
	public static final String HTML_EXT = "html";
	public static final String XLS_EXT = "xls";
	public static final String XLSX_EXT = "xlsx";
	public static final String XLSM_EXT = "xlsm";
	public static final String ODS_EXT = "ods";

	private boolean ignoreCase = true;

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

	public boolean accept(File f)
	{
		if (f.isDirectory())
		{
			return true;
		}
		if (this.extensions == null || this.extensions.size() == 0) return true;

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
		return getFileFilter(SQL_EXT, "TxtFileFilterSql");
	}

	public static FileFilter getSqlInsertDeleteFilter()
	{
		if (sqlInsertDeleteFilter == null)
		{
			String desc = ResourceMgr.getString("TxtFileFilterSqlInsDel");
			sqlInsertDeleteFilter = new ExtensionFileFilter(desc, SQL_EXT, true);
		}
		return sqlInsertDeleteFilter;
	}

	public static FileFilter getSqlUpdateFileFilter()
	{
		if (sqlUpdateFileFilter == null)
		{
			String desc = ResourceMgr.getString("TxtFileFilterSqlUpdate");
			sqlUpdateFileFilter = new ExtensionFileFilter(desc, SQL_EXT, true);
		}
		return sqlUpdateFileFilter;
	}

	public static FileFilter getTextFileFilter()
	{
		return getFileFilter(TXT_EXT, "TxtFileFilterText");
	}

	public static FileFilter getXmlFileFilter()
	{
		return getFileFilter(XML_EXT, "TxtFileFilterXml");
	}

	public static FileFilter getWorkspaceFileFilter()
	{
		return getFileFilter(WORKSPACE_EXT, "TxtFileFilterWksp");
	}

	public static FileFilter getHtmlFileFilter()
	{
		return getFileFilter(HTML_EXT, "TxtFileFilterHtml");
	}

	public static FileFilter getXlsFileFilter()
	{
		return getFileFilter(XLS_EXT, "TxtFileFilterXls");
	}

	public static FileFilter getXlsXFileFilter()
	{
		return getFileFilter(XLSX_EXT, "TxtFileFilterXlsX");
	}

	public static FileFilter getXlsMFileFilter()
	{
		return getFileFilter(XLSM_EXT, "TxtFileFilterXlsM");
	}

	public static FileFilter getOdsFileFilter()
	{
		return getFileFilter(ODS_EXT, "TxtFileFilterOds");
	}

	private static FileFilter getFileFilter(String ext, String key)
	{
		FileFilter ff = filters.get(ext);
		if (ff == null)
		{
			String desc = ResourceMgr.getString(key);
			ff = new ExtensionFileFilter(desc, ext, true);
			filters.put(ext, ff);
		}
		return ff;
	}
	// The description of this filter
	public String getDescription()
	{
		return this.desc;
	}
}
