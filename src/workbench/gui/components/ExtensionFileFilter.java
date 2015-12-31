/*
 * ExtensionFileFilter.java
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
package workbench.gui.components;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.filechooser.FileFilter;

import workbench.resource.ResourceMgr;

import workbench.db.exporter.ExportType;

import workbench.util.CollectionUtil;
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
	private static Map<String, ExtensionFileFilter> filters = new HashMap<>();
	private static FileFilter jarFileFilter;

	private List<String> extensions;
	private String desc;
	public static final String SQL_EXT = "sql";
	public static final String TXT_EXT = "txt";
	public static final String CSV_EXT = "csv";
	public static final String WORKSPACE_EXT = "wksp";
	public static final String XML_EXT = "xml";
	public static final String HTML_EXT = "html";
	public static final String XLS_EXT = "xls";
	public static final String XLSX_EXT = "xlsx";
	public static final String XLSM_EXT = "xml";
	public static final String ODS_EXT = "ods";
	public static final String JSON_EXT = "json";

	private boolean ignoreCase = true;
	private ExportType exportType;

	public ExtensionFileFilter(String aDescription, List<String> anExtensionList, boolean ignoreFilenameCase)
	{
		super();
		this.desc = aDescription;
		this.extensions = anExtensionList;
		this.ignoreCase = ignoreFilenameCase;
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
		return TXT_EXT.equalsIgnoreCase(ext) || CSV_EXT.equalsIgnoreCase(ext);
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
			List<String> ext = CollectionUtil.arrayList("jar","zip");
			String desc = ResourceMgr.getString("TxtArchivesFilterName");
			jarFileFilter = new ExtensionFileFilter(desc, ext, true);
		}
		return jarFileFilter;
	}

	public static FileFilter getSqlFileFilter()
	{
		return getFileFilter("TxtFileFilterSql", ExportType.SQL_INSERT, SQL_EXT);
	}

	public static FileFilter getSqlInsertDeleteFilter()
	{
		return getFileFilter("TxtFileFilterSqlInsDel", ExportType.SQL_DELETE_INSERT, SQL_EXT);
	}

	public static FileFilter getSqlUpdateFileFilter()
	{
		return getFileFilter("TxtFileFilterSqlInsDel", ExportType.SQL_UPDATE, SQL_EXT);
	}

	public static FileFilter getTextFileFilter()
	{
		return getFileFilter("TxtFileFilterText", ExportType.TEXT, TXT_EXT, CSV_EXT);
	}

	public static FileFilter getXmlFileFilter()
	{
		return getFileFilter("TxtFileFilterXml", ExportType.XML, XML_EXT);
	}

	public static FileFilter getWorkspaceFileFilter()
	{
		return getFileFilter("TxtFileFilterWksp", null, WORKSPACE_EXT);
	}

	public static FileFilter getHtmlFileFilter()
	{
		return getFileFilter("TxtFileFilterHtml", ExportType.HTML, HTML_EXT);
	}

	public static FileFilter getXlsFileFilter()
	{
		return getFileFilter("TxtFileFilterXls", ExportType.XLS, XLS_EXT);
	}

	public static FileFilter getXlsXFileFilter()
	{
		return getFileFilter("TxtFileFilterXlsX", ExportType.XLSX, XLSX_EXT);
	}

	public static FileFilter getXlsMFileFilter()
	{
		 return getFileFilter("TxtFileFilterXlsM", ExportType.XLSM, XLSM_EXT);
	}

	public static FileFilter getOdsFileFilter()
	{
		return getFileFilter("TxtFileFilterOds", ExportType.ODS, ODS_EXT);
	}

	public static FileFilter getJsonFilterFilter()
	{
		return getFileFilter("TxtFileFilterJson", ExportType.JSON, JSON_EXT);
	}

	private static ExtensionFileFilter getFileFilter(String key, ExportType type, String... ext)
	{
		ExtensionFileFilter ff = filters.get(key);
		if (ff == null)
		{
			String desc = ResourceMgr.getString(key);
			ff = new ExtensionFileFilter(desc, Arrays.asList(ext), true);
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
