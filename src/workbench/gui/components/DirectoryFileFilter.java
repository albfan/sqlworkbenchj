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
public class DirectoryFileFilter extends FileFilter
{
	private final String desc;
	public DirectoryFileFilter(String aDesc)
	{
		this.desc = aDesc;
	}
	
	public boolean accept(File f)
	{
		return f.isDirectory();
	}

	public String getDescription()
	{
		return this.desc;
	}
}
