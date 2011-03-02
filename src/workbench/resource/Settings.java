/*
 * Settings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.UIManager;
import workbench.WbManager;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.profiles.ProfileKey;
import workbench.gui.settings.ExternalFileHandling;
import workbench.interfaces.PropertyStorage;
import workbench.interfaces.FontChangedListener;
import workbench.log.LogMgr;
import workbench.sql.DelimiterDefinition;
import workbench.sql.BatchRunner;
import workbench.storage.PkMapping;
import workbench.util.FileDialogUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.ToolDefinition;
import workbench.util.WbFile;
import workbench.util.WbLocale;
import workbench.util.WbProperties;

/**
 * The singleton to manage configuration settings for SQL Workbench/J
 *
 * @author Thomas Kellerer
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
	public static final String PROPERTY_SHOW_TOOLBAR = "workbench.gui.mainwindow.showtoolbar";
	public static final String PROPERTY_TAB_POLICY = "workbench.gui.mainwindow.tabpolicy";
	public static final String PROPERTY_SHOW_LINE_NUMBERS = "workbench.editor.showlinenumber";
	public static final String PROPERTY_HIGHLIGHT_CURRENT_STATEMENT = "workbench.editor.highlightcurrent";
	public static final String PROPERTY_AUTO_JUMP_STATEMENT = "workbench.editor.autojumpnext";
	public static final String PROPERTY_DBEXP_REMEMBER_SORT = "workbench.dbexplorer.remembersort";
	public static final String PROPERTY_SHOW_TAB_INDEX = "workbench.gui.tabs.showindex";

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
	public static final String PROPERTY_EDITOR_BRACKET_HILITE_COLOR = "workbench.editor.bracket.hilite.color";
	public static final String PROPERTY_EDITOR_ELECTRIC_SCROLL = "workbench.editor.electricscroll";
	public static final String PROPERTY_EDITOR_BG_COLOR = "workbench.editor.color.background";
	public static final String PROPERTY_EDITOR_FG_COLOR = "workbench.editor.color.foreground";
	public static final String PROPERTY_EDITOR_CURSOR_COLOR = "workbench.editor.color.cursor";
	public static final String PROPERTY_EDITOR_DATATYPE_COLOR = "workbench.editor.color.datatype";

	public static final String PROPERTY_LOG_ALL_SQL = "workbench.sql.log.statements";
	// </editor-fold>

	public static final String TEST_MODE_PROPERTY = "workbench.gui.testmode";

	public static final String PK_MAPPING_FILENAME_PROPERTY = "workbench.pkmapping.file";
	public static final String UNIX_LINE_TERMINATOR_PROP_VALUE = "lf";
	public static final String DOS_LINE_TERMINATOR_PROP_VALUE = "crlf";
	public static final String DEFAULT_LINE_TERMINATOR_PROP_VALUE = "default";

	private static final String LIB_DIR_KEY = "%LibDir%";

	private WbProperties props;
	private WbFile configfile;

	private List<FontChangedListener> fontChangeListeners = new ArrayList<FontChangedListener>(5);
	private List<SettingsListener> saveListener = new ArrayList<SettingsListener>(5);

	private long fileTime;

	/**
	 * Thread safe singleton-instance
	 */
	protected static class LazyInstanceHolder
	{
		protected static final Settings instance = new Settings();
	}

	public static Settings getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	protected Settings()
	{
		initialize();
		renameOldProps();
		migrateProps();
		removeObsolete();
	}

	@Override
	public Set<String> getKeys()
	{
		return props.getKeys();
	}

	public final void initialize()
	{
		final String configFilename = "workbench.settings";

		// The check for a null WbManager is necessary to allow design-time loading
		// of some GUI forms in NetBeans. As they access the ResourceMgr and that in turn
		// usess the Settings class, this class might be instantiated without a valid WbManager
		if (WbManager.getInstance() != null)
		{
			// Make the installation directory available as a system property as well.
			// this can e.g. be used to defined the location of the logfile relative
			// to the installation path
			System.setProperty("workbench.install.dir", WbManager.getInstance().getJarPath());
		}

		WbFile cfd = null;
		try
		{
			String configDir = StringUtil.replaceProperties(System.getProperty("workbench.configdir", null));
			configDir = StringUtil.trimQuotes(configDir);
			if (StringUtil.isBlank(configDir))
			{
				// check the current directory for a configuration file
				// if it is not present, then use the directory of the jar file
				// this means that upon first startup, the settings file
				// will be created in the directory of the jar file
				File f = new File(System.getProperty("user.dir"), configFilename);
				if (f.exists())
				{
					cfd = new WbFile(f.getParentFile());
				}
				else if (WbManager.getInstance() != null)
				{
					cfd = new WbFile(WbManager.getInstance().getJarPath());
					f = new File(cfd,configFilename);
					if (!f.exists())
					{
						cfd = null;
					}
				}
			}
			else
			{
				cfd = new WbFile(configDir);
			}
		}
		catch (Exception e)
		{
			cfd = new WbFile(System.getProperty("user.home"), ".sqlworkbench");
		}

		if (cfd == null)
		{
			// no config file in the jar directory --> create a config directory in user.home
			cfd = new WbFile(System.getProperty("user.home"), ".sqlworkbench");
		}

		if (!cfd.exists() && WbManager.getInstance() != null && WbManager.getInstance().getSettingsShouldBeSaved())
		{
			cfd.mkdirs();
		}

		WbFile settings = new WbFile(cfd, configFilename);

		boolean configLoaded = loadConfig(settings);

		if (configfile != null)
		{
			// Make the configuration directory available through a system property
			// So that e.g. a log4j.xml can reference the directory using ${workbench.config.dir}
			System.setProperty("workbench.config.dir", configfile.getParentFile().getAbsolutePath());
		}

		if (configLoaded || isTestMode())
		{
			initLogging();

			// This message should not be logged before initLogging() has been called!
			LogMgr.logInfo("Settings.<init>", "Using configdir: " + configfile.getParentFile().getAbsolutePath());

			if (getBoolProperty("workbench.db.resetdefaults"))
			{
				LogMgr.logInfo("Settings.<init>", "Resetting database properties to built-in defaults");
				resetDefaults();
			}
		}
	}

	private boolean initLog4j()
	{
		String log4jParameter = StringUtil.replaceProperties(getProperty("workbench.log.log4j", "false"));
		String log4jConfig = null;

		boolean useLog4j = false;
		if ("true".equalsIgnoreCase(log4jParameter) || "false".equalsIgnoreCase(log4jParameter))
		{
			useLog4j = StringUtil.stringToBool(log4jParameter);
		}
		else
		{
			File f = new File(log4jParameter);
			if (f.exists())
			{
				// Assume this is a log4j configuration file (.xml or .properties)
				useLog4j = true;
				log4jConfig = log4jParameter;
			}
		}

		if (useLog4j)
		{
			if (StringUtil.isNonBlank(log4jConfig))
			{
				try
				{
					File f = new File(log4jConfig);
					if (!f.isAbsolute() && configfile != null)
					{
						f = new File(configfile.getParentFile(), log4jConfig);
					}

					if (f.exists())
					{
						String fileUrl = f.toURI().toString();
						System.setProperty("log4j.configuration", fileUrl);
					}
				}
				catch (Throwable th)
				{
					// ignore
				}
			}
		}
		return useLog4j;
	}

	private void initLogging()
	{
		boolean useLog4j = initLog4j();

		LogMgr.init(useLog4j);

		boolean logSysErr = getBoolProperty("workbench.log.console", false);
		LogMgr.logToSystemError(logSysErr);

		String format = getProperty("workbench.log.format", "{timestamp} {type} {message} {error}");
		LogMgr.setMessageFormat(format);

		String level = getProperty("workbench.log.level", "INFO");
		LogMgr.setLevel(level);

		try
		{
			String logfilename = StringUtil.replaceProperties(getProperty("workbench.log.filename", "workbench.log"));

			// Replace old System.out or System.err settings
			if (logfilename.equalsIgnoreCase("System.out") || logfilename.equalsIgnoreCase("System.err"))
			{
				logfilename = "workbench.log";
				setProperty("workbench.log.filename", "workbench.log");
			}

			WbFile logfile = new WbFile(logfilename);
			if (!logfile.isAbsolute())
			{
				logfile = new WbFile(getConfigDir(), logfilename);
			}

			String old = null;
			if (!logfile.isWriteable())
			{
				old = logfile.getFullPath();
				logfile = new WbFile(getConfigDir(), "workbench.log");
				setProperty("workbench.log.filename", "workbench.log");
			}

			int maxSize = this.getMaxLogfileSize();
			LogMgr.setOutputFile(logfile, maxSize);
			if (old != null)
			{
				LogMgr.logWarning("Settings.<init>", "Could not write requested logfile '" + old + "'");
			}
		}
		catch (Throwable e)
		{
			System.err.println("Error initializing log system!");
			e.printStackTrace(System.err);
		}
	}

	private boolean loadConfig(WbFile cfile)
	{
		if (cfile == null) return false;
		long time = cfile.lastModified();

		if (cfile.equals(this.configfile) && this.fileTime == time)
		{
			// same file, same modification time --> nothing to do
			return false;
		}

		this.configfile = cfile;
		this.props = new WbProperties(this);
		this.fileTime = time;

		fillDefaults();

		if (cfile.exists() && cfile.length() > 0)
		{
			// default comments should only be read the very first time.
			props.clearComments();
		}

		BufferedInputStream in = null;
		try
		{
			in = new BufferedInputStream(new FileInputStream(this.configfile), 32*1024);
			this.props.loadFromStream(in);
		}
		catch (IOException e)
		{
			fillDefaults();
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
		return true;
	}

	private void resetDefaults()
	{
		WbProperties defaults = getDefaultProperties();
		for (String key : defaults.stringPropertyNames())
		{
			if (key.startsWith("workbench.db"))
			{
				setProperty(key, defaults.getProperty(key));
			}
		}
	}

	/**
	 * Return all keys that contain the specified string
	 */
	public List<String> getKeysLike(String partialKey)
	{
		return props.getKeysWithPrefix(partialKey);
	}

	public void addSaveListener(SettingsListener l)
	{
		saveListener.add(l);
	}

	public void removeSaveListener(SettingsListener l)
	{
		saveListener.remove(l);
	}

	public String replaceProperties(String input)
	{
		return StringUtil.replaceProperties(props, input);
	}

	public void setUseSinglePageHelp(boolean flag)
	{
		setProperty("workbench.help.singlepage", flag);
	}

	public boolean useSinglePageHelp()
	{
		return getBoolProperty("workbench.help.singlepage", false);
	}

	// <editor-fold defaultstate="collapsed" desc="Language settings">
	public void setLanguage(Locale locale)
	{
		setProperty("workbench.gui.language", locale.getLanguage());
	}

	public List<WbLocale> getLanguages()
	{
		List<String> codes = getListProperty("workbench.gui.languages.available", false, "en,de");
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

	// <editor-fold defaultstate="collapsed" desc="Settings Configuration">
	public String getDriverConfigFilename()
	{
		return new WbFile(getConfigDir(), "WbDrivers.xml").getFullPath();
	}

	public File getColumnOrderStorage()
	{
		return new WbFile(getConfigDir(), "WbColumnOrder.xml");
	}

	public File getConfigDir()
	{
		return this.configfile.getParentFile();
	}

	public String replaceLibDirKey(String aPathname)
	{
		if (aPathname == null) return null;
		WbFile libDir = getLibDir();
		return StringUtil.replace(aPathname, LIB_DIR_KEY, libDir.getFullPath());
	}

	public WbFile getLibDir()
	{
		String dir = System.getProperty("workbench.libdir", getProperty("workbench.libdir", null));
		dir = FileDialogUtil.replaceConfigDir(dir);
		if (dir == null) return new WbFile(getConfigDir());
		return new WbFile(dir);
	}

	public String getShortcutFilename()
	{
		return new WbFile(getConfigDir(), "WbShortcuts.xml").getFullPath();
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

	public List<ToolDefinition> getExternalTools()
	{
		return getExternalTools(true, false);
	}

	public List<ToolDefinition> getAllExternalTools()
	{
		return getExternalTools(false, false);
	}

	public List<ToolDefinition> getExternalTools(boolean checkExists, boolean addPdfReader)
	{
		int numTools = getIntProperty("workbench.tools.count", 0);
		List<ToolDefinition> result = new ArrayList<ToolDefinition>(numTools);

		for (int i = 0; i < numTools; i++)
		{
			String path = getProperty("workbench.tools." + i + ".executable", "");
			String name = getProperty("workbench.tools." + i + ".name", path);

			ToolDefinition tool = new ToolDefinition(path, name);

			if (!checkExists)
			{
				 result.add(tool);
			}
			else if (tool.executableExists())
			{
				result.add(tool);
			}
		}

		return result;
	}

	private void clearTools()
	{
		int numTools = getIntProperty("workbench.tools.count", 0);
		for (int i=0; i < numTools; i++)
		{
			removeProperty("workbench.tools." + i + ".executable");
			removeProperty("workbench.tools." + i + ".name");
		}
	}

	public void setExternalTools(Collection<ToolDefinition> tools)
	{
		clearTools();
		int count = 0;
		for (ToolDefinition tool : tools)
		{
			setProperty("workbench.tools." + count + ".executable", tool.getCommandLine());
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
		return getIntProperty("workbench.gui.updatecheck.interval", 30);
	}

	public void setUpdateCheckInterval(int days)
	{
		setProperty("workbench.gui.updatecheck.interval", days);
	}
	// </editor-fold>

	public boolean isPropertyDefined(String key)
	{
		return props.containsKey(key);
	}

	public boolean getLogAllStatements()
	{
		return getBoolProperty(PROPERTY_LOG_ALL_SQL, false);
	}

	public void setLogAllStatements(boolean flag)
	{
		setProperty(PROPERTY_LOG_ALL_SQL, flag);
	}

	/**
	 * Controls if the initialization of connections in the main window is logged
	 * for debugging purposes.
	 * @return
	 */
	public boolean getLogConnectionDetails()
	{
		return getBoolProperty("workbench.connection.debug", false);
	}

	public boolean getFixSqlServerTimestampDisplay()
	{
		return getBoolProperty("workbench.db.microsoft_sql_server.fix.timestamp", true);
	}

	public boolean getConvertOracleTypes()
	{
		return getBoolProperty("workbench.db.oracle.types.autoconvert", true);
	}

	public void setConvertOracleTypes(boolean flag)
	{
		setProperty("workbench.db.oracle.types.autoconvert", flag);
	}

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

	public boolean getShowConnectDialogOnStartup()
	{
		return getBoolProperty("workbench.gui.autoconnect", true);
	}

	public void setShowConnectDialogOnStartup(boolean flag)
	{
		setProperty("workbench.gui.autoconnect", flag);
	}

	// <editor-fold defaultstate="collapsed" desc="Formatting options">

	public int getFormatterMaxColumnsInUpdate()
	{
		return getIntProperty("workbench.sql.formatter.update.columnsperline", 1);
	}

	public void setFormatterMaxColumnsInUpdate(int num)
	{
		setProperty("workbench.sql.formatter.update.columnsperline", num);
	}

	public int getFormatterMaxColumnsInInsert()
	{
		return getIntProperty("workbench.sql.formatter.insert.columnsperline", 1);
	}

	public void setFormatterMaxColumnsInInsert(int num)
	{
		setProperty("workbench.sql.formatter.insert.columnsperline", num);
	}

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

	public boolean getFormatterUpperCaseKeywords()
	{
		return getBoolProperty("workbench.sql.formatter.keywords.uppercase", true);
	}

	public void setFormatterUpperCaseKeywords(boolean flag)
	{
		setProperty("workbench.sql.formatter.keywords.uppercase", flag);
	}

	public boolean getFormatterAddSpaceAfterComma()
	{
		return getBoolProperty("workbench.sql.formatter.comma.spaceafter", false);
	}

	public void setFormatterAddSpaceAfterComma(boolean flag)
	{
		setProperty("workbench.sql.formatter.comma.spaceafter", flag);
	}

	public boolean getFormatterSetCommaAfterLineBreak()
	{
	    return getBoolProperty("workbench.sql.formatter.comma.afterLineBreak", false);
	}

	public void setFormatterSetCommaAfterLineBreak(boolean flag)
	{
	    setProperty("workbench.sql.formatter.comma.afterLineBreak", flag);
	}

	public boolean getFormatterAddSpaceAfterLineBreakComma()
	{
	    return getBoolProperty("workbench.sql.formatter.comma.spaceAfterLineBreakComma", false);
	}

	public void setFormatterAddSpaceAfterLineBreakComma(boolean flag)
	{
	    setProperty("workbench.sql.formatter.comma.spaceAfterLineBreakComma", flag);
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

	public boolean getAlwaysUseSeparateConnForDbExpWindow()
	{
		return getBoolProperty("workbench.dbexplorer.connection.always.separate", false);
	}

	public String getDefaultExplorerObjectType()
	{
		return getProperty("workbench.gui.dbobjects.TableListPanel.objecttype", null);
	}

	public void setDefaultExplorerObjectType(String type)
	{
		setProperty("workbench.gui.dbobjects.TableListPanel.objecttype", type);
	}

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

	public void setRememberColumnOrder(boolean flag)
	{
		setProperty("workbench.dbexplorer.remember.columnorder", flag);
	}

	public boolean getRememberColumnOrder()
	{
		return getBoolProperty("workbench.dbexplorer.remember.columnorder", false);
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
		if (properties.length > 0)
		{
			this.props.addPropertyChangeListener(l, properties);
		}
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
		{
			value = "BOLD";
		}

		if ((style & Font.ITALIC) == Font.ITALIC)
		{
			if (value == null) value = "ITALIC";
			else value += ",ITALIC";
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
		return getEditorFont(true);
	}

	public Font getEditorFont(boolean returnDefault)
	{
		Font f = this.getFont(PROPERTY_EDITOR_FONT);
		if (f == null && returnDefault)
		{
			f = new Font("Monospaced", Font.PLAIN, 12);
		}
		return f;
	}

	public void setMsgLogFont(Font f)
	{
		this.setFont(PROPERTY_MSGLOG_FONT, f);
	}

	public Font getMsgLogFont()
	{
		return this.getFont(PROPERTY_MSGLOG_FONT);
	}

	public void setDataFont(Font f)
	{
		this.setFont(PROPERTY_DATA_FONT, f);
	}

	public Font getDataFont()
	{
		return getDataFont(false);
	}

	public Font getDataFont(boolean returnDefault)
	{
		Font f = this.getFont(PROPERTY_DATA_FONT);
		if (f != null && returnDefault)
		{
			f = UIManager.getFont("Table.font");
		}
		return f;
	}

	public Font getPrinterFont()
	{
		Font f  = this.getFont(PROPERTY_PRINTER_FONT);
		if (f == null)
		{
			f = this.getDataFont();
		}
		return f;
	}

	 public Font getStandardFont()
	{
		return this.getFont(PROPERTY_STANDARD_FONT);
	}

	public void setStandardFont(Font f)
	{
		this.setFont(PROPERTY_STANDARD_FONT, f);
	}

	/**
	 *	Returns the font configured for this keyword
	 */
	public Font getFont(String aFontName)
	{
		Font result = null;

		String baseKey = "workbench.font." + aFontName;
		String name = this.props.getProperty(baseKey + ".name", null);

		if (name == null) return null;

		String sizeS = this.props.getProperty(baseKey + ".size", "10");
		String type = this.props.getProperty(baseKey + ".style", "Plain");
		int style = Font.PLAIN;
		int size = 12;
		StringTokenizer tok = new StringTokenizer(type);
		while (tok.hasMoreTokens())
		{
			String t = tok.nextToken();
			if ("bold".equalsIgnoreCase(t)) style |= Font.BOLD;
			if ("italic".equalsIgnoreCase(type)) style |= Font.ITALIC;
		}

		try
		{
			size = Integer.parseInt(sizeS);
		}
		catch (NumberFormatException e)
		{
			size = 10;
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

	public List<String> getLiteralTypeList()
	{
		List<String> result = getListProperty("workbench.sql.literals.types", false, "jdbc,ansi,dbms,default");
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

	public boolean getDefaultExportInfoSheet(String type)
	{
		if (type == null) return false;
		return getBoolProperty("workbench.export." + type.trim().toLowerCase() + ".default.infosheet", false);
	}

	public String getDefaultDiffDateLiteralType()
	{
		return getProperty("workbench.diff.sql.default.dateliterals", "jdbc");
	}

	public void setDefaultDiffDateLiteralType(String type)
	{
		setProperty("workbench.diff.sql.default.dateliterals", type);
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
		PageFormat page = new PageFormat();
		double iw = getDoubleProperty("workbench.print.paper.iwidth", 538.5826771653543);
		double ih = getDoubleProperty("workbench.print.paper.iheight", 785.1968503937007);
		double top = getDoubleProperty("workbench.print.paper.top", 28.346456692913385);
		double left = getDoubleProperty("workbench.print.paper.left", 28.346456692913385);
		double width = getDoubleProperty("workbench.print.paper.width", 595.275590551181);
		double height = getDoubleProperty("workbench.print.paper.height", 841.8897637795276);

		Paper paper = new Paper();
		paper.setSize(width, height);
		paper.setImageableArea(left, top, iw, ih);
		page.setPaper(paper);
		page.setOrientation(this.getPrintOrientation());
		return page;
	}

	private double getDoubleProperty(String prop, double defValue)
	{
		String v = getProperty(prop, null);
		return StringUtil.getDoubleValue(v, defValue);
	}

	public boolean getShowNativePageDialog()
	{
		return getBoolProperty("workbench.print.nativepagedialog", true);
	}

	private int getPrintOrientation()
	{
		return getIntProperty("workbench.print.orientation", PageFormat.PORTRAIT);
	}

	public void setPageFormat(PageFormat aFormat)
	{
		double width = aFormat.getWidth();
		double height = aFormat.getHeight();

		double iw = aFormat.getImageableWidth();
		double ih = aFormat.getImageableHeight();

		double left = aFormat.getImageableX();
		double top = aFormat.getImageableY();

		this.props.setProperty("workbench.print.paper.iwidth", Double.toString(iw));
		this.props.setProperty("workbench.print.paper.iheight", Double.toString(ih));
		this.props.setProperty("workbench.print.paper.top", Double.toString(top));
		this.props.setProperty("workbench.print.paper.left", Double.toString(left));
		this.props.setProperty("workbench.print.paper.width", Double.toString(width));
		this.props.setProperty("workbench.print.paper.height", Double.toString(height));
		this.props.setProperty("workbench.print.orientation", Integer.toString(aFormat.getOrientation()));
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

	public Color getEditorBackgroundColor()
	{
		Color std = UIManager.getColor("TextArea.background");
		if (std == null) std = Color.WHITE;
		return getColor(PROPERTY_EDITOR_BG_COLOR, std);
	}

	public void setEditorBackgroundColor(Color c)
	{
		setColor(PROPERTY_EDITOR_BG_COLOR, c);
	}

	public Color getEditorDatatypeColor()
	{
		Color std = new Color(0x990033);
		if (std == null) std = Color.BLACK;
		return getColor(PROPERTY_EDITOR_DATATYPE_COLOR, std);
	}

	public void setEditorDatatypeColor(Color c)
	{
		setColor(PROPERTY_EDITOR_DATATYPE_COLOR, c);
	}

	public Color getEditorTextColor()
	{
		Color std = UIManager.getColor("TextArea.foreground");
		if (std == null) std = Color.BLACK;
		return getColor(PROPERTY_EDITOR_FG_COLOR, std);
	}

	public void setEditorTextColor(Color c)
	{
		setColor(PROPERTY_EDITOR_FG_COLOR, c);
	}

	public void setEditorCursorColor(Color c)
	{
		setColor(PROPERTY_EDITOR_CURSOR_COLOR, c);
	}

	public Color getEditorCursorColor()
	{
		return getColor(PROPERTY_EDITOR_CURSOR_COLOR, Color.BLACK);
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

	public Color getEditorBracketHighlightColor()
	{
		return getColor(PROPERTY_EDITOR_BRACKET_HILITE_COLOR, null);
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

	public boolean getEditorUseTabCharacter()
	{
		return getBoolProperty("workbench.editor.usetab", true);
	}

	public void setEditorUseTabCharacter(boolean flag)
	{
		this.setProperty("workbench.editor.usetab", flag);
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

	public boolean getEmptyLineIsDelimiter()
	{
		return getBoolProperty("workbench.editor.sql.emptyline.delimiter", false);
	}

	public void setEmptyLineIsDelimiter(boolean flag)
	{
		setProperty("workbench.editor.sql.emptyline.delimiter", flag);
	}

	public boolean getAutoSaveWorkspace()
	{
		return getBoolProperty("workbench.workspace.autosave", false);
	}

	public void setAutoSaveWorkspace(boolean flag)
	{
		this.setProperty("workbench.workspace.autosave", flag);
	}

	public int getMaxWorkspaceBackup()
	{
		return getIntProperty("workbench.workspace.maxbackup", 5);
	}

	public void setMaxWorkspaceBackup(int max)
	{
		setProperty("workbench.workspace.maxbackup", max);
	}

	public void setCreateWorkspaceBackup(boolean flag)
	{
		setProperty("workbench.workspace.createbackup", flag);
	}

	public boolean getCreateWorkspaceBackup()
	{
		return getBoolProperty("workbench.workspace.createbackup", true);
	}

	public void setWorkspaceBackupDir(String dir)
	{
		setProperty("workbench.workspace.backup.dir", dir);
	}

	public String getWorkspaceBackupDir()
	{
		return getProperty("workbench.workspace.backup.dir", null);
	}


	public void setFilesInWorkspaceHandling(ExternalFileHandling handling)
	{
		setProperty("workbench.workspace.store.filenames", handling.toString());
	}

	public ExternalFileHandling getFilesInWorkspaceHandling()
	{
		String v = getProperty("workbench.workspace.store.filenames", ExternalFileHandling.link.toString());
		return ExternalFileHandling.getValue(v);
	}

	public String getFileVersionDelimiter()
	{
		return getProperty("workbench.file.version.delimiter", ".");
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Database">
	public boolean getUseGenericExecuteForSelect()
	{
		return getBoolProperty("workbench.db.select.genericexecute", false);
	}

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

	public List<String> getServersWithInlineConstraints()
	{
		String list = getProperty("workbench.db.inlineconstraints", "");
		return StringUtil.stringToList(list, ",");
	}

	/**
	 * Returns a list of DBIDs of servers that do not accept the NULL keyword
	 * in a column definition.
	 */
	public List<String> getServersWithNoNullKeywords()
	{
		String list = getProperty("workbench.db.nonullkeyword", "");
		return StringUtil.stringToList(list, ",");
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

	private String getSystemFileEncoding()
	{
		String def = System.getProperty("file.encoding");
		// Replace the strange Windows encoding with something more standard
		if ("Cp1252".equals(def)) def = "ISO-8859-15";
		return def;
	}

	public String getDefaultDataEncoding()
	{
		return getProperty("workbench.file.data.encoding", getSystemFileEncoding());
	}

	public String getDefaultEncoding()
	{
		return getProperty("workbench.encoding", getSystemFileEncoding());
	}

	public String getDefaultFileEncoding()
	{
		return getProperty("workbench.file.encoding", getSystemFileEncoding());
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
		setProperty(prop, delim);
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

	public File getDefaultXsltDirectory()
	{
		String dir = getProperty("workbench.xslt.dir", null);
		File result = null;
		if (dir == null)
		{
			result = new File(WbManager.getInstance().getJarPath(), "xslt");
		}
		else
		{
			result = new File(dir);
		}
		return result;
	}

	public String getDefaultXmlVersion()
	{
		return getProperty("workbench.xml.default.version", "1.0");
	}

	public boolean getDefaultWriteEmptyExports()
	{
		return getBoolProperty("workbench.export.default.writeempty", true);
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

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Date and Time Formatting">

	public void setDefaultDateFormat(String aFormat)
	{
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

	public int getMaxFractionDigits()
	{
		return getIntProperty("workbench.gui.display.maxfractiondigits", 2);
	}

	public void setMaxFractionDigits(int aValue)
	{
		this.props.setProperty("workbench.gui.display.maxfractiondigits", Integer.toString(aValue));
	}

	public DecimalFormat createDefaultDecimalFormatter()
	{
		DecimalFormat formatter = new DecimalFormat("0.#");
		String sep = this.getDecimalSymbol();
		int maxDigits = this.getMaxFractionDigits();
		DecimalFormatSymbols decSymbols = new DecimalFormatSymbols();
		decSymbols.setDecimalSeparator(sep.charAt(0));
		formatter.setDecimalFormatSymbols(decSymbols);
		formatter.setMaximumFractionDigits(maxDigits);
		return formatter;
	}

	public String getDecimalSymbol()
	{
		return getProperty("workbench.gui.display.decimal.separator", ".");
	}

	public void setDecimalSymbol(String aSep)
	{
		this.props.setProperty("workbench.gui.display.decimal.separator", aSep);
	}

	// </editor-fold>

	public int getSyncChunkSize()
	{
		return getIntProperty("workbench.sql.sync.chunksize", 25);
	}

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

	/**
	 * Return the maximum size of the log file (when using the built-in logging)
	 * If this size is exceeded a new log file is created
	 * <br/>
	 * The default max. size is 5MB
	 * @see workbench.log.SimpleLogger#setOutputFile(java.io.File, int)
	 */
	public int getMaxLogfileSize()
	{
		return this.getIntProperty("workbench.log.maxfilesize", 5 * 1024 * 1024);
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
	public boolean getAutoConnectObjectSearcher()
	{
		return getBoolProperty("workbench.objectsearcher.autoconnect", true);
	}

	public void setAutoConnectObjectSearcer(boolean flag)
	{
		this.setProperty("workbench.objectsearcher.autoconnect", flag);
	}

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

		// comparing with == is intended
		if (prof.getName() == BatchRunner.CMD_LINE_PROFILE_NAME) return;

		this.props.setProperty(key, prof.getName());
		this.props.setProperty(key + ".group", prof.getGroup());
	}
	// </editor-fold>

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

	public Locale getSortLocale()
	{
		if (!getBoolProperty("workbench.sort.usecollator", false)) return null;

		Locale l = null;
		String lang = Settings.getInstance().getSortLanguage();
		String country = Settings.getInstance().getSortCountry();
		try
		{
			if (lang != null && country != null)
			{
				l = new Locale(lang, country);
			}
			else if (lang != null && country == null)
			{
				l = new Locale(lang);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("Settings.getSortLocale()", "Error creating collation", e);
			l = Locale.getDefault();
		}
		return l;
	}

	public void setSortLocale(Locale l)
	{
		if (l == null)
		{
			setProperty("workbench.sort.usecollator", false);
		}
		else
		{
			setProperty("workbench.sort.usecollator", true);
			setProperty("workbench.sort.language", l.getLanguage());
			setProperty("workbench.sort.country", l.getCountry());
		}
	}

	private String getSortLanguage()
	{
		return getProperty("workbench.sort.language", System.getProperty("user.language"));
	}

	private String getSortCountry()
	{
		return getProperty("workbench.sort.country", System.getProperty("user.country"));
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

	@Override
	public boolean getBoolProperty(String property, boolean defaultValue)
	{
		String sysValue = System.getProperty(property, null);
		if (sysValue != null)
		{
			return Boolean.valueOf(sysValue);
		}
		return this.props.getBoolProperty(property, defaultValue);
	}

	@Override
	public void setProperty(String property, boolean value)
	{
		this.props.setProperty(property, value);
	}

	@Override
	public Object setProperty(String aProperty, String aValue)
	{
		return this.props.setProperty(aProperty, aValue);
	}

	@Override
	public void setProperty(String aProperty, int aValue)
	{
		this.props.setProperty(aProperty, Integer.toString(aValue));
	}

	@Override
	public String getProperty(String aProperty, String aDefault)
	{
		return System.getProperty(aProperty, this.props.getProperty(aProperty, aDefault));
	}

	public List<String> getListProperty(String aProperty, boolean makeLowerCase)
	{
		return getListProperty(aProperty, makeLowerCase, null);
	}

	public List<String> getListProperty(String aProperty, boolean makeLowerCase, String defaultList)
	{
		String list = System.getProperty(aProperty, this.props.getProperty(aProperty, defaultList));
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

	public void setColor(String key, Color c)
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
		int xPos = getIntProperty(getResolutionDependentKey(windowClass, "x"), Integer.MIN_VALUE);
		if (xPos == Integer.MIN_VALUE)
		{
			xPos = getIntProperty(windowClass + ".x", Integer.MIN_VALUE);
		}
		return xPos;
	}

	public int getWindowPosY(String windowClass)
	{
		int yPos = getIntProperty(getResolutionDependentKey(windowClass, "y"), Integer.MIN_VALUE);
		if (yPos == Integer.MIN_VALUE)
		{
			yPos = getIntProperty(windowClass + ".y", Integer.MIN_VALUE);
		}
		return yPos;
	}

	public int getWindowWidth(String windowClass)
	{
		int width = getIntProperty(getResolutionDependentKey(windowClass, "width"), Integer.MIN_VALUE);
		if (width == Integer.MIN_VALUE)
		{
			width = getIntProperty(windowClass + ".width", Integer.MIN_VALUE);
		}
		return width;
	}

	public int getWindowHeight(String windowClass)
	{
		int height = getIntProperty(getResolutionDependentKey(windowClass, "height"), Integer.MIN_VALUE);
		if (height == Integer.MIN_VALUE)
		{
			height = getIntProperty(windowClass + ".height", Integer.MIN_VALUE);
		}
		return height;
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

	private String getScreenResolutionKey()
	{
		try
		{
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			return Long.toString((long)screen.getWidth()) + "x" + Long.toString((long)screen.getHeight());
		}
		catch (Throwable th)
		{
			return "";
		}
	}

	private String getResolutionDependentKey(String base, String attribute)
	{
		String resKey = getScreenResolutionKey();
		if (resKey == null)
		{
			return base + "." + attribute;
		}
		return base + "." + resKey + "." + attribute;
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
		this.props.setProperty(getResolutionDependentKey(windowClass, "x"), Integer.toString(x));
		this.props.setProperty(getResolutionDependentKey(windowClass, "y"), Integer.toString(y));
	}

	public void setWindowSize(String windowClass, int width, int height)
	{
		this.props.setProperty(getResolutionDependentKey(windowClass, "width"), Integer.toString(width));
		this.props.setProperty(getResolutionDependentKey(windowClass, "height"), Integer.toString(height));
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
				@Override
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
		upgradeProp(def, "workbench.db.sql.comment.column", "COMMENT ON COLUMN %object_name%.%column% IS '%comment%';");

		upgradeProp(def, "workbench.db.oracle.add.column", "ALTER TABLE %table_name% ADD COLUMN %column_name% %datatype% %default_expression% %nullable%");

		upgradeListProp(def, "workbench.db.nonullkeyword");
	}

	private void upgradeProp(WbProperties defProps, String property, String originalvalue)
	{
		String currentValue = getProperty(property, "");

		// Make sure it has not been modified
		if (originalvalue.equals(currentValue))
		{
			String newvalue = defProps.getProperty(property, null);
			setProperty(property, newvalue);
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
		this.renameProperty("workbench.worspace.recent", "workbench.workspace.recent");
		this.renameProperty("workbench.sql.search.lastValue", "workbench.sql.search.lastvalue");
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
			s = s.replace(",postgres,",",postgresql,");
			this.setProperty("workbench.db.truncatesupported",s);
		}
		this.renameProperty("workbench.history.tablelist", "workbench.quickfilter.tablelist.history");
		this.renameProperty("workbench.history.columnlist", "workbench.quickfilter.columnlist.history");
		this.renameProperty("workbench.gui.dbobjects.ProcedureListPanel.lastsearch", "workbench.quickfilter.procedurelist.history");
		this.renameProperty("workbench.blob.text.encoding", "workbench.gui.blob.text.encoding");
		this.renameProperty("workbench.javacode.includenewline", "workbench.clipcreate.includenewline");
		this.renameProperty("workbench.javacode.codeprefix", "workbench.clipcreate.codeprefix");

		this.renameProperty("workbench.sql.replace.criteria", "workbench.sql.replace.criteria.lastvalue");
		this.renameProperty("workbench.sql.replace.replacement", "workbench.sql.replace.replacement.lastvalue");
		this.renameProperty("workbench.db.nullkeyword.ingres", "workbench.db.ingres.nullkeyword");
		this.renameProperty("workbench.db.defaultbeforenull.ingres", "workbench.db.ingres.defaultbeforenull");
		this.renameProperty("workbench.db.defaultbeforenull.firebird", "workbench.db.firebird.defaultbeforenull");
		this.renameProperty("workbench.db.defaultbeforenull.oracle", "workbench.db.oracle.defaultbeforenull");

		this.renameProperty("workbench.db.procversiondelimiter.microsoft_sql_server", "workbench.db.microsoft_sql_server.procversiondelimiter");
		this.renameProperty("workbench.db.procversiondelimiter.adaptive_server_enterprise", "workbench.db.adaptive_server_enterprise.procversiondelimiter");
		renameProperty("workbench.sql.searchsearch.history", "workbench.sql.search.history");
		renameProperty("workbench.sql.searchsearch.lastvalue", "workbench.sql.search.lastvalue");
		renameProperty("workbench.datasearch.history", "workbench.data.search.history");
		renameProperty("workbench.datasearch.lastvalue", "workbench.data.search.lastvalue");
		renameProperty("workbench.editor.autocompletion.sql.emptylineseparator", "workbench.editor.sql.emptyline.delimiter");
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
			this.props.remove("workbench.db.cancelwithreconnect");
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
			this.props.remove("workbench.db.retrievepklist");
			this.props.remove("workbench.print.margin.bottom");
			this.props.remove("workbench.print.margin.left");
			this.props.remove("workbench.print.margin.right");
			this.props.remove("workbench.print.margin.top");
			this.props.remove("workbench.dbexplorer.defTableType");
			this.props.remove("workbench.dbexplorer.deftabletype");

			this.props.remove("workbench.db.mysql.dropindex.needstable");
			this.props.remove("workbench.db.hxtt_dbf.dropindex.needstable");
			this.props.remove("workbench.ignoretypes.postgresql");
			this.props.remove("workbench.ignoretypes.mysql");
			props.remove("workbench.db.syntax.functions");
			props.remove("workbench.warn.java5");
			props.remove("workbench.db.microsoft_sql_server.drop.index.ddl");
			props.remove("workbench.db.mysql.drop.index.ddl");
			props.remove("workbench.db.h2.drop.column.single");
			props.remove("workbench.db.hsql_database_engine.drop.column.single");
			props.remove("workbench.db.oracle.drop.column.single");
			props.remove("workbench.db.postgresql.drop.column.single");
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
			FileUtil.closeQuietely(in);
		}
		return defProps;
	}

	private void fillDefaults()
	{
		InputStream in = ResourceMgr.getDefaultSettings();
		try
		{
			this.props.loadFromStream(in);
		}
		catch (IOException e)
		{
			LogMgr.logError(this, "Could not read default settings", e);
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}
	}
	// </editor-fold>

	public boolean isTestMode()
	{
		return getBoolProperty(TEST_MODE_PROPERTY, false);
	}

	public boolean wasExternallyModified()
	{
		long time = this.configfile.lastModified();
		return time > this.fileTime;
	}

	public WbFile getConfigFile()
	{
		return this.configfile;
	}

	public void saveSettings(boolean makeBackup)
	{
		if (this.props == null) return;

		// Never save settings in test mode
		if (isTestMode()) return;

		for (SettingsListener l : saveListener)
		{
			if (l != null) l.beforeSettingsSave();
		}

		ShortcutManager.getInstance().saveSettings();

		if (makeBackup)
		{
			this.configfile.makeBackup();
		}

		try
		{
			WbProperties defaults = getDefaultProperties();
			this.props.saveToFile(this.configfile, defaults);
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

	@Override
	public String toString()
	{
		return "[Settings]";
	}

}
