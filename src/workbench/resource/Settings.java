/*
 * Settings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.resource;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import workbench.WbManager;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.profiles.ProfileKey;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.FontChangedListener;
import workbench.log.LogMgr;
import workbench.sql.DelimiterDefinition;
import workbench.sql.BatchRunner;
import workbench.storage.PkMapping;
import workbench.util.FileDialogUtil;
import workbench.util.StringUtil;
import workbench.util.ToolDefinition;
import workbench.util.WbFile;
import workbench.util.WbLocale;
import workbench.util.WbProperties;

/**
 *	@author  support@sql-workbench.net
 */
public class Settings
	implements PropertyStorage
{
	// <editor-fold defaultstate="collapsed" desc="Property Keys">
	public static final String PROPERTY_ANIMATED_ICONS = "workbench.gui.animatedicon";
	public static final String PROPERTY_ENCRYPT_PWD = "workbench.profiles.encryptpassword";
	public static final String PROPERTY_DATE_FORMAT = "workbench.gui.display.dateformat";
	public static final String PROPERTY_DATETIME_FORMAT = "workbench.gui.display.datetimeformat";
	public static final String PROPERTY_TIME_FORMAT = "workbench.gui.display.timeformat";
	public static final String PROPERTY_PDF_READER_PATH = "workbench.gui.pdfreader.path";
	public static final String PROPERTY_SHOW_TOOLBAR = "workbench.gui.mainwindow.showtoolbar";
	public static final String PROPERTY_SHOW_LINE_NUMBERS = "workbench.editor.showlinenumber";
	public static final String PROPERTY_HIGHLIGHT_CURRENT_STATEMENT = "workbench.editor.highlightcurrent";
	public static final String PROPERTY_AUTO_JUMP_STATEMENT = "workbench.editor.autojumpnext";
	public static final String PROPERTY_DBEXP_REMEMBER_SORT = "workbench.dbexplorer.remembersort";

	public static final String PROPERTY_EDITOR_FONT = "editor";
	public static final String PROPERTY_STANDARD_FONT = "std";
	public static final String PROPERTY_MSGLOG_FONT = "msglog";
	public static final String PROPERTY_DATA_FONT = "data";
	public static final String PROPERTY_PRINTER_FONT = "printer";
	/**
	 * The property that identifies the name of the file containing the connection profiles.
	 */
	public static final String PROPERTY_PROFILE_STORAGE = "workbench.settings.profilestorage";
	public static final String PROPERTY_EDITOR_TAB_WIDTH = "workbench.editor.tabwidth";
	public static final String PROPERTY_EDITOR_CURRENT_LINE_COLOR = "workbench.editor.currentline.color";
	public static final String PROPERTY_EDITOR_ELECTRIC_SCROLL = "workbench.editor.electricscroll";
	// </editor-fold>
	
	public static final String PK_MAPPING_FILENAME_PROPERTY = "workbench.pkmapping.file";
	public static final String UNIX_LINE_TERMINATOR_PROP_VALUE = "lf";
	public static final String DOS_LINE_TERMINATOR_PROP_VALUE = "crlf";
	public static final String DEFAULT_LINE_TERMINATOR_PROP_VALUE = "default";

	private static final String LIB_DIR_KEY = "%LibDir%";
	
	private WbProperties props;
	private WbFile configfile;
	
	private List<FontChangedListener> fontChangeListeners = new LinkedList<FontChangedListener>();

	private ShortcutManager keyManager;

	private static class LazyInstanceHolder
	{
		private static Settings instance = new Settings();
	}
	
	public static final Settings getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	private Settings()
	{
		this.props = new WbProperties(this);
		String configFile = System.getProperty("workbench.settings.file", "workbench.settings");

		// first read the built-in defaults
		// this ensures that new defaults will be applied automatically.
		fillDefaults();

		String configDir = getProperty("workbench.configdir", null);

		WbFile cfd = null;
		try
		{
			if (configDir == null || configDir.trim().length() == 0)
			{
				// check the current directory for a configuration file
				// if it is not present, then use the directory of the jar file
				// this means that upon first startup, the settings file
				// will be created in the directory of the jar file
				File f = new File(System.getProperty("user.dir"), configFile);
				if (f.exists())
				{
					cfd = new WbFile(f.getParentFile());
				}
				else
				{
					cfd = new WbFile(WbManager.getInstance().getJarPath());
					f = new File(cfd,configFile);
					if (!f.exists())
					{
						// no config file in the jar directory --> create a config directory in user.home
						cfd = new WbFile(System.getProperty("user.home"), ".sqlworkbench");
					}
				}
			}
			else
			{
				if (configDir.indexOf("${user.home}") > -1)
				{
					configDir = StringUtil.replace(configDir, "${user.home}", System.getProperty("user.home"));
				}
				cfd = new WbFile(configDir);
			}
		}
		catch (Exception e)
		{
			cfd = new WbFile(System.getProperty("user.home"), ".sqlworkbench");
		}
		
		if (!cfd.exists())
		{
			cfd.mkdirs();
		}
		
		this.configfile = new WbFile(cfd, configFile);
		configDir = cfd.getFullPath();

		BufferedInputStream in = null;
	  try
		{
			in = new BufferedInputStream(new FileInputStream(this.configfile));
			this.props.load(in);
		}
		catch (IOException e)
		{
			fillDefaults();
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		
		boolean logSysErr = getBoolProperty("workbench.log.console", false);
		LogMgr.logToSystemError(logSysErr);

		String format = getProperty("workbench.log.format", "{type} {timestamp} {message} {error}");
		LogMgr.setMessageFormat(format);

		String level = getProperty("workbench.log.level", "INFO");
		LogMgr.setLevel(level);

    try
    {
			String logfilename = getProperty("workbench.log.filename", "workbench.log");
			
			// Replace old System.out or System.err settings
			if (logfilename.equalsIgnoreCase("System.out") || logfilename.equalsIgnoreCase("System.err"))
			{
				logfilename = "workbench.log";
				this.props.setProperty("workbench.log.filename", "workbench.log");
			}
			
			File logfile = new File(logfilename);
			if (!logfile.isAbsolute())
			{
				logfile = new File(getConfigDir(), logfilename);
			}

			if (!logfile.canWrite())
			{
				logfile = new File(getConfigDir(), "workbench.log");
				this.props.setProperty("workbench.log.filename", "workbench.log");
			}
			
			int maxSize = this.getMaxLogfileSize();
			LogMgr.setOutputFile(logfile, maxSize);
    }
    catch (Throwable e)
    {
      System.err.println("Error initializing Log system!");
      e.printStackTrace(System.err);
    }

		LogMgr.logInfo("Settings.<init>", "Using configdir: " + configDir);

		this.renameOldProps();
		this.migrateProps();
		this.removeObsolete();
	}

	// <editor-fold defaultstate="collapsed" desc="Manual">
	public String getPdfPath()
	{
		String pdfManual = getProperty("workbench.manual.pdf.file", "SQLWorkbench-Manual.pdf");
		
		File f = new File(pdfManual);
		if (f.isDirectory())
		{
			f = new File(f, "SQLWorkbench-Manual.pdf");
		}
		
		if (f.exists() && f.canRead())
		{
			return f.getAbsolutePath();
		}
		
		String jarDir = WbManager.getInstance().getJarPath();
		WbFile pdf = new WbFile(jarDir, pdfManual);
		
		if (!pdf.exists())
		{
			pdf = new WbFile(getConfigDir(), pdfManual);
		}
		
		if (pdf.exists() && pdf.canRead())
		{
			return pdf.getFullPath();
		}
		else
		{
			return null;
		}
	}	
	/**
	 * Returns the directory where the HTML manual is located.
	 * 
	 * @return the directory where the HTML manual is located or null if it cannot be found
	 */
	public File getHtmlManualDir()
	{
		// Allow overriding the default location of the HTML manual
		String dir = getProperty("workbench.manual.html.dir", null);
		File htmldir = null;
		
		if (dir == null)
		{
			// First look in the directory of the jar file.
			File jardir = WbManager.getInstance().getJarFile().getParentFile();
			htmldir = new File(jardir, "manual");
		}
		else
		{
			htmldir = new File(dir);
		}
		
		if (!htmldir.exists())
		{
			htmldir = new File(getConfigDir(), "manual");
		}
		
		if (htmldir.exists())
		{
			return htmldir;
		}
		
		return null;
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="Language settings">
	public void setLanguage(Locale locale)
	{
		setProperty("workbench.gui.language", locale.getLanguage());
	}

	public List<WbLocale> getLanguages()
	{
		String prop = getProperty("workbench.gui.languages.available", "en,de");
		List<String> codes = StringUtil.stringToList(prop, ",", true, true, false);
		List<WbLocale> result = new ArrayList<WbLocale>(codes.size());
		for (String c : codes)
		{
			try
			{
				result.add(new WbLocale(new Locale(c)));
			}
			catch (Exception e)
			{
				LogMgr.logError("Settings.getLanguages()", "Invalid locale specified: " + c, e);
			}
		}
		return result;
	}
	
	public Locale getLanguage()
	{
		String lanCode = getProperty("workbench.gui.language", "en");
		Locale l = null;
		try
		{
			l = new Locale(lanCode);
		}
		catch (Exception e)
		{
			LogMgr.logError("Settings.getLanguage()", "Error creating Locale for language=" + lanCode, e);
			l = new Locale("en");
		}
		return l;
	}
	// </editor-fold>
	
	public ShortcutManager getShortcutManager()
	{
		if (this.keyManager == null)
		{
			this.keyManager = new ShortcutManager(this.getShortcutFilename());
		}
		return this.keyManager;
	}

	// <editor-fold defaultstate="collapsed" desc="Settings Configuration">
	public String getDriverConfigFilename()
	{
		return new WbFile(getConfigDir(), "WbDrivers.xml").getFullPath();
	}
	
	public File getConfigDir() 
	{ 
		return this.configfile.getParentFile(); 
	}
	
	public String replaceLibDirKey(String aPathname)
	{
		if (aPathname == null) return null;
		String libDir = Settings.getInstance().getLibDir();
		if (libDir == null) return aPathname;
		return StringUtil.replace(aPathname, LIB_DIR_KEY, libDir);
	}	
	
	public String getLibDir()
	{
		return System.getProperty("workbench.libdir", getProperty("workbench.libdir", null));
	}
	private String getShortcutFilename()
	{
		return new File(getConfigDir(), "WbShortcuts.xml").getAbsolutePath();
	}

	public void setProfileStorage(String file)
	{
		if (StringUtil.isEmptyString(file))
		{
			this.props.remove(PROPERTY_PROFILE_STORAGE);
		}
		else
		{
			this.props.setProperty(PROPERTY_PROFILE_STORAGE, file);
		}
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="Blob Stuff">
	
	public void setLastUsedBlobTool(String name)
	{
		setProperty("workbench.tools.last.blob", name);
	}

	public String getLastUsedBlobTool()
	{
		return getProperty("workbench.tools.last.blob", null);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="External Tools">

	public ToolDefinition[] getExternalTools()
	{
		return getExternalTools(true);
	}

	public ToolDefinition[] getAllExternalTools()
	{
		return getExternalTools(false);
	}

	private ToolDefinition[] getExternalTools(boolean check)
	{
		int numTools = getIntProperty("workbench.tools.count", 0);
		LinkedList<ToolDefinition> l = new LinkedList<ToolDefinition>();
		int count = 0;
		for (int i = 0; i < numTools; i++)
		{
			String path = getProperty("workbench.tools." + i + ".executable", "");
			String name = getProperty("workbench.tools." + i + ".name", path);

			ToolDefinition tool = new ToolDefinition(path, name);
			
			if (check && tool.executableExists())
			{
				l.add(tool);
				count++;
			}
			else
			{
				l.add(tool);
				count ++;
			}
		}
		ToolDefinition[] result = new ToolDefinition[count];
		Iterator<ToolDefinition> itr = l.iterator();
		int i=0;
		while (itr.hasNext())
		{
			result[i] = itr.next();
			i++;
		}
		return result;
	}

	public void setExternalTool(Collection tools)
	{
		int count = 0;
		Iterator itr = tools.iterator();
		while (itr.hasNext())
		{
			ToolDefinition tool = (ToolDefinition)itr.next();
			setProperty("workbench.tools." + count + ".executable", tool.getApplicationPath());
			setProperty("workbench.tools." + count + ".name", tool.getName());
			count ++;
		}
		setProperty("workbench.tools.count", count);
	}

	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="Update Check">
	public Date getLastUpdateCheck()
	{
		String dt = getProperty("workbench.gui.updatecheck.lastcheck", null);
		if (dt == null) return null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date last = null;
		try
		{
			last = sdf.parse(dt);
			return last;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public void setLastUpdateCheck()
	{
		Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		this.props.setProperty("workbench.gui.updatecheck.lastcheck", sdf.format(now), false);
	}
	
	public int getUpdateCheckInterval()
	{
		return getIntProperty("workbench.gui.updatecheck.interval", 7);
	}
	
	public void setUpdateCheckInterval(int days)
	{
		setProperty("workbench.gui.updatecheck.interval", days);
	}
	// </editor-fold>

	/**
	 * Return a list of popular encodings to be used for the code-completion
	 * of the -encoding parameter.
	 * @see workbench.sql.wbcommands.CommonArgs#addEncodingParameter(workbench.util.ArgumentParser)
	 */
	public String getPopularEncodings()
	{
		return getProperty("workbench.export.defaultencodings", "UTF-8,ISO-8859-1,ISO-8859-15,<name>");
	}
	
	/**
	 * Return true if the application should be terminated if the first connect
	 * dialog is cancelled.
	 */
	public boolean getExitOnFirstConnectCancel()
	{
		return getBoolProperty("workbench.gui.cancel.firstconnect.exit", false);
	}

	public void setExitOnFirstConnectCancel(boolean flag)
	{
		setProperty("workbench.gui.cancel.firstconnect.exit", flag);
	}
	
	public String getPDFReaderPath()
	{
		return getProperty(PROPERTY_PDF_READER_PATH, null);
	}

	public void setPDFReaderPath(String path)
	{
		setProperty(PROPERTY_PDF_READER_PATH, path);
	}

	// <editor-fold defaultstate="collapsed" desc="Formatting options">
	
	public int getFormatterMaxColumnsInSelect()
	{
		return getIntProperty("workbench.sql.formatter.select.columnsperline", 1);
	}

	public void setFormatterMaxColumnsInSelect(int value)
	{
		setProperty("workbench.sql.formatter.select.columnsperline", value);
	}

	public boolean getFormatterLowercaseFunctions()
	{
		return getBoolProperty("workbench.sql.formatter.functions.lowercase", false);
	}

	public void setFormatterLowercaseFunctions(boolean flag)
	{
		setProperty("workbench.sql.formatter.functions.lowercase", flag);
	}
	
	public int getFormatterMaxSubselectLength()
	{
		return getIntProperty("workbench.sql.formatter.subselect.maxlength", 60);
	}

	public void setFormatterMaxSubselectLength(int value)
	{
		setProperty("workbench.sql.formatter.subselect.maxlength", value);
	}
	
	public int getMaxCharInListElements()
	{
		return getIntProperty("workbench.editor.format.list.maxelements.quoted", 2);
	}

	public void setMaxCharInListElements(int value)
	{
		setProperty("workbench.editor.format.list.maxelements.quoted", (value <= 0 ? 2 : value));
	}

	public int getMaxNumInListElements()
	{
		return getIntProperty("workbench.editor.format.list.maxelements.nonquoted", 10);
	}

	public void setMaxNumInListElements(int value)
	{
		setProperty("workbench.editor.format.list.maxelements.nonquoted", (value <= 0 ? 10 : value));
	}

	public int getFormatUpdateColumnThreshold()
	{
		return getIntProperty("workbench.sql.generate.update.newlinethreshold", 5);
	}

	public void setFormatUpdateColumnThreshold(int value)
	{
		setProperty("workbench.sql.generate.update.newlinethreshold", value);
	}

	public int getFormatInsertColsPerLine()
	{
		return getIntProperty("workbench.sql.generate.insert.colsperline",1);
	}

	public void setFormatInsertColsPerLine(int value)
	{
		setProperty("workbench.sql.generate.insert.colsperline",1);
	}

	public boolean getFormatInsertIgnoreIdentity()
	{
		return getBoolProperty("workbench.sql.generate.insert.ignoreidentity",true);
	}

	public void setFormatInsertIgnoreIdentity(boolean flag)
	{
		setProperty("workbench.sql.generate.insert.ignoreidentity",flag);
	}

	public int getFormatInsertColumnThreshold()
	{
		return getIntProperty("workbench.sql.generate.insert.newlinethreshold", 5);
	}

	public void setFormatInsertColumnThreshold(int value)
	{
		setProperty("workbench.sql.generate.insert.newlinethreshold", value);
	}

	public boolean getDoFormatUpdates()
	{
		return getBoolProperty("workbench.sql.generate.update.doformat",true);
	}

	public void setDoFormatUpdates(boolean flag)
	{
		setProperty("workbench.sql.generate.update.doformat", flag);
	}

	public boolean getDoFormatInserts()
	{
		return getBoolProperty("workbench.sql.generate.insert.doformat",true);
	}

	public void setDoFormatInserts(boolean flag)
	{
		setProperty("workbench.sql.generate.insert.doformat", flag);
	}

	public void setIncludeEmptyComments(boolean flag)
	{
		setProperty("workbench.sql.generate.comment.includeempty", flag);
	}
	
	public boolean getIncludeEmptyComments()
	{
		return getBoolProperty("workbench.sql.generate.comment.includeempty", false);
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="DbExplorer">

	public boolean getRetrieveDbExplorer()
	{
		return getBoolProperty("workbench.dbexplorer.retrieveonopen", true);
	}

	public void setRetrieveDbExplorer(boolean aFlag)
	{
		this.setProperty("workbench.dbexplorer.retrieveonopen", aFlag);
	}
	
	public void setShowDbExplorerInMainWindow(boolean showWindow)
	{
		this.setProperty("workbench.dbexplorer.mainwindow", showWindow);
	}

	public boolean getShowDbExplorerInMainWindow()
	{
		return this.getBoolProperty("workbench.dbexplorer.mainwindow", true);
	}
	
	public boolean getAutoGeneratePKName()
	{
		return getBoolProperty("workbench.db.createpkname", true);
	}

	public void setAutoGeneratePKName(boolean flag)
	{
		setProperty("workbench.db.createpkname", flag);
	}
	
	/**
	 * Returns true if the DbExplorer should show an additional 
	 * panel with all triggers
	 */
	public boolean getShowTriggerPanel()
	{
		return getBoolProperty("workbench.dbexplorer.triggerpanel.show", true);
	}
	
	public void setShowTriggerPanel(boolean flag)
	{
		setProperty("workbench.dbexplorer.triggerpanel.show", flag);
	}
	
	public void setShowFocusInDbExplorer(boolean flag)
	{
		setProperty("workbench.gui.dbobjects.showfocus", flag);
	}
	
	public boolean showFocusInDbExplorer()
	{
		return getBoolProperty("workbench.gui.dbobjects.showfocus", false);
	}
	
	public void setRememberSortInDbExplorer(boolean flag)
	{
		setProperty(PROPERTY_DBEXP_REMEMBER_SORT, flag);
	}
	
	public boolean getRememberSortInDbExplorer()
	{
		return getBoolProperty(PROPERTY_DBEXP_REMEMBER_SORT, false);
	}
	
	public String getDefaultObjectType()
	{
		return getProperty("workbench.dbexplorer.defTableType", null);
	}

	public void setStoreExplorerObjectType(boolean flag)
	{
		setProperty("workbench.dbexplorer.rememberObjectType", flag);
	}

	public boolean getStoreExplorerObjectType()
	{
		return getBoolProperty("workbench.dbexplorer.rememberObjectType", false);
	}

	public boolean getSwitchCatalogInExplorer()
	{
		return getBoolProperty("workbench.dbexplorer.switchcatalog", true);
	}

	public String getProfileStorage()
	{
		String profiles = this.props.getProperty(PROPERTY_PROFILE_STORAGE);
		if (profiles == null)
		{
			return new File(getConfigDir(), "WbProfiles.xml").getAbsolutePath();
		}
		String realFilename = FileDialogUtil.replaceConfigDir(profiles);

		WbFile f = new WbFile(realFilename);
		if (!f.isAbsolute())
		{
			// no directory in filename -> use config directory
			f = new WbFile(getConfigDir(), realFilename);
		}
		return f.getFullPath();
	}

	public boolean getSelectDataPanelAfterRetrieve()
	{
		return getBoolProperty("workbench.gui.dbobjects.autoselectdatapanel", true);
	}
	
	public void setSelectDataPanelAfterRetrieve(boolean flag)
	{
		setProperty("workbench.gui.dbobjects.autoselectdatapanel", flag);
	}
	
	// </editor-fold>
	
	public void addFontChangedListener(FontChangedListener aListener)
	{
		this.fontChangeListeners.add(aListener);
	}

	public synchronized void removeFontChangedListener(FontChangedListener aListener)
	{
		this.fontChangeListeners.remove(aListener);
	}

	public synchronized void addPropertyChangeListener(PropertyChangeListener l, String property, String ... properties)
	{
		this.props.addPropertyChangeListener(l, property);
		if (properties.length > 0) this.props.addPropertyChangeListener(l, properties);
	}

	public void removePropertyChangeListener(PropertyChangeListener l)
	{
		this.props.removePropertyChangeListener(l);
	}

	// <editor-fold defaultstate="collapsed" desc="Font Settings">
	public void setFont(String aFontName, Font aFont)
	{
		String baseKey = "workbench.font." + aFontName;
		if (aFont == null)
		{
			this.props.remove(baseKey + ".name");
			this.props.remove(baseKey + ".size");
			this.props.remove(baseKey + ".style");
			return;
		}

		String name = aFont.getFamily();
		String size = Integer.toString(aFont.getSize());
		int style = aFont.getStyle();
		this.props.setProperty(baseKey + ".name", name);
		this.props.setProperty(baseKey + ".size", size);
		String value = null;
		if ((style & Font.BOLD) == Font.BOLD)
			value = "BOLD";
		if ((style & Font.ITALIC) == Font.ITALIC)
		{
			if (value == null) value = "ITALIC";
			else value = value + ",ITALIC";
		}
		if (value == null) value = "PLAIN";
		this.props.setProperty(baseKey + ".style", value);
		this.fireFontChangedEvent(aFontName, aFont);
	}

	public void fireFontChangedEvent(String aKey, Font aFont)
	{
		for (FontChangedListener listener : fontChangeListeners)
		{
			if (listener != null)	listener.fontChanged(aKey, aFont);
		}
	}
	
	public void setPrintFont(Font aFont)
	{
		this.setFont(PROPERTY_PRINTER_FONT, aFont);
	}

	public void setEditorFont(Font f)
	{
		this.setFont(PROPERTY_EDITOR_FONT, f);
	}

	public Font getEditorFont()
	{
		return this.getFont(PROPERTY_EDITOR_FONT, true);
	}

	public void setMsgLogFont(Font f)
	{
		this.setFont(PROPERTY_MSGLOG_FONT, f);
	}

	public Font getMsgLogFont()
	{
		return this.getFont(PROPERTY_MSGLOG_FONT, true);
	}

	public void setDataFont(Font f)
	{
		this.setFont(PROPERTY_DATA_FONT, f);
	}

	public Font getDataFont(boolean returnDefault)
	{
		Font f = this.getFont(PROPERTY_DATA_FONT, false);
		if (f == null && returnDefault)
		{
			UIDefaults def = UIManager.getLookAndFeelDefaults();
			f = def.getFont("Table.font");
		}
		return f;
	}

	public Font getPrinterFont()
	{
		Font f  = this.getFont(PROPERTY_PRINTER_FONT, false);
		if (f == null)
		{
			f = this.getDataFont(true);
		}
		return f;
	}

	 public Font getStandardFont()
	{
		return this.getFont(PROPERTY_STANDARD_FONT, false);
	}
	
	public void setStandardFont(Font f)
	{
		this.setFont(PROPERTY_STANDARD_FONT, f);
	}
	
	/**
	 *	Returns the font configured for this keyword
	 */
	public Font getFont(String aFontName, boolean returnDefault)
	{
		Font result = null;

		String baseKey = "workbench.font." + aFontName;
		String name = this.props.getProperty(baseKey + ".name", null);
		if (name == null && returnDefault) name = "Dialog";
		
		if (name == null) return null;

		String sizeS = this.props.getProperty(baseKey + ".size", "11");
		String type = this.props.getProperty(baseKey + ".style", "Plain");
		int style = Font.PLAIN;
		int size = 12;
		StringTokenizer tok = new StringTokenizer(type);
		while (tok.hasMoreTokens())
		{
			String t = tok.nextToken();
			if ("bold".equalsIgnoreCase(t)) style = style | Font.BOLD;
			if ("italic".equalsIgnoreCase(type)) style = style | Font.ITALIC;
		}

		try
		{
			size = Integer.parseInt(sizeS);
		}
		catch (NumberFormatException e)
		{
			size = 11;
		}
		result = new Font(name, style, size);
		return result;
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Date Formats">
	public void setCopyDefault(String value)
	{
		if (value == null) return;
		if (value.equalsIgnoreCase("all") || value.equalsIgnoreCase("selected"))
		{
			setProperty("workbench.gui.table.copydefault", value.toLowerCase());
		}
	}

	public String getLiteralTypes()
	{
		return getProperty("workbench.sql.literals.types", "jdbc,ansi,dbms,default");
	}
	
	public List<String> getLiteralTypeList()
	{
		String types = getLiteralTypes();
		List<String> result = StringUtil.stringToList(types, ",", true, true, false);	
		if (!result.contains("dbms"))
		{
			result.add("dbms");
		}
		return result;
	}
	
	public void setDefaultCopyDateLiteralType(String type)
	{
		setProperty("workbench.export.copy.sql.dateliterals", type);
	}
	
	public String getDefaultCopyDateLiteralType()
	{
		return getProperty("workbench.export.copy.sql.dateliterals", "dbms");
	}

	public void setDefaultExportDateLiteralType(String type)
	{
		setProperty("workbench.export.sql.default.dateliterals", type);
	}
	
	public String getDefaultExportDateLiteralType()
	{
		return getProperty("workbench.export.sql.default.dateliterals", "dbms");
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Data display">

	public boolean getAllowRowHeightResizing()
	{
		return getBoolProperty("workbench.gui.display.rowheightresize", false);
	}

	public void setAllowRowHeightResizing(boolean flag)
	{
		setProperty("workbench.gui.display.rowheightresize", flag);
	}
	
	public boolean getUseAlternateRowColor()
	{
		return getBoolProperty("workbench.gui.table.alternate.use", false);
	}

	public void setUseAlternateRowColor(boolean flag)
	{
		setProperty("workbench.gui.table.alternate.use", flag);
	}

	public void setNullColor(Color c)
	{
		setColor("workbench.gui.table.null.color", c);
	}
	
	public Color getNullColor()
	{
		return getColor("workbench.gui.table.null.color", null);
	}

	public Color getExpressionHighlightColor()
	{
		return getColor("workbench.gui.table.searchhighlite.color", Color.YELLOW);
	}

	public void setExpressionHighlightColor(Color c)
	{
		setColor("workbench.gui.table.searchhighlite.color", c);
	}
	
	public Color getAlternateRowColor()
	{
		return getColor("workbench.gui.table.alternate.color", new Color(252,252,252));
	}

	public void setAlternateRowColor(Color c)
	{
		setColor("workbench.gui.table.alternate.color", c);
	}

	public void setRequiredFieldColor(Color c)
	{
		setColor("workbench.gui.edit.requiredfield.color", c);
	}

	public Color getRequiredFieldColor()
	{
		return getColor("workbench.gui.edit.requiredfield.color", new Color(255,100,100));
	}

	public void setHighlightRequiredFields(boolean flag)
	{
		setProperty("workbench.gui.edit.requiredfield.dohighlight", flag);
	}

	public boolean getHighlightRequiredFields()
	{
		return getBoolProperty("workbench.gui.edit.requiredfield.dohighlight", true);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Color Handling">
	public Color getColor(String aColorKey)
	{
		return getColor(aColorKey, null);
	}

	public Color getColor(String aColorKey, Color defaultColor)
	{
		String value = this.getProperty(aColorKey, null);
		if (value == null) return defaultColor;
		String[] colors = value.split(",");
		if (colors.length != 3) return defaultColor;
		try
		{
			int r = StringUtil.getIntValue(colors[0]);
			int g = StringUtil.getIntValue(colors[1]);
			int b = StringUtil.getIntValue(colors[2]);
			return new Color(r,g,b);
		}
		catch (Exception e)
		{
			return defaultColor;
		}
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="Printing">
	public PageFormat getPageFormat()
	{
		double leftmargin = this.getPrintMarginLeft();
		double rightmargin = this.getPrintMarginRight();

		double topmargin = this.getPrintMarginTop();
		double bottommargin = this.getPrintMarginBottom();

		PageFormat page = new PageFormat();
		page.setOrientation(this.getPrintOrientation());

		Paper paper = null;
		double width = this.getPrintPaperWidth();
		double height = this.getPrintPaperHeight();

		if (width > 0 && height > 0)
		{
			paper = new Paper();
			paper.setSize(width, height);
		}
		else
		{
			paper = page.getPaper();
			width = paper.getWidth();
			height = paper.getHeight();
		}
		paper.setImageableArea(leftmargin, topmargin, width - leftmargin - rightmargin, height - topmargin - bottommargin);
		page.setPaper(paper);
		return page;
	}

	public boolean getShowNativePageDialog()
	{
		return getBoolProperty("workbench.print.nativepagedialog", true);
	}

	private int getPrintOrientation()
	{
		return getIntProperty("workbench.print.orientation", PageFormat.PORTRAIT);
	}

	private double getPrintPaperWidth()
	{
		return StringUtil.getDoubleValue(this.props.getProperty("workbench.print.paper.width"), -1);
	}

	private double getPrintPaperHeight()
	{
		return StringUtil.getDoubleValue(this.props.getProperty("workbench.print.paper.height"), -1);
	}

	public void setPageFormat(PageFormat aFormat)
	{
		Paper p = aFormat.getPaper();
		double width = p.getWidth();
		double height = p.getHeight();

		double leftmargin = aFormat.getImageableX();
		double rightmargin = width - leftmargin - aFormat.getImageableWidth();

		double topmargin = aFormat.getImageableY();
		double bottommargin = height - topmargin - aFormat.getImageableHeight();

		this.setPrintMarginLeft(leftmargin);
		this.setPrintMarginRight(rightmargin);
		this.setPrintMarginTop(topmargin);
		this.setPrintMarginBottom(bottommargin);

		this.props.setProperty("workbench.print.paper.width", Double.toString(width));
		this.props.setProperty("workbench.print.paper.height", Double.toString(height));
		this.props.setProperty("workbench.print.orientation", Integer.toString(aFormat.getOrientation()));
	}

	public void setPrintMarginLeft(double aValue)
	{
		this.setPrintMargin("left", aValue);
	}
	public void setPrintMarginRight(double aValue)
	{
		this.setPrintMargin("right", aValue);
	}
	public void setPrintMarginTop(double aValue)
	{
		this.setPrintMargin("top", aValue);
	}
	public void setPrintMarginBottom(double aValue)
	{
		this.setPrintMargin("bottom", aValue);
	}

	public double getPrintMarginLeft()
	{
		return this.getPrintMargin("left");
	}
	public double getPrintMarginRight()
	{
		return this.getPrintMargin("right");
	}
	public double getPrintMarginTop()
	{
		return this.getPrintMargin("top");
	}
	public double getPrintMarginBottom()
	{
		return this.getPrintMargin("bottom");
	}

	private void setPrintMargin(String aKey, double aValue)
	{
		this.props.setProperty("workbench.print.margin." + aKey, Double.toString(aValue));
	}

	private double getPrintMargin(String aKey)
	{
		return StringUtil.getDoubleValue(this.props.getProperty("workbench.print.margin." + aKey, "72"),72);
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="GUI Stuff">
	public boolean getIncludeHeaderInOptimalWidth()
	{
		return getBoolProperty("workbench.gui.optimalwidth.includeheader", true);
	}

	public void setIncludeHeaderInOptimalWidth(boolean flag)
	{
		setProperty("workbench.gui.optimalwidth.includeheader", flag);
	}

	public boolean getAutomaticOptimalWidth()
	{
		return getBoolProperty("workbench.gui.optimalwidth.automatic", true);
	}

	public void setAutomaticOptimalWidth(boolean flag)
	{
		setProperty("workbench.gui.optimalwidth.automatic", flag);
	}
	
	public boolean getUseAnimatedIcon()
	{
		return getBoolProperty(PROPERTY_ANIMATED_ICONS, false);
	}

	public void setUseAnimatedIcon(boolean flag)
	{
		this.setProperty(PROPERTY_ANIMATED_ICONS, flag);
	}

	public boolean getUseDynamicLayout()
	{
		return getBoolProperty("workbench.gui.dynamiclayout", true);
	}

	public void setUseDynamicLayout(boolean flag)
	{
		this.setProperty("workbench.gui.dynamiclayout", flag);
	}

	public boolean getShowMnemonics()
	{
		return getBoolProperty("workbench.gui.showmnemonics", true);
	}

	public boolean getShowSplash()
	{
		return getBoolProperty("workbench.gui.showsplash", false);
	}

	public int getProfileDividerLocation()
	{
		return getIntProperty("workbench.gui.profiles.divider", -1);
	}

	public void setProfileDividerLocation(int aValue)
	{
		this.props.setProperty("workbench.gui.profiles.divider", Integer.toString(aValue));
	}

	public void setMinColumnWidth(int width)
	{
		setProperty("workbench.gui.optimalwidth.minsize", width);
	}

	public int getMinColumnWidth()
	{
		return this.getIntProperty("workbench.gui.optimalwidth.minsize", 50);
	}

	public int getMaxColumnWidth()
	{
		return getIntProperty("workbench.gui.optimalwidth.maxsize", 850);
	}

	public void setMaxColumnWidth(int width)
	{
		this.setProperty("workbench.gui.optimalwidth.maxsize", width);
	}
	
	public void setLookAndFeelClass(String aClassname)
	{
		this.props.setProperty("workbench.gui.lookandfeelclass", aClassname);
	}

	public String getLookAndFeelClass()
	{
		return getProperty("workbench.gui.lookandfeelclass", "");
	}
	
	public int getMaxMacrosInMenu()
	{
		return getIntProperty("workbench.gui.macro.maxmenuitems", 9);
	}
	
	public static final int SHOW_NO_FILENAME = 0;
	public static final int SHOW_FILENAME = 1;
	public static final int SHOW_FULL_PATH = 2;

	public void setShowFilenameInWindowTitle(int type)
	{
		switch (type)
		{
			case SHOW_NO_FILENAME:
				this.setProperty("workbench.gui.display.showfilename", "none");
				break;
			case SHOW_FILENAME:
				this.setProperty("workbench.gui.display.showfilename", "name");
				break;
			case SHOW_FULL_PATH:
				this.setProperty("workbench.gui.display.showfilename", "path");
				break;
		}
	}

	public int getShowFilenameInWindowTitle()
	{
		String type = this.getProperty("workbench.gui.display.showfilename", "none");
		if ("name".equalsIgnoreCase(type)) return SHOW_FILENAME;
		if ("path".equalsIgnoreCase(type)) return SHOW_FULL_PATH;
		return SHOW_NO_FILENAME;
	}

	public String getTitleGroupSeparator()
	{
		String sep = getProperty("workbench.gui.display.titlegroupsep", "/");
		if ("XXX".equals(sep)) return "";
		return sep;
	}
	
	public void setTitleGroupSeparator(String sep)
	{
		if (StringUtil.isEmptyString(sep) || sep.trim().length() == 0) sep = "XXX";
		setProperty("workbench.gui.display.titlegroupsep", sep);
	}
	
	public String getTitleGroupBracket()
	{
		return getProperty("workbench.gui.display.titlegroupbracket", null);
	}
	
	public void setTitleGroupBracket(String bracket)
	{
		setProperty("workbench.gui.display.titlegroupbracket", bracket);
	}
	
	public void setShowWorkspaceInWindowTitle(boolean flag)
	{
		setProperty("workbench.gui.display.showpworkspace", flag);
	}
	
	public boolean getShowWorkspaceInWindowTitle()
	{
		return getBoolProperty("workbench.gui.display.showpworkspace", true);
	}
	
	public void setShowProfileGroupInWindowTitle(boolean flag)
	{
		setProperty("workbench.gui.display.showprofilegroup", flag);
	}
	
	public boolean getShowProfileGroupInWindowTitle()
	{
		return getBoolProperty("workbench.gui.display.showprofilegroup", false);
	}

	public void setShowProductNameAtEnd(boolean flag)
	{
		setProperty("workbench.gui.display.name_at_end", flag);
	}
	
	public boolean getShowProductNameAtEnd()
	{
		return getBoolProperty("workbench.gui.display.name_at_end", false);
	}

	public boolean getShowToolbar()
	{
		return getBoolProperty(PROPERTY_SHOW_TOOLBAR, true);
	}

	public void setShowToolbar(final boolean show)
	{
		setProperty(PROPERTY_SHOW_TOOLBAR, show);
	}
	
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Editor">
	public boolean getConsolidateLogMsg()
	{
		return getBoolProperty("workbench.gui.log.consolidate", false);
	}

	public void setConsolidateLogMsg(boolean aFlag)
	{
		this.setProperty("workbench.gui.log.consolidate", aFlag);
	}

	public boolean getPlainEditorWordWrap()
	{
		return getBoolProperty("workbench.editor.plain.wordwrap", true);
	}

	public void setPlainEditorWordWrap(boolean flag)
	{
		setProperty("workbench.editor.plain.wordwrap", flag);
	}

	public boolean getUsePlainEditorForData()
	{
		return getBoolProperty("workbench.gui.editor.data.plain", true);
	}
	
	/**
	 *  Returns the modifier key for rectangular selections in the editor
	 */
	public int getRectSelectionModifier()
	{
		String mod = getProperty("workbench.editor.rectselection.modifier", "alt");
		if (mod.equalsIgnoreCase("ctrl"))
		{
			return InputEvent.CTRL_MASK;
		}
		return InputEvent.ALT_MASK;
	}

	public void setRectSelectionModifier(String mod)
	{
		if (mod == null) return;
		if (mod.equalsIgnoreCase("alt"))
		{
			setProperty("workbench.editor.rectselection.modifier", "alt");
		}
		else if (mod.equalsIgnoreCase("ctrl"))
		{
			setProperty("workbench.editor.rectselection.modifier", "ctrl");
		}
	}
	
	public String getExternalEditorLineEnding()
	{
		return getLineEndingProperty("workbench.editor.lineending.external", DEFAULT_LINE_TERMINATOR_PROP_VALUE);
	}

	public void setExternalEditorLineEnding(String value)
	{
		setLineEndingProperty("workbench.editor.lineending.external", value);
	}

	public String getInternalEditorLineEnding()
	{
		return getLineEndingProperty("workbench.editor.lineending.internal", UNIX_LINE_TERMINATOR_PROP_VALUE);
	}

	public void setInternalEditorLineEnding(String value)
	{
		setLineEndingProperty("workbench.editor.lineending.internal", value);
	}

	/**
	 * The real setting for the external line ending property
	 * to be used by the options dialog
	 */
	public String getExternalLineEndingValue()
	{
		return getProperty("workbench.editor.lineending.external", DEFAULT_LINE_TERMINATOR_PROP_VALUE);
	}

	/**
	 * The real setting for the internal line ending property
	 * to be used by the options dialog
	 */
	public String getInteralLineEndingValue()
	{
		return getProperty("workbench.editor.lineending.internal", UNIX_LINE_TERMINATOR_PROP_VALUE);
	}

	private String getLineEndingProperty(String key, String def)
	{
		String value = getProperty(key, def);
		if (DEFAULT_LINE_TERMINATOR_PROP_VALUE.equalsIgnoreCase(value))
		{
			return StringUtil.LINE_TERMINATOR;
		}
		if (UNIX_LINE_TERMINATOR_PROP_VALUE.equalsIgnoreCase(value))
		{
			return "\n";
		}
		else if (DOS_LINE_TERMINATOR_PROP_VALUE.equalsIgnoreCase(value))
		{
			return "\r\n";
		}
		else
		{
			return "\n";
		}
	}

	private void setLineEndingProperty(String key, String value)
	{
		if (value == null) return;
		setProperty(key, value.toLowerCase());
	}
	
	public String getCodeSnippetPrefix()
	{
		String value = getProperty("workbench.editor.codeprefix", "String sql = ");
		return value;
	}
	
	public boolean getStoreFilesInHistory()
	{
		return getBoolProperty("workbench.sql.history.includefiles", true);
	}
	
	public void getStoreFilesInHistory(boolean flag)
	{
		setProperty("workbench.sql.history.includefiles", flag);
	}
	
	public int getMaxHistorySize()
	{
		return getIntProperty("workbench.sql.historysize", 15);
	}

	public void setMaxHistorySize(int aValue)
	{
		this.setProperty("workbench.sql.historysize", aValue);
	}
	
	public DelimiterDefinition getAlternateDelimiter(WbConnection con)
	{
		DelimiterDefinition delim = null;
		if (con != null && con.getProfile() != null)
		{
			delim = con.getProfile().getAlternateDelimiter();
		}
		return (delim == null ? getAlternateDelimiter() : delim);
	}
	
	public DelimiterDefinition getAlternateDelimiter()
	{
		String delim = getProperty("workbench.sql.alternatedelimiter", "/");
		boolean sld = getBoolProperty("workbench.sql.alternatedelimiter.singleline", true);
		if (StringUtil.isEmptyString(delim)) return null;
		DelimiterDefinition def = new DelimiterDefinition(delim, sld);
		if (def.isStandard()) return null;
		return def;
	}

	public void setAlternateDelimiter(DelimiterDefinition aDelimit)
	{
		this.setProperty("workbench.sql.alternatedelimiter", aDelimit.getDelimiter());
		this.setProperty("workbench.sql.alternatedelimiter.singleline", aDelimit.isSingleLine());
	}
	
	public boolean getRightClickMovesCursor()
	{
		return this.getBoolProperty("workbench.editor.rightclickmovescursor", false);
	}

	public void setRightClickMovesCursor(boolean flag)
	{
		setProperty("workbench.editor.rightclickmovescursor", flag);
	}

	public boolean getShowLineNumbers()
	{
		return getBoolProperty(PROPERTY_SHOW_LINE_NUMBERS, true);
	}

	public void setShowLineNumbers(boolean show)
	{
		setProperty(PROPERTY_SHOW_LINE_NUMBERS, show);
	}
	
	public boolean getAutoJumpNextStatement()
	{
		return getBoolProperty(PROPERTY_AUTO_JUMP_STATEMENT , false);
	}

	public void setAutoJumpNextStatement(boolean flag)
	{
		this.setProperty(PROPERTY_AUTO_JUMP_STATEMENT, flag);
	}

	public boolean getIgnoreErrors()
	{
		return StringUtil.stringToBool(this.props.getProperty("workbench.sql.ignoreerror", "false"));
	}

	public void setIgnoreErrors(boolean ignore)
	{
		this.setProperty("workbench.sql.ignoreerror", ignore);
	}
	
	public boolean getHighlightCurrentStatement()
	{
		return getBoolProperty(PROPERTY_HIGHLIGHT_CURRENT_STATEMENT, false);
	}

	public boolean getIncludeNewLineInCodeSnippet()
	{
		return getBoolProperty("workbench.javacode.includenewline", true);
	}

	public void setIncludeNewLineInCodeSnippet(boolean useEncryption)
	{
		this.setProperty("workbench.javacode.includenewline", useEncryption);
	}
	
	public void setEditorSelectionColor(Color c)
	{
		setColor("workbench.editor.color.selection", c);
	}

	public Color getEditorSelectionColor()
	{
		return getColor("workbench.editor.color.selection", new Color(0xccccff));
	}

	public void setEditorErrorColor(Color c)
	{
		setColor("workbench.editor.color.error", c);
	}

	public Color getEditorErrorColor()
	{
		return getColor("workbench.editor.color.error", Color.RED.brighter());
	}

	public Color getEditorCurrentLineColor()
	{
		return getColor(PROPERTY_EDITOR_CURRENT_LINE_COLOR, null);
	}

	public void setEditorCurrentLineColor(Color c)
	{
		setColor(PROPERTY_EDITOR_CURRENT_LINE_COLOR, c);
	}
	
	public int getElectricScroll()
	{
		return this.getIntProperty(PROPERTY_EDITOR_ELECTRIC_SCROLL, 0);
	}

	public void setElectricScroll(int value)
	{
		setProperty(PROPERTY_EDITOR_ELECTRIC_SCROLL, (value < 0 ? 0 : value));
	}
	
	public int getEditorTabWidth()
	{
		return getIntProperty(PROPERTY_EDITOR_TAB_WIDTH, 2);
	}

	public void setEditorTabWidth(int aWidth)
	{
		this.setProperty(PROPERTY_EDITOR_TAB_WIDTH, aWidth);
	}

	public String getEditorNoWordSep()
	{
		return getProperty("workbench.editor.nowordsep", "");
	}
	
	public void setEditorNoWordSep(String noSep)
	{
		setProperty("workbench.editor.nowordsep", noSep);
	}
	
	public void setAutoCompletionPasteCase(String value)
	{
		if (value != null)
		{
			if (value.toLowerCase().startsWith("lower")) setProperty("workbench.editor.autocompletion.paste.case", "lower");
			else if (value.toLowerCase().startsWith("upper")) setProperty("workbench.editor.autocompletion.paste.case", "upper");
			else setProperty("workbench.editor.autocompletion.paste.case", null);
		}
		else
		{
			setProperty("workbench.editor.autocompletion.paste.case", null);
		}
	}

	public ColumnSortType getAutoCompletionColumnSortType()
	{
		String sort = getProperty("workbench.editor.autocompletion.paste.sort", "name");
		try
		{
			return ColumnSortType.valueOf(sort);
		}
		catch (Exception e)
		{
			return ColumnSortType.name;
		}
	}

	public void setAutoCompletionColumnSort(String sort)
	{
		
		try
		{
			setAutoCompletionColumnSort(ColumnSortType.valueOf(sort));
		}
		catch (Exception e)
		{
			setAutoCompletionColumnSort(ColumnSortType.name);
		}
	}
	
	public void setAutoCompletionColumnSort(ColumnSortType sort)
	{
		setProperty("workbench.editor.autocompletion.paste.sort", (sort == ColumnSortType.position ? "position" : "name"));
	}
	
	public String getAutoCompletionPasteCase()
	{
		return getProperty("workbench.editor.autocompletion.paste.case", null);
	}

	public boolean getCloseAutoCompletionWithSearch()
	{
		return getBoolProperty("workbench.editor.autocompletion.closewithsearch", false);
	}

	public void setCloseAutoCompletionWithSearch(boolean flag)
	{
		setProperty("workbench.editor.autocompletion.closewithsearch", flag);
	}

	public boolean getAutoCompletionEmptyLineIsSeparator()
	{
		return getBoolProperty("workbench.editor.autocompletion.sql.emptylineseparator", false);
	}

	public boolean getAutoSaveWorkspace()
	{
		return getBoolProperty("workbench.workspace.autosave", false);
	}

	public void setAutoSaveWorkspace(boolean flag)
	{
		this.setProperty("workbench.workspace.autosave", flag);
	}

	public boolean getCreateWorkspaceBackup()
	{
		return getBoolProperty("workbench.workspace.createbackup", false);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Database">
	public String getPKMappingFilename()
	{
		String fName = System.getProperty(PK_MAPPING_FILENAME_PROPERTY, getProperty(PK_MAPPING_FILENAME_PROPERTY, null));
		if (StringUtil.isEmptyString(fName)) return null;
		return StringUtil.replace(fName, FileDialogUtil.CONFIG_DIR_KEY, getConfigDir().getAbsolutePath());
	}
	public void setPKMappingFilename(String file)
	{
		setProperty(PK_MAPPING_FILENAME_PROPERTY,file);
	}
	
	public boolean getPreviewDml()
	{
		return getBoolProperty("workbench.db.previewsql", true);
	}

	public void setPreviewDml(boolean aFlag)
	{
		this.props.setProperty("workbench.db.previewsql", Boolean.toString(aFlag));
	}

	public boolean getDebugMetadataSql()
	{
		return getBoolProperty("workbench.dbmetadata.debugmetasql", false);
	}

	public List<String> getServersWhereDDLNeedsCommit()
	{
		String list = getProperty("workbench.db.ddlneedscommit", "");
    return StringUtil.stringToList(list, ",");
	}

	public List<String> getServersWithInlineConstraints()
	{
		String list = getProperty("workbench.db.inlineconstraints", "");
		return StringUtil.stringToList(list, ",");
	}
	public List<String> getServersWhichNeedJdbcCommit()
	{
		String list = getProperty("workbench.db.usejdbccommit", "");
    return StringUtil.stringToList(list, ",");
	}

	public List<String> getServersWithNoNullKeywords()
	{
		String list = getProperty("workbench.db.nonullkeyword", "");
		return StringUtil.stringToList(list, ",");
	}

	public List<String> getCaseSensitivServers()
	{
		String list = getProperty("workbench.db.casesensitive", "");
		return StringUtil.stringToList(list, ",");
	}
	
	public boolean useOracleNVarcharFix()
	{
		return getBoolProperty("workbench.db.oracle.fixnvarchartype", true);
	}
	
	public boolean useOracleCharSemanticsFix()
	{
		return getBoolProperty("workbench.db.oracle.fixcharsemantics", true);
	}

	public boolean getCheckPreparedStatements()
	{
		return getBoolProperty("workbench.sql.checkprepared", false);
	}

	public void setCheckPreparedStatements(boolean flag)
	{
		this.setProperty("workbench.sql.checkprepared", flag);
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Export">
	public boolean getIncludeOwnerInSqlExport()
	{
		return this.getBoolProperty("workbench.export.sql.includeowner", true);
	}

	public void setIncludeOwnerInSqlExport(boolean flag)
	{
		setProperty("workbench.export.sql.includeowner", flag);
	}
	public String getDefaultTextDelimiter()
	{
		return this.getDefaultTextDelimiter(false);
	}

	public String getQuoteChar()
	{
		return getProperty("workbench.export.text.quotechar", "");
	}

	public void setQuoteChar(String aQuoteChar)
	{
		this.props.setProperty("workbench.export.text.quotechar", aQuoteChar);
	}

	public String getDefaultBlobTextEncoding()
	{
		return getProperty("workbench.blob.text.encoding", getDefaultDataEncoding());
	}

	public void setDefaultBlobTextEncoding(String enc)
	{
		setProperty("workbench.blob.text.encoding", enc);
	}

	public String getDefaultDataEncoding()
	{
		String def = System.getProperty("file.encoding");
		if ("Cp1252".equals(def)) def = "ISO-8859-15";
		return getProperty("workbench.file.data.encoding", def);
	}

	public String getDefaultFileEncoding()
	{
		String def = System.getProperty("file.encoding");
		return getProperty("workbench.file.encoding", def);
	}

	public void setDefaultFileEncoding(String enc)
	{
		this.props.setProperty("workbench.file.encoding", enc);
	}

	public String getDefaultTextDelimiter(boolean readable)
	{
		return getDelimiter("workbench.export.text.fielddelimiter", "\\t", readable);
	}

	public void setClipboardDelimiter(String delim)
	{
		setDelimiter("workbench.import.clipboard.fielddelimiter", delim);
	}

	public String getClipboardDelimiter(boolean readable)
	{
		return getDelimiter("workbench.import.clipboard.fielddelimiter", "\\t", readable);
	}
	
	public void setDelimiter(String prop, String delim)
	{
		if (delim.equals("\t")) delim = "\\t";
		setProperty("workbench.import.clipboard.fielddelimiter", delim);
	}
	
	public String getDelimiter(String prop, String def, boolean readable)
	{
		String del = getProperty(prop, def);
		if (readable)
		{
			if (del.equals("\t"))
			{
				del = "\\t";
			}
		}
		else
		{
			if (del.equals("\\t")) del = "\t";
		}

		return del;
		
	}
	public void setDefaultTextDelimiter(String aDelimit)
	{
		if (aDelimit.equals("\t")) aDelimit = "\\t";

		this.props.setProperty("workbench.export.text.fielddelimiter", aDelimit);
	}

	public String getLastImportDelimiter(boolean readable)
	{
		return getDelimiter("workbench.import.text.fielddelimiter", "\\t", readable);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Import">
	public void setLastImportDelimiter(String aDelimit)
	{
		if (aDelimit.equals("\t")) aDelimit = "\\t";

		this.props.setProperty("workbench.import.text.fielddelimiter", aDelimit);
	}

	public boolean getLastImportWithHeaders()
	{
		return getBoolProperty("workbench.import.text.containsheader", true);
	}

	public void setLastImportWithHeaders(boolean aFlag)
	{
		this.props.setProperty("workbench.import.text.containsheader", Boolean.toString(aFlag));
	}
	
	public String getLastImportDateFormat()
	{
		return getProperty("workbench.import.dateformat", this.getDefaultDateFormat());
	}

	public void setLastImportDateFormat(String aFormat)
	{
		this.props.setProperty("workbench.import.dateformat", aFormat);
	}

	public void setLastImportNumberFormat(String aFormat)
	{
		this.props.setProperty("workbench.import.numberformat", aFormat);
	}

	public String getLastImportNumberFormat()
	{
		String result = getProperty("workbench.import.numberformat", null);
		if (result == null)
		{
			result = "#" + this.getDecimalSymbol() + "#";
		}
		return result;
	}

	public boolean getLastImportDecode()
	{
		return this.getBoolProperty("workbench.import.decode");
	}

	public void setLastImportDecode(boolean flag)
	{
		this.setProperty("workbench.import.decode", flag);
	}

	public String getLastImportQuoteChar()
	{
		return getProperty("workbench.import.quotechar", "\"");
	}

	public void setLastImportQuoteChar(String aChar)
	{
		this.props.setProperty("workbench.import.quotechar", aChar);
	}

	public String getLastImportDir()
	{
		return getProperty("workbench.import.lastdir", this.getLastExportDir());
	}

	public void setLastImportDir(String aDir)
	{
		this.props.setProperty("workbench.import.lastdir", aDir);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Directories">
	public String getLastLibraryDir()
	{
		return getProperty("workbench.drivers.lastlibdir", "");
	}

	public void setLastLibraryDir(String aDir)
	{
		this.props.setProperty("workbench.drivers.lastlibdir", aDir);
	}
	
	public String getLastBlobDir()
	{
		return getProperty("workbench.data.blob.save.lastdir", null);
	}

	public void setLastBlobDir(String aDir)
	{
		this.setProperty("workbench.data.blob.save.lastdir", aDir);
	}

	public String getLastWorkspaceDir()
	{
		return getProperty("workbench.workspace.lastdir", this.getConfigDir().getAbsolutePath());
	}

	public void setLastWorkspaceDir(String aDir)
	{
		this.props.setProperty("workbench.workspace.lastdir", aDir);
	}

	public String getLastExportDir()
	{
		return getProperty("workbench.export.lastdir","");
	}

	public void setLastExportDir(String aDir)
	{
		this.props.setProperty("workbench.export.lastdir", aDir);
	}

	public String getLastSqlDir()
	{
		return getProperty("workbench.sql.lastscriptdir","");
	}

	public void setLastSqlDir(String aDir)
	{
		this.props.setProperty("workbench.sql.lastscriptdir", aDir);
	}

	public String getLastEditorDir()
	{
		return getProperty("workbench.editor.lastdir","");
	}

	public void setLastEditorDir(String aDir)
	{
		this.props.setProperty("workbench.editor.lastdir", aDir);
	}

	public String getLastFilterDir()
	{
		return getProperty("workbench.filter.lastdir","");
	}

	public void setLastFilterDir(String dir)
	{
		this.props.setProperty("workbench.filter.lastdir",dir);
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="Date and Time Formatting">
	private SimpleDateFormat defaultDateFormatter = null;
	private SimpleDateFormat defaultTimestampFormatter = null;

	public SimpleDateFormat getDefaultDateFormatter()
	{
		if (this.defaultDateFormatter == null)
		{
			this.defaultDateFormatter = new SimpleDateFormat(this.getDefaultDateFormat());
		}
		return this.defaultDateFormatter;
	}

	public void setDefaultDateFormat(String aFormat)
	{
		this.defaultDateFormatter = null;
		this.props.setProperty(PROPERTY_DATE_FORMAT, aFormat);
	}

	public void registerDateFormatChangeListener(PropertyChangeListener l)
	{
    this.addPropertyChangeListener(l, PROPERTY_DATE_FORMAT, PROPERTY_DATETIME_FORMAT, PROPERTY_TIME_FORMAT);
	}

	public boolean isDateFormatProperty(String prop)
	{
		if (prop == null) return false;
		return (PROPERTY_DATE_FORMAT.equals(prop) || PROPERTY_DATETIME_FORMAT.equals(prop) || PROPERTY_TIME_FORMAT.equals(prop));
	}

	public String getDefaultDateFormat()
	{
		return getProperty(PROPERTY_DATE_FORMAT, StringUtil.ISO_DATE_FORMAT);
	}

	public String getDefaultTimestampFormat()
	{
		return getProperty(PROPERTY_DATETIME_FORMAT, StringUtil.ISO_TIMESTAMP_FORMAT);
	}

	public void setDefaultTimestampFormat(String aFormat)
	{
		this.defaultDateFormatter = null;
		this.props.setProperty(PROPERTY_DATETIME_FORMAT, aFormat);
	}

	public void setDefaultTimeFormat(String format)
	{
		this.props.setProperty(PROPERTY_TIME_FORMAT, format);
	}

	public String getDefaultTimeFormat()
	{
		return getProperty(PROPERTY_TIME_FORMAT, "HH:mm:ss");
	}

	public SimpleDateFormat getDefaultTimestampFormatter()
	{
		if (this.defaultTimestampFormatter == null)
		{
			this.defaultTimestampFormatter = new SimpleDateFormat(this.getDefaultTimestampFormat());
		}
		return this.defaultTimestampFormatter;
	}

	public int getMaxFractionDigits()
	{
		return getIntProperty("workbench.gui.display.maxfractiondigits", 2);
	}

	public void setMaxFractionDigits(int aValue)
	{
		this.props.setProperty("workbench.gui.display.maxfractiondigits", Integer.toString(aValue));
		this.defaultDecimalFormatter = null;
	}

	private DecimalFormat defaultDecimalFormatter = null;
	private DecimalFormatSymbols decSymbols = new DecimalFormatSymbols();

	public DecimalFormat getDefaultDecimalFormatter()
	{
		this.initFormatter();
		return this.defaultDecimalFormatter;
	}

	private void initFormatter()
	{
		if (this.defaultDecimalFormatter == null)
		{
			this.defaultDecimalFormatter = new DecimalFormat("0.#");
			String sep = this.getDecimalSymbol();
			int maxDigits = this.getMaxFractionDigits();
			this.decSymbols.setDecimalSeparator(sep.charAt(0));
			this.defaultDecimalFormatter.setDecimalFormatSymbols(this.decSymbols);
			this.defaultDecimalFormatter.setMaximumFractionDigits(maxDigits);
		}
	}

	public String getDecimalSymbol()
	{
		return getProperty("workbench.gui.display.decimal.separator", ".");
	}

	public void setDecimalSymbol(String aSep)
	{
		this.props.setProperty("workbench.gui.display.decimal.separator", aSep);
		this.defaultDecimalFormatter = null;
	}

	// </editor-fold>
	
	public String getSqlParameterPrefix()
	{
		String value = getProperty("workbench.sql.parameter.prefix", "$[");
		if (StringUtil.isEmptyString(value)) value = "$[";
		return value;
	}

	public String getSqlParameterSuffix()
	{
		return getProperty("workbench.sql.parameter.suffix", "]");
	}

	public int getMaxLogfileSize()
	{
		return this.getIntProperty("workbench.log.maxfilesize", 30000);
	}
	
	public boolean getCheckEscapedQuotes()
	{
		return getBoolProperty("workbench.sql.checkescapedquotes", false);
	}

	public void setCheckEscapedQuotes(boolean flag)
	{
		this.setProperty("workbench.sql.checkescapedquotes", flag);
	}

	// <editor-fold defaultstate="collapsed" desc="Connections">
	public boolean getAutoConnectDataPumper()
	{
		return getBoolProperty("workbench.datapumper.autoconnect", true);
	}

	public void setAutoConnectDataPumper(boolean flag)
	{
		this.setProperty("workbench.datapumper.autoconnect", flag);
	}
	
	public ProfileKey getLastConnection(String key)
	{
		if (key == null)
		{
			key = "workbench.connection.last";
		}
		String name = getProperty(key, null);
		if (name == null) return null;
		String group = getProperty(key + ".group", null);
		return new ProfileKey(name, group);
	}

	public void setLastConnection(ConnectionProfile prof)
	{
		setLastConnection("workbench.connection.last", prof);
	}

	public void setLastConnection(String key, ConnectionProfile prof)
	{
		if (prof == null)
		{
			this.props.setProperty(key, "");
			this.props.setProperty(key + ".group", "");
		}

		// comparing with == is intended!!!!
		if (prof.getName() == BatchRunner.CMD_LINE_PROFILE_NAME) return;

		this.props.setProperty(key, prof.getName());
		this.props.setProperty(key + ".group", prof.getGroup());
	}
	// </editor-fold>

	public int getInMemoryScriptSizeThreshold()
	{
		// Process scripts up to 1 MB in memory
		// this is used by the ScriptParser
		return getIntProperty("workbench.sql.script.inmemory.maxsize", 1024 * 1024);
	}

	public void setInMemoryScriptSizeThreshold(int size)
	{
		setProperty("workbench.sql.script.inmemory.maxsize", size);
	}

	public boolean getUseCollator()
	{
		return getBoolProperty("workbench.sort.usecollator", false);
	}

	public String getSortLanguage()
	{
		return getProperty("workbench.sort.language", System.getProperty("user.language"));
	}

	public String getSortCountry()
	{
		return getProperty("workbench.sort.country", System.getProperty("user.country"));
	}

	public void setGeneratedSqlTableCase(String value)
	{
		if (value != null)
		{
			if (value.toLowerCase().startsWith("lower")) setProperty("workbench.sql.generate.table.case", "lower");
			else if (value.toLowerCase().startsWith("upper")) setProperty("workbench.sql.generate.table.case", "upper");
			else setProperty("workbench.sql.generate.table.case", "original");
		}
		else
		{
			setProperty("workbench.sql.generate.table.case", "original");
		}
	}

	public String getGeneratedSqlTableCase()
	{
		return getProperty("workbench.sql.generate.table.case", getAutoCompletionPasteCase());
	}

	public boolean getUseEncryption()
	{
		return getBoolProperty(PROPERTY_ENCRYPT_PWD, false);
	}

	public void setUseEncryption(boolean useEncryption)
	{
		this.setProperty(PROPERTY_ENCRYPT_PWD, useEncryption);
	}
	
	// <editor-fold defaultstate="collapsed" desc="Utility">
	public void removeProperty(String property)
	{
		this.props.remove(property);
	}

	public boolean getBoolProperty(String property)
	{
		return getBoolProperty(property, false);
	}

	public boolean getBoolProperty(String property, boolean defaultValue)
	{
		String sysValue = System.getProperty(property, null);
		if (sysValue != null)
		{
			return StringUtil.stringToBool(sysValue);
		}
		return this.props.getBoolProperty(property, defaultValue);
	}

	public void setProperty(String property, boolean value)
	{
		this.props.setProperty(property, value);
	}

	public Object setProperty(String aProperty, String aValue)
	{
		return this.props.setProperty(aProperty, aValue);
	}

	public void setProperty(String aProperty, int aValue)
	{
		this.props.setProperty(aProperty, Integer.toString(aValue));
	}

	public String getProperty(String aProperty, String aDefault)
	{
		return System.getProperty(aProperty, this.props.getProperty(aProperty, aDefault));
	}

	public List<String> getListProperty(String aProperty, boolean makeLowerCase)
	{
		String list = System.getProperty(aProperty, this.props.getProperty(aProperty, null));
		if (makeLowerCase && list != null)
		{
			list = list.toLowerCase();
		}
		return StringUtil.stringToList(list, ",", true, true, false);
	}
	
	public int getIntProperty(String aProperty, int defaultValue)
	{
		String sysValue = System.getProperty(aProperty, null);
		if (sysValue != null)
		{
			return StringUtil.getIntValue(sysValue, defaultValue);
		}
		return this.props.getIntProperty(aProperty, defaultValue);
	}

	private void setColor(String key, Color c)
	{
		String value = null;
		if (c != null)
		{
			int r = c.getRed();
			int g = c.getGreen();
			int b = c.getBlue();
			value = Integer.toString(r) + "," + Integer.toString(g) + "," + Integer.toString(b);
		}
		this.setProperty(key, value);
	}
	
	public int getWindowPosX(String windowClass)
	{
		return getIntProperty(windowClass + ".x", Integer.MIN_VALUE);
	}

	public int getWindowPosY(String windowClass)
	{
		return getIntProperty(windowClass + ".y", Integer.MIN_VALUE);
	}

	public int getWindowWidth(String windowClass)
	{
		return getIntProperty(windowClass + ".width", Integer.MIN_VALUE);
	}

	public int getWindowHeight(String windowClass)
	{
		return getIntProperty(windowClass + ".height", Integer.MIN_VALUE);
	}
	
	public void storeWindowPosition(Component target)
	{
		this.storeWindowPosition(target, target.getClass().getName());
	}

	public void storeWindowPosition(Component target, String id)
	{
		Point p = target.getLocation();
		this.setWindowPosition(id, p.x, p.y);
	}

	public void storeWindowSize(Component target)
	{
		this.storeWindowSize(target, null);
	}

	public void storeWindowSize(Component target, String id)
	{
		if (target == null) return;
		Dimension d = target.getSize();
		if (id == null) id = target.getClass().getName();
		this.setWindowSize(id, d.width, d.height);
	}

	public void setWindowPosition(String windowClass, int x, int y)
	{
		this.props.setProperty(windowClass + ".x", Integer.toString(x));
		this.props.setProperty(windowClass + ".y", Integer.toString(y));
	}

	public void setWindowSize(String windowClass, int width, int height)
	{
		this.props.setProperty(windowClass + ".width", Integer.toString(width));
		this.props.setProperty(windowClass + ".height", Integer.toString(height));
	}

	public boolean restoreWindowSize(Component target)
	{
		return this.restoreWindowSize(target, target.getClass().getName());
	}

	public boolean restoreWindowSize(final Component target, final String id)
	{
		boolean result = false;
		final int w = this.getWindowWidth(id);
		final int h = this.getWindowHeight(id);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		if (w > 0 && h > 0 && w <= screen.getWidth() && h <= screen.getHeight())
		{
			result = true;
			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					target.setSize(new Dimension(w, h));
				}
			});
		}
		return result;
	}

	public boolean restoreWindowPosition(Component target)
	{
		return this.restoreWindowPosition(target, target.getClass().getName());
	}

	public boolean restoreWindowPosition(final Component target, final String id)
	{
		boolean result = false;
		final int x = this.getWindowPosX(id);
		final int y = this.getWindowPosY(id);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE 
			&& x <= screen.getWidth() - 20 && y <= screen.getHeight() - 20)
		{
			result = true;
			WbSwingUtilities.invoke(new Runnable()
			{
				public void run()
				{
					target.setLocation(new Point(x, y));
				}
			});
		}
		return result;
	}
	
	private void migrateProps()
	{
		// Fix incorrectly distributed defaults
		String defaultSelectable = getProperty("workbench.db.objecttype.selectable.default", null);
		if (defaultSelectable != null)
		{
			List<String> types = StringUtil.stringToList(defaultSelectable.toLowerCase(), ",", true, true, false);
			if (!types.contains("synonym"))
			{
				types.add("synonym");
				setProperty("workbench.db.objecttype.selectable.default", StringUtil.listToString(types, ','));
			}
		}
		
		String synRegex = getProperty("workbench.db.oracle.exclude.synonyms", null);
		if (synRegex != null && !synRegex.startsWith("^/.*"))
		{
			synRegex = "^/.*|" + synRegex;
			setProperty("workbench.db.oracle.exclude.synonyms", synRegex);
		}
		
		WbProperties def = getDefaultProperties();
		upgradeListProp(def, "workbench.db.oracle.syntax.functions");
		
		// Adjust the patterns for SELECT ... INTO
		upgradeProp(def, "workbench.db.postgresql.selectinto.pattern", "(?s)^SELECT\\s+.*INTO\\s+\\p{Print}*\\s*FROM.*");
		upgradeProp(def, "workbench.db.informix_dynamic_server.selectinto.pattern", "(?s)^SELECT.*FROM.*INTO\\s*\\p{Print}*");
	}

	private void upgradeProp(WbProperties defProps, String property, String originalvalue)
	{
		String p = getProperty(property, "");
		
		// Make sure it has not been modified
		if (originalvalue.equals(p))
		{
			String newprop = defProps.getProperty("workbench.db.postgresql.selectinto.pattern", null);
			setProperty("workbench.db.postgresql.selectinto.pattern", newprop);
		}
	}
	
	private void upgradeListProp(WbProperties defProps, String key)
	{
		String currentValue = getProperty(key, "");
		List<String> currentList = StringUtil.stringToList(currentValue, ",", true, true, false);
		
		// Use a HashSet to ensure that no duplicates are contained in the list
		Set<String> currentProps = new HashSet<String>();
		currentProps.addAll(currentList);
		
		String defValue = defProps.getProperty(key, "");
		List<String> defList = StringUtil.stringToList(defValue, ",", true, true, false);
		currentProps.addAll(defList);
		this.setProperty(key, StringUtil.listToString(currentProps,','));
	}
	
	private void renameOldProps()
	{
		this.renameProperty("workbench.sql.maxcolwidth","workbench.gui.optimalwidth.maxsize");
		this.renameProperty("workbench.sql.mincolwidth","workbench.gui.optimalwidth.minsize");
		this.renameProperty("sort.language", "workbench.sort.language");
		this.renameProperty("sort.country", "workbench.sort.country");
		this.renameProperty("connection.last", "workbench.connection.last");
		this.renameProperty("drivers.lastlibdir", "workbench.drivers.lastlibdir");
		this.renameProperty("workbench.db.debugger", "workbench.db.previewsql");

		// Fix typos from incorrect default.properties
		this.renameProperty("workbench.db.objecttype.data.postgres", "workbench.db.objecttype.data.postgresql");
		this.renameProperty("workbench.db.objecttype.selectable.postgres", "workbench.db.objecttype.selectable.postgresql");
		this.renameProperty("workbench.ignoretypes.postgres", "workbench.ignoretypes.postgresql");
		String s = getProperty("workbench.db.truncatesupported",null);
		if (s!=null)
		{
			s = s.replaceAll(",postgres,",",postgresql,");
			this.setProperty("workbench.db.truncatesupported",s);
		}
		this.renameProperty("workbench.history.tablelist", "workbench.quickfilter.tablelist.history");
		this.renameProperty("workbench.history.columnlist", "workbench.quickfilter.columnlist.history");
		this.renameProperty("workbench.gui.dbobjects.ProcedureListPanel.lastsearch", "workbench.quickfilter.procedurelist.history");
		this.renameProperty("workbench.blob.text.encoding", "workbench.gui.blob.text.encoding");
	}

	private void renameProperty(String oldKey, String newKey)
	{
		if (this.props.containsKey(oldKey))
		{
			Object value = this.props.get(oldKey);
			this.props.remove(oldKey);
			this.props.put(newKey, value);
		}
	}
	private void removeObsolete()
	{
		try
		{
			this.props.remove("workbench.db.fetchsize");
			this.props.remove("workbench.editor.java.lastdir");
			this.props.remove("workbench.sql.replace.ignorecase");
			this.props.remove("workbench.sql.replace.selectedtext");
			this.props.remove("workbench.sql.replace.useregex");
			this.props.remove("workbench.sql.replace.wholeword");
			this.props.remove("workbench.sql.search.ignorecase");
			this.props.remove("workbench.sql.search.useregex");
			this.props.remove("workbench.sql.search.wholeword");
			this.props.remove("workbench.sql.search.lastvalue");
			this.props.remove("workbench.dbexplorer.rememberSchema");
			this.props.remove("workbench.db.postgres.select.startstransaction");
			this.props.remove("workbench.db.oracle.quotedigits");
			this.props.remove("workbench.gui.macros.replaceonrun");
			this.props.remove("workbench.db.cancelneedsreconnect");
			this.props.remove("workbench.db.trigger.replacenl");
			this.props.remove("workbench.sql.multipleresultsets");
			
			this.props.remove("workbench.db.keywordlist.oracle");
			this.props.remove("workbench.db.keywordlist.thinksql_relational_database_management_system");
			
			this.props.remove("workbench.gui.settings.ExternalToolsPanel.divider");
			this.props.remove("workbench.gui.settings.LnFOptionsPanel.divider");
			this.props.remove("workbench.gui.profiles.DriverlistEditorPanel.divider");
			
			this.props.remove("workbench.gui.dbobjects.TableListPanel.quickfilter.history");
			this.props.remove("workbench.gui.dbobjects.TableListPanel.quickfilter.lastvalue");

			boolean mySQLRemoved = getBoolProperty("workbench.migrate.settings.mysql.cascade", false);
			if (!mySQLRemoved)
			{
				// Only remove them once!
				this.props.remove("workbench.db.drop.table.cascade.mysql");
				this.props.remove("workbench.db.drop.view.cascade.mysql");
				this.props.remove("workbench.db.drop.function.cascade.mysql");
				setProperty("workbench.migrate.settings.mysql.cascade", true);
			}
			
			// Starting with build 95 no default standard font should be used 
			// (to make sure the default font of the Look & Feel is used)
			// Only if the user sets one through the Options dialog
			// The "user-defined" standard font is then saved with 
			// the property key workbench.font.std.XXXX
			this.props.remove("workbench.font.standard.name");
			this.props.remove("workbench.font.standard.size");
			this.props.remove("workbench.font.standard.style");
			
			this.props.remove("workbench.db.sql_server.batchedstatements");
			this.props.remove("workbench.db.sql_server.currentcatalog.query");
			this.props.remove("workbench.db.sql_server.objectname.case");
			this.props.remove("workbench.db.sql_server.schemaname.case");

			this.props.remove("workbench.dbexplorer.visible");
			
			// DbMetadata now uses db2 as the dbid for all DB2 versions (stripping the _linux or _nt suffix)
      this.props.remove("workbench.db.db2_nt.currentschema.query");
      this.props.remove("workbench.db.objecttype.selectable.db2_nt");
      this.props.remove("workbench.db.db2_nt.synonymtypes");
      this.props.remove("workbench.db.db2_nt.additional.viewtypes");
      this.props.remove("workbench.db.db2_nt.retrieve_sequences");
      this.props.remove("workbench.db.db2_nt.additional.tabletypes");
			
			this.props.remove("workbench.sql.dbms_output.defaultbuffer");
			this.props.remove("workbench.sql.enable_dbms_output");
			
			this.props.remove("workbench.db.stripprocversion");
			this.props.remove("workbench.dbexplorer.cleardata");
			this.props.remove("workbench.db.verifydriverurl");
		}
		catch (Throwable e)
		{
			LogMgr.logWarning("Settings.removeObsolete()", "Error when removing obsolete properties", e);
		}
	}

	private WbProperties getDefaultProperties()
	{
		WbProperties defProps = new WbProperties(this);
		InputStream in = ResourceMgr.getDefaultSettings();
		try
		{
			defProps.load(in);
		}
		catch (IOException e)
		{
			LogMgr.logError(this, "Could not read default settings", e);
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		return defProps;
	}
	
	private void fillDefaults()
	{
		InputStream in = ResourceMgr.getDefaultSettings();
		try
		{
			this.props.load(in);
		}
		catch (IOException e)
		{
			LogMgr.logError(this, "Could not read default settings", e);
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
	}
	// </editor-fold>

	public void saveSettings()
	{
		if (this.props == null) return;
		if (keyManager!= null) this.keyManager.saveSettings();
		try
		{
			this.props.saveToFile(this.configfile);
		}
		catch (IOException e)
		{
			LogMgr.logError(this, "Error saving Settings file '" + configfile.getFullPath() + "'", e);
		}
		if (this.getPKMappingFilename() != null && PkMapping.isInitialized())
		{
			PkMapping.getInstance().saveMapping(this.getPKMappingFilename());
		}
	}
	
	public String toString()
	{
		return "[Settings]";
	}

}
