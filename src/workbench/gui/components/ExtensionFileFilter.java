/*
 * ExtensionFileFilter.java
 *
 * Created on August 13, 2002, 12:47 PM
 */

package workbench.gui.components;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.filechooser.FileFilter;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public class ExtensionFileFilter
	extends FileFilter
{
	private static FileFilter textFileFilter;
	private static FileFilter sqlFileFilter;
	private static FileFilter jarFileFilter;

	private List extensions;
	private String desc;

	private boolean ignoreCase = true;

	public ExtensionFileFilter(String aDescription, List anExtensionList)
	{
		this(aDescription, anExtensionList, true);
	}

	public ExtensionFileFilter(String aDescription, List anExtensionList, boolean ignoreCase)
	{
		this.desc = aDescription;
		this.extensions = anExtensionList;
		this.ignoreCase = ignoreCase;
	}

	public static String getExtension(File f)
	{
		String ext = "";
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 &&  i < s.length() - 1)
		{
			ext = s.substring(i+1);
		}
		return ext;
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
				if (extension.equalsIgnoreCase(this.extensions.get(i).toString())) return true;
			}
		}

		return false;
	}

	public static FileFilter getJarFileFilter()
	{
		if (jarFileFilter == null)
		{
			ArrayList ext = new ArrayList();
			ext.add("jar");
			ext.add("zip");
			String desc = ResourceMgr.getString("TxtArchivesFilterName");
			jarFileFilter = new ExtensionFileFilter(desc, ext, true);
		}
		return jarFileFilter;
	}
	
	public static FileFilter getSqlFileFilter()
	{
		if (sqlFileFilter == null)
		{
			ArrayList ext = new ArrayList();
			ext.add("sql");
			String desc = ResourceMgr.getString("TxtFileFilterSql");
			sqlFileFilter = new ExtensionFileFilter(desc, ext, true);
		}
		return sqlFileFilter;
	}

	public static FileFilter getTextFileFilter()
	{
		if (textFileFilter == null)
		{
			ArrayList ext = new ArrayList();
			ext.add("txt");
			String desc = ResourceMgr.getString("TxtFileFilterText");
			textFileFilter = new ExtensionFileFilter(desc, ext, true);
		}
		return textFileFilter;
	}

	// The description of this filter
	public String getDescription()
	{
		return this.desc;
	}
}
