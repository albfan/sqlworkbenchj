/*
 * FileDialogUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.util;

import java.awt.Component;
import java.awt.Window;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.EncodingDropDown;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.WbFileChooser;

/**
 *
 * @author Thomas Kellerer
 */
public class FileDialogUtil
{
  private static final int FILE_TYPE_UNKNOWN = -1;
  private static final int FILE_TYPE_TXT = 0;
  private static final int FILE_TYPE_SQL = 1;
  private static final int FILE_TYPE_XML = 2;
  private static final int FILE_TYPE_HTML = 3;
  private static final int FILE_TYPE_SQL_UPDATE = 4;

  private int lastFileType = FILE_TYPE_UNKNOWN;
  public static final String CONFIG_DIR_KEY = "%ConfigDir%";
  private String encoding = null;

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
    String lastDir = Settings.getInstance().getProperty("workbench.xmlreport.lastdir", null);
    JFileChooser fc = new WbFileChooser(lastDir);
    fc.addChoosableFileFilter(ExtensionFileFilter.getXmlFileFilter());
    fc.setFileFilter(ExtensionFileFilter.getXmlFileFilter());

    if (this.encoding == null)
    {
      this.encoding = Settings.getInstance().getDefaultDataEncoding();
    }

    EncodingDropDown encodingPanel = new EncodingDropDown(this.encoding);
    encodingPanel.setBorder(new EmptyBorder(0,5,0,0));
    fc.setAccessory(encodingPanel);

    String filename = null;

    Window parent = SwingUtilities.getWindowAncestor(caller);

    int answer = fc.showSaveDialog(parent);
    if (answer == JFileChooser.APPROVE_OPTION)
    {
      this.encoding = encodingPanel.getEncoding();

      File fl = fc.getSelectedFile();
      if (StringUtil.isEmptyString(encoding))
      {
        encoding = FileUtil.detectFileEncoding(fl);
      }
      FileFilter ff = fc.getFileFilter();
      if (ff instanceof ExtensionFileFilter)
      {
        ExtensionFileFilter eff = (ExtensionFileFilter)ff;
        filename = fl.getAbsolutePath();

        String ext = ExtensionFileFilter.getExtension(fl);
        if (StringUtil.isEmptyString(ext))
        {
          if (!filename.endsWith(".")) filename += ".";
          filename += eff.getDefaultExtension();
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
    try
    {
      Window parent = SwingUtilities.getWindowAncestor(caller);
      String lastDir = Settings.getInstance().getLastBlobDir();
      JFileChooser fc = new WbFileChooser(lastDir);
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
    catch (Throwable e)
    {
      LogMgr.logError("FileDialogUtil.getBlobFile()", "Error selecting file", e);
      WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
      return null;
    }
  }

  public String getWorkspaceFilename(Window parent, boolean toSave, boolean replaceConfigDir)
  {
    try
    {
      String lastDir = Settings.getInstance().getLastWorkspaceDir();
      JFileChooser fc = new WbFileChooser(lastDir);

      FileFilter wksp = ExtensionFileFilter.getWorkspaceFileFilter();
      fc.removeChoosableFileFilter(fc.getFileFilter()); // remove the default "All files filter"
      fc.addChoosableFileFilter(wksp);

      String filename = null;

      int answer = JFileChooser.CANCEL_OPTION;
      if (toSave)
      {
        fc.setDialogTitle(ResourceMgr.getString("TxtSaveWksp"));
        answer = fc.showSaveDialog(parent);
      }
      else
      {
        fc.setDialogTitle(ResourceMgr.getString("TxtLoadWksp"));
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
          if (StringUtil.isEmptyString(ext))
          {
            if (!filename.endsWith(".")) filename += ".";
            filename += ExtensionFileFilter.WORKSPACE_EXT;
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
        filename = this.removeConfigDir(filename);
      }
      return filename;
    }
    catch (Throwable e)
    {
      LogMgr.logError("FileDialogUtil.getWorkspaceFilename()", "Error selecting file", e);
      WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
      return null;
    }
  }

  public static String removeConfigDir(String aPathname)
  {
    WbFile f = new WbFile(aPathname);
    return removeConfigDir(f);
  }

  public static String removeConfigDir(WbFile toRemove)
  {
    String fname = toRemove.getName();
    File dir = toRemove.getParentFile();
    File config = Settings.getInstance().getConfigDir();
    if (dir != null && dir.equals(config))
    {
      return fname;
    }
    else
    {
      return toRemove.getFullPath();
    }
  }

  public static String getPathWithPlaceholder(WbFile file)
  {
    File configDir = Settings.getInstance().getConfigDir();

    File fileDir = file.getParentFile();
    while (!fileDir.equals(configDir))
    {
      fileDir = fileDir.getParentFile();
      if (fileDir == null) break;
    }
    if (fileDir == null) return file.getFullPath();

    String fpath = file.getAbsolutePath().replace(fileDir.getAbsolutePath(), CONFIG_DIR_KEY);
    return fpath;
  }

  public static String replaceConfigDir(String aPathname)
  {
    if (aPathname == null) return null;
    WbFile dir = new WbFile(Settings.getInstance().getConfigDir());
    return StringUtil.replace(aPathname, CONFIG_DIR_KEY, dir.getFullPath());
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
      f = Settings.getInstance().getConfigDir();
    }
    else
    {
      f = new File(fileName).getParentFile();
    }

    try
    {
      JFileChooser dialog = new WbFileChooser(f);
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
    catch (Throwable e)
    {
      LogMgr.logError("FileDialogUtil.selectPkMapFile()", "Error selecting file", e);
      WbSwingUtilities.showErrorMessage(ExceptionUtil.getDisplay(e));
      return null;
    }
  }

  public static WbFile selectPngFile(JComponent parent, String lastDirProp)
  {
    String last = Settings.getInstance().getProperty(lastDirProp, null);
    File lastDir = null;

    if (StringUtil.isNonBlank(last))
    {
      lastDir = new File(last);
    }
    else
    {
      lastDir = Settings.getInstance().getConfigDir();
    }

    JFileChooser fc = new WbFileChooser(lastDir);
    ExtensionFileFilter ff = new ExtensionFileFilter(ResourceMgr.getString("TxtFileFilterIcons"), CollectionUtil.arrayList("png"), true);
    fc.setMultiSelectionEnabled(false);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());
    fc.addChoosableFileFilter(ff);
    fc.setFileFilter(ff);

    int answer = fc.showOpenDialog(SwingUtilities.getWindowAncestor(parent));

    if (answer == JFileChooser.APPROVE_OPTION)
    {
      WbFile f = new WbFile(fc.getSelectedFile());
      if (!ImageUtil.isPng(f))
      {
        String msg = ResourceMgr.getFormattedString("ErrInvalidIcon", f.getName());
        WbSwingUtilities.showErrorMessage(parent, msg);
      }
      WbFile dir = new WbFile(fc.getCurrentDirectory());
      Settings.getInstance().setProperty(lastDirProp, dir.getFullPath());
      return f;
    }
    return null;
  }
}
