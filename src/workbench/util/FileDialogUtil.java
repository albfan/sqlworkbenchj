/*
 * FileDialogUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.awt.Component;
import java.awt.Window;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import workbench.gui.components.EncodingPanel;

import workbench.gui.components.ExtensionFileFilter;
import workbench.interfaces.EncodingSelector;
import workbench.resource.Settings;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;
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
	public static final String CONFIG_DIR_KEY = "%ConfigDir%";
	private String encoding = null;

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

	public static void initFileChooserLabels()
	{
		// This is "borrowed" from com.sun.java.swing.plaf.windows.resources.windows
		Object[] labels = new Object[] { 
             "FileChooser.detailsViewActionLabelText", "Details" ,
             "FileChooser.detailsViewButtonAccessibleName", "Details" ,
             "FileChooser.detailsViewButtonToolTipText", "Details" ,
             "FileChooser.fileAttrHeaderText", "Attributes" ,
             "FileChooser.fileDateHeaderText", "Modified" ,
             "FileChooser.fileNameHeaderText", "Name" ,
             "FileChooser.fileNameLabelText", "File name:" ,
             "FileChooser.fileSizeHeaderText", "Size" ,
             "FileChooser.fileTypeHeaderText", "Type" ,
             "FileChooser.filesOfTypeLabelText", "Files of type:" ,
             "FileChooser.homeFolderAccessibleName", "Home" ,
             "FileChooser.homeFolderToolTipText", "Home" ,
             "FileChooser.listViewActionLabelText", "List" ,
             "FileChooser.listViewButtonAccessibleName", "List" ,
             "FileChooser.listViewButtonToolTipText", "List" ,
             "FileChooser.lookInLabelText", "Look in:" ,
             "FileChooser.newFolderAccessibleName", "New Folder" ,
             "FileChooser.newFolderActionLabelText", "New Folder" ,
             "FileChooser.newFolderToolTipText", "Create New Folder" ,
             "FileChooser.refreshActionLabelText", "Refresh" ,
             "FileChooser.saveInLabelText", "Save in:" ,
             "FileChooser.upFolderAccessibleName", "Up" ,
             "FileChooser.upFolderToolTipText", "Up One Level" ,
             "FileChooser.viewMenuLabelText", "View" 
        };
		
//		UIManager.put("FileChooser.newFolderErrorText",ResourceMgr.getPlainString(""));
//		UIManager.put("FileChooser.newFolderErrorSeparator",ResourceMgr.getPlainString(""));
//
		UIManager.put("FileChooser.fileDescriptionText","FileDescription");
//		UIManager.put("FileChooser.directoryDescriptionText",ResourceMgr.getPlainString(""));

		UIManager.getDefaults().putDefaults(labels);
		
		UIManager.put("FileChooser.saveButtonText",ResourceMgr.getPlainString("LblSave"));
		UIManager.put("FileChooser.openButtonText",ResourceMgr.getPlainString("LblOpen"));
		UIManager.put("FileChooser.saveDialogTitleText",ResourceMgr.getPlainString("LblSave"));
		UIManager.put("FileChooser.openDialogTitleText",ResourceMgr.getPlainString("LblOpen"));
		UIManager.put("FileChooser.cancelButtonText",ResourceMgr.getPlainString("LblCancel"));
//		UIManager.put("FileChooser.updateButtonText",ResourceMgr.getPlainString("LblUpdate"));
		UIManager.put("FileChooser.helpButtonText",ResourceMgr.getPlainString("LblHelp"));
		UIManager.put("FileChooser.directoryOpenButtonText",ResourceMgr.getPlainString("LblOpen"));

//		UIManager.put("FileChooser.saveButtonMnemonic", "");
//		UIManager.put("FileChooser.openButtonMnemonic", "");
//		UIManager.put("FileChooser.cancelButtonMnemonic", "");
//		UIManager.put("FileChooser.updateButtonMnemonic", "");
//		UIManager.put("FileChooser.helpButtonMnemonic", "");
//		UIManager.put("FileChooser.directoryOpenButtonMnemonic", "");

//		UIManager.put("FileChooser.saveButtonToolTipText",ResourceMgr.getPlainString(""));
//		UIManager.put("FileChooser.openButtonToolTipText",ResourceMgr.getPlainString(""));
//		UIManager.put("FileChooser.cancelButtonToolTipText",ResourceMgr.getPlainString(""));
//		UIManager.put("FileChooser.updateButtonToolTipText",ResourceMgr.getPlainString(""));
//		UIManager.put("FileChooser.helpButtonToolTipText",ResourceMgr.getPlainString(""));
//		UIManager.put("FileChooser.directoryOpenButtonToolTipText",ResourceMgr.getPlainString(""));
	}
	
	public String getXmlReportFilename(Component caller, JComponent accessory)
	{
		String lastDir = Settings.getInstance().getProperty("workbench.xmlreport.lastdir", null);
		JFileChooser fc = new JFileChooser(lastDir);
		fc.addChoosableFileFilter(ExtensionFileFilter.getXmlFileFilter());
		fc.setFileFilter(ExtensionFileFilter.getXmlFileFilter());

		if (this.encoding == null)
		{
			this.encoding = Settings.getInstance().getDefaultDataEncoding();
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
			p.setBorder(new EmptyBorder(0,5,0,0));
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
			Settings.getInstance().setProperty("workbench.xmlreport.lastdir", lastDir);
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

	public static String getBlobFile(Component caller)
	{
		return getBlobFile(caller, true);
	}
	public static String getBlobFile(Component caller, boolean showSaveDialog)
	{
		Window parent = SwingUtilities.getWindowAncestor(caller);
		String lastDir = Settings.getInstance().getLastBlobDir();
		JFileChooser fc = new JFileChooser(lastDir);
		int answer = JFileChooser.CANCEL_OPTION;
		if (showSaveDialog)
		{
			answer = fc.showSaveDialog(parent);
		}
		else
		{
			answer = fc.showOpenDialog(parent);
		}
		String filename = null;
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			File fl = fc.getSelectedFile();
			filename = fl.getAbsolutePath();
			Settings.getInstance().setLastBlobDir(fl.getParentFile().getAbsolutePath());
		}
		return filename;
	}
	
	public String getWorkspaceFilename(Window parent, boolean toSave)
	{
		return this.getWorkspaceFilename(parent, toSave, false);
	}

	public String getWorkspaceFilename(Window parent, boolean toSave, boolean replaceConfigDir)
	{
		String lastDir = Settings.getInstance().getLastWorkspaceDir();
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
			Settings.getInstance().setLastWorkspaceDir(lastDir);
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
		File config = new File(Settings.getInstance().getConfigDir());
		if (dir.equals(config))
		{
			return CONFIG_DIR_KEY + StringUtil.FILE_SEPARATOR + fname;
		}
		else
		{
			return aPathname;
		}
	}

	public static String replaceConfigDir(String aPathname)
	{
		if (aPathname == null) return null;
		String dir = Settings.getInstance().getConfigDir();
		return StringUtil.replace(aPathname, CONFIG_DIR_KEY, dir);
	}

	public static void selectPkMapFileIfNecessary(Component parent)
	{
		String file = Settings.getInstance().getPKMappingFilename();
		if (file != null)
		{
			File f = new File(file);
			if (f.exists()) return;
		}
		boolean doSelectFile = WbSwingUtilities.getYesNo(parent, ResourceMgr.getString("MsgSelectPkMapFile"));
		if (!doSelectFile) return;
		file = selectPkMapFile(parent);
		if (file != null)
		{
			Settings.getInstance().setPKMappingFilename(file);
		}
	}
	
	public static String selectPkMapFile(Component parent)
	{
		String fileName = Settings.getInstance().getPKMappingFilename();
		File f = null;
		if (fileName == null)
		{
			f = new File(Settings.getInstance().getConfigDir());
		}
		else
		{
			f = new File(fileName).getParentFile();
		}
				
		JFileChooser dialog = new JFileChooser(f);
		dialog.setApproveButtonText(ResourceMgr.getString("LblOK"));
		if (fileName != null) 
		{
			dialog.setSelectedFile(new File(fileName));
		}
		String selectedFile = null;
		int choice = dialog.showSaveDialog(parent);
		if (choice == JFileChooser.APPROVE_OPTION)
		{
			File target = dialog.getSelectedFile();
			selectedFile = target.getAbsolutePath();
		}
		return selectedFile;
	}
}
