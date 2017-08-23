/*
 * Settings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.resource;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import workbench.WbManager;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.PropertyStorage;
import workbench.log.LogMgr;

import workbench.db.ConnectionProfile;
import workbench.db.IniProfileStorage;
import workbench.db.PasswordTrimType;
import workbench.db.WbConnection;
import workbench.db.XmlProfileStorage;

import workbench.gui.WbSwingUtilities;
import workbench.gui.lnf.FontScaler;
import workbench.gui.profiles.ProfileKey;
import workbench.gui.settings.ExternalFileHandling;

import workbench.storage.PkMapping;

import workbench.sql.DelimiterDefinition;
import workbench.sql.ErrorReportLevel;
import workbench.sql.formatter.JoinWrapStyle;

import workbench.util.CollectionUtil;
import workbench.util.DurationFormat;
import workbench.util.FileAttributeChanger;
import workbench.util.FileDialogUtil;
import workbench.util.FileUtil;
import workbench.util.FileVersioner;
import workbench.util.PlatformHelper;
import workbench.util.StringUtil;
import workbench.util.ToolDefinition;
import workbench.util.WbFile;
import workbench.util.WbLocale;
import workbench.util.WbNumberFormatter;
import workbench.util.WbProperties;


/**
 * The singleton to manage configuration settings for SQL Workbench/J
 *
 * Configuration properties are read from the file workbench.settings
 *
 * Defaults are applied by first read default.properties and then reading the user configuration
 * stored in workbench.settings
 *
 * @author Thomas Kellerer
 */
public class Settings
	implements PropertyStorage
{
	// <editor-fold defaultstate="collapsed" desc="Property Keys">
	public static final String PROPERTY_ENCRYPT_PWD = "workbench.profiles.encryptpassword";
	public static final String PROPERTY_TRIM_PWD = "workbench.profiles.trimpassword";
	public static final String PROPERTY_DATE_FORMAT = "workbench.gui.display.dateformat";
	public static final String PROPERTY_DATETIME_FORMAT = "workbench.gui.display.datetimeformat";
	public static final String PROPERTY_VARIABLE_LENGTH_TS_FRACTION = "workbench.gui.display.datetimeformat.fractions.variable";
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

	/**
	 * The property that identifies the name of the file containing the connection profiles.
	 */
	public static final String PROPERTY_DEFAULT_MACRO_STORAGE = "workbench.settings.macrostorage";

	public static final String PROPERTY_EDITOR_TAB_WIDTH = "workbench.editor.tabwidth";

	public static final String PROPERTY_EDITOR_CURRENT_LINE_COLOR = "workbench.editor.currentline.color";
	public static final String PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_BASE = "workbench.editor.occurance.highlight";
	public static final String PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT = PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_BASE + ".enable";
	public static final String PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_COLOR = PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_BASE + ".color";
	public static final String PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_MINLEN = PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_BASE + ".minlength";
	public static final String PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_IGNORE_CASE = PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_BASE + ".casesensitive";
	public static final String PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_NO_WHITESPACE = PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_BASE + ".nowhitespace";
	public static final String PROPERTY_EDITOR_BRACKET_HILITE_BASE = "workbench.editor.bracket.hilite";
	public static final String PROPERTY_EDITOR_BRACKET_HILITE_COLOR = PROPERTY_EDITOR_BRACKET_HILITE_BASE + ".color";
	public static final String PROPERTY_EDITOR_BRACKET_HILITE_LEFT = PROPERTY_EDITOR_BRACKET_HILITE_BASE + ".left";
	public static final String PROPERTY_EDITOR_BRACKET_HILITE_REC = PROPERTY_EDITOR_BRACKET_HILITE_BASE + ".rectangle";
	public static final String PROPERTY_EDITOR_BRACKET_HILITE_BOTH = PROPERTY_EDITOR_BRACKET_HILITE_BASE + ".both";
	public static final String PROPERTY_EDITOR_BRACKET_HILITE = PROPERTY_EDITOR_BRACKET_HILITE_BASE + ".enable";
	public static final String PROPERTY_EDITOR_ELECTRIC_SCROLL = "workbench.editor.electricscroll";
	public static final String PROPERTY_EDITOR_BG_COLOR = "workbench.editor.color.background";
	public static final String PROPERTY_EDITOR_FG_COLOR = "workbench.editor.color.foreground";
	public static final String PROPERTY_EDITOR_CURSOR_COLOR = "workbench.editor.color.cursor";
	public static final String PROPERTY_EDITOR_SELECTION_COLOR = "workbench.editor.color.selection";
	public static final String PROPERTY_EDITOR_DATATYPE_COLOR = "workbench.editor.color.datatype";

	public static final String PROPERTY_CONSOLIDATE_LOG_MESSAGES = "workbench.gui.log.consolidate";
	public static final String PROPERTY_LOG_ALL_SQL = "workbench.sql.log.statements";
	public static final String PROPERTY_LOG_OBFUSCATE = "workbench.sql.log.obfuscate";
	public static final String PROPERTY_SHOW_IGNORED_WARN = "workbench.sql.ignored.show.warning";
  public static final String PROP_LOG_VARIABLE_SUBSTITUTION = "workbench.sql.parameter.log.substitution";
  public static final String PROP_LOG_CLEAN_SQL = "workbench.sql.log.statements.clean";

  public static final String PROP_DURATION_FORMAT = "workbench.log.timing.format";
  public static final String PROP_DURATION_DECIMAL = "workbench.log.timing.decimal";

  /** The property that controls if the statement causing an error should be logged as well */
	public static final String PROPERTY_ERROR_STATEMENT_LOG_LEVEL = "workbench.gui.log.errorstatement";

	public static final String PROPERTY_VAR_CLEANUP = "workbench.sql.parameter.values.cleanup";
	public static final String PROPERTY_SORT_VARS = "workbench.sql.parameter.prompt.sort";
	public static final String PROPERTY_VAR_PREFIX = "workbench.sql.parameter.prefix";
	public static final String PROPERTY_VAR_SUFFIX = "workbench.sql.parameter.suffix";
	public static final String PROPERTY_DECIMAL_DIGITS = "workbench.gui.display.maxfractiondigits";
	public static final String PROPERTY_FIXED_DIGITS = "workbench.gui.display.decimal.digits.fixed";
	public static final String PROPERTY_DECIMAL_SEP = "workbench.gui.display.decimal.separator";
	public static final String PROPERTY_DECIMAL_GROUP = "workbench.gui.display.decimal.group";
	public static final String PROPERTY_DECIMAL_FORMAT = "workbench.gui.display.decimal.format";
	public static final String PROPERTY_INTEGER_FORMAT = "workbench.gui.display.integer.format";

	public static final String PROP_JOIN_COMPLETION_USE_PARENS = "workbench.gui.sql.join.completion.use.parenthesis";
	public static final String PROP_JOIN_COMPLETION_PREFER_USING = "workbench.gui.sql.join.completion.prefer.using";
	public static final String PROP_EDITOR_TRIM = "workbench.file.save.trim.trailing";

	public static final String PROP_LIBDIR = "workbench.libdir";

  public static final String PROP_LOGFILE_VIEWER = "workbench.logfile.viewer.program";

	// </editor-fold>

	public static final String TEST_MODE_PROPERTY = "workbench.gui.testmode";

	public static final String DEFAULT_MACRO_FILENAME = "WbMacros.xml";
	public static final String PK_MAPPING_FILENAME_PROPERTY = "workbench.pkmapping.file";
	public static final String UNIX_LINE_TERMINATOR_PROP_VALUE = "lf";
	public static final String DOS_LINE_TERMINATOR_PROP_VALUE = "crlf";
	public static final String DEFAULT_LINE_TERMINATOR_PROP_VALUE = "default";

	public static final String LIB_DIR_KEY = "%LibDir%";
	private static final String TOOLS_EXE = ".executable";
	private static final String TOOLS_NAME = ".name";
	private static final String TOOLS_PARAM = ".parameter";
	private static final String TOOLS_PREFIX = "workbench.tools.";

	private WbProperties props;
	private WbFile configfile;

	private final List<FontChangedListener> fontChangeListeners = new ArrayList<>(5);
	private final List<SettingsListener> saveListener = new ArrayList<>(5);

	private long fileTime;
	private boolean createBackup;

	/**
	 * Thread safe singleton-instance
	 */
	private static class LazyInstanceHolder
	{
		protected static final Settings instance = new Settings();
	}

	public static final Settings getInstance()
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

	public boolean isModified()
	{
		return props.isModified();
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
		// uses the Settings class, this class might be instantiated without a valid WbManager
		if (WbManager.getInstance() != null)
		{
			// Make the installation directory available as a system property as well.
			// this can e.g. be used to define the location of the logfile relative
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

			// These messages should not be logged before initLogging() has been called!
			LogMgr.logInfo("Settings.initialize()", "Using configdir: " + configfile.getParentFile().getAbsolutePath());
			LogMgr.logDebug("Settings.initialize()", "Using default xstlDir: " + getDefaultXsltDirectory());
			LogMgr.logDebug("Settings.initialize()", "Last modification time of loaded config file: " + this.fileTime);

			if (getBoolProperty("workbench.db.resetdefaults"))
			{
				LogMgr.logInfo("Settings.initialize()", "Resetting database properties to built-in defaults");
				resetDefaults();
			}
		}

		if (cfd.isHidden() && PlatformHelper.isWindows())
		{
      FileAttributeChanger changer = new FileAttributeChanger();
			// For some reason the settings are not properly saved under Windows 7 if the config directory is hidden
			LogMgr.logDebug("Settings.initialize()", "Removing hidden attribute of the configuration directory");
			changer.removeHidden(cfd);
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

		if (useLog4j && StringUtil.isNonBlank(log4jConfig))
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
		return useLog4j;
	}

	private void initLogging()
	{
		boolean useLog4j = initLog4j();

		LogMgr.init(useLog4j);

		boolean logSysErr = getBoolProperty("workbench.log.console", false);
		LogMgr.logToSystemError(logSysErr);

		String level = getProperty("workbench.log.level", "INFO");
		LogMgr.setLevel(level);

		String defaultFormat = "{timestamp} {type} {message} {error}";
		if (LogMgr.isDebugEnabled())
		{
			defaultFormat = "{timestamp} {type} {source} {message} {error}";
		}

		String format = getProperty("workbench.log.format", defaultFormat);
		LogMgr.setMessageFormat(format);

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
				if (!logfile.getParentFile().exists())
				{
					logfile.getParentFile().mkdirs();
				}
			}

			String configuredFile = null;
			if (!logfile.isWriteable())
			{
				configuredFile = logfile.getFullPath();
				logfile = new WbFile(getConfigDir(), "workbench.log");
				setProperty("workbench.log.filename", "workbench.log");
			}

			int maxSize = this.getMaxLogfileSize();
      int backups = getLogfileBackupCount();
			LogMgr.setOutputFile(logfile, maxSize, backups);

			if (configuredFile != null)
			{
				LogMgr.logWarning("Settings.initLogging()", "Could not write requested logfile '" + configuredFile + "'");
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

	public void setCreatBackupOnSave(boolean flag)
	{
		this.createBackup = flag;
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
		return StringUtil.replaceProperties((Map)(props), input);
	}

	public void setUseSinglePageHelp(boolean flag)
	{
		setProperty("workbench.help.singlepage", flag);
	}

	public boolean useSinglePageHelp()
	{
		return getBoolProperty("workbench.help.singlepage", false);
	}

  public boolean replaceEnvVarsInProfile()
  {
    return getBoolProperty("workbench.profiles.replace.env.vars", true);
  }

	public boolean usePgPassFile()
	{
		return getBoolProperty("workbench.db.postgresql.use.pgpass", true);
	}

	// <editor-fold defaultstate="collapsed" desc="Language settings">
	public void setLanguage(Locale locale)
	{
		setProperty("workbench.gui.language", locale.getLanguage());
	}

	public List<WbLocale> getLanguages()
	{
		List<String> codes = getListProperty("workbench.gui.languages.available", false, "en,de");
		List<WbLocale> result = new ArrayList<>(codes.size());
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

	public final File getConfigDir()
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
		String dir = getProperty(PROP_LIBDIR, null);
		dir = FileDialogUtil.replaceConfigDir(dir);
    dir = FileDialogUtil.replaceProgramDir(dir);
		if (dir == null) return new WbFile(getConfigDir());
		return new WbFile(dir);
	}

	public String getShortcutFilename()
	{
		return new WbFile(getConfigDir(), "WbShortcuts.xml").getFullPath();
	}

	public String getMacroStorage()
	{
		String macros = this.props.getProperty(PROPERTY_DEFAULT_MACRO_STORAGE);
		if (macros == null)
		{
			return new File(getConfigDir(), DEFAULT_MACRO_FILENAME).getAbsolutePath();
		}
		String realFilename = FileDialogUtil.replaceConfigDir(macros);

		WbFile f = new WbFile(realFilename);
		if (!f.isAbsolute())
		{
			// no directory in filename -> use config directory
			f = new WbFile(getConfigDir(), realFilename);
		}
		return f.getFullPath();
	}

	public void setMacroStorage(String file)
	{
		if (StringUtil.isEmptyString(file))
		{
			this.props.remove(PROPERTY_DEFAULT_MACRO_STORAGE);
		}
		else
		{
			this.props.setProperty(PROPERTY_DEFAULT_MACRO_STORAGE, file);
		}
	}

	public String getProfileStorage()
	{
		String profiles = this.props.getProperty(PROPERTY_PROFILE_STORAGE);
		if (profiles == null)
		{
      String xmlFile = XmlProfileStorage.DEFAULT_FILE_NAME;
      String iniFile = IniProfileStorage.DEFAULT_FILE_NAME;

      List<String> toSearch = new ArrayList<>();
      toSearch.add(xmlFile);
      toSearch.add(iniFile);

      for (String fname : toSearch)
  		{
        WbFile f = new WbFile(getConfigDir(), fname);
        if (f.exists()) return f.getFullPath();
      }

      // no file exists, use the default
      WbFile xml = new WbFile(getConfigDir(), xmlFile);
			return xml.getFullPath();
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

  public String getOpenLogFileTool()
  {
    return getProperty(PROP_LOGFILE_VIEWER, "internal");
  }

	public List<ToolDefinition> getExternalTools(boolean checkExists, boolean addPdfReader)
	{
		int numTools = getIntProperty("workbench.tools.count", 0);
		List<ToolDefinition> result = new ArrayList<>(numTools);

		for (int i = 0; i < numTools; i++)
		{
			String path = getProperty(TOOLS_PREFIX + i + TOOLS_EXE, "");
			String name = getProperty(TOOLS_PREFIX + i + TOOLS_NAME, path);
			String params = getProperty(TOOLS_PREFIX + i + TOOLS_PARAM, null);

			ToolDefinition tool = new ToolDefinition(path, params, name);

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
			removeProperty(TOOLS_PREFIX + i + TOOLS_EXE);
			removeProperty(TOOLS_PREFIX + i + TOOLS_NAME);
			removeProperty(TOOLS_PREFIX + i + TOOLS_PARAM);
		}
	}

	public void setExternalTools(Collection<ToolDefinition> tools)
	{
		clearTools();
		int count = 0;
		for (ToolDefinition tool : tools)
		{
			setProperty(TOOLS_PREFIX + count + TOOLS_EXE, tool.getExecutablePath());
			setProperty(TOOLS_PREFIX + count + TOOLS_NAME, tool.getName());
			setProperty(TOOLS_PREFIX + count + TOOLS_PARAM, tool.getParameters());
			count ++;
		}
		setProperty("workbench.tools.count", count);
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Update Check">
  public boolean checkJavaVersion()
  {
    return getBoolProperty("workbench.gui.check.java.version", true);
  }

	public Date getLastUpdateCheck()
	{
		String dt = getProperty("workbench.gui.updatecheck.lastcheck", null);
		return StringUtil.parseISODate(dt);
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

  public final DurationFormat getDurationFormat()
  {
    return getEnumProperty(PROP_DURATION_FORMAT, DurationFormat.dynamic);
  }

	public final boolean getObfuscateDbInformation()
  {
    return getBoolProperty(PROPERTY_LOG_OBFUSCATE, false);
  }

  public final boolean getLogAllStatements()
	{
		return getBoolProperty(PROPERTY_LOG_ALL_SQL, false);
	}

	public void setLogAllStatements(boolean flag)
	{
		setProperty(PROPERTY_LOG_ALL_SQL, flag);
	}

  public int getLowMemoryCheckInterval()
  {
    return getIntProperty("workbench.gui.data.memcheckinterval", 1000);
  }

	public boolean showSuccessMessageForVerb(String verb)
	{
		if (verb == null) return false;
		return getBoolProperty("workbench.sql." + verb.toLowerCase() + ".show.success", true);
	}

	public boolean getFixSqlServerTimestampDisplay()
	{
		return getBoolProperty("workbench.db.microsoft_sql_server.fix.timestamp", false);
	}

	public boolean useOracleNativeCancel()
	{
		return getBoolProperty("workbench.db.oracle.cancel.native", true);
	}

	public boolean fixOracleDateType()
	{
		return getBoolProperty("workbench.db.oracle.fixdatetype", false);
	}

	public void setFixOracleDateType(boolean flag)
	{
		setProperty("workbench.db.oracle.fixdatetype", flag);
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
		return getProperty("workbench.export.defaultencodings", "UTF-8,ISO-8859-1,ISO-8859-15");
	}

	public List<String> getEncodingsToUse()
	{
		return getListProperty("workbench.encodings", false, null);
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

	public GeneratedIdentifierCase getFormatterDatatypeCase()
	{
		return getIdentifierCase("workbench.sql.formatter.datatype.case", GeneratedIdentifierCase.upper);
  }

	public void setFormatterDatatypeCase(GeneratedIdentifierCase typeCase)
	{
		setIdentifierCase("workbench.sql.formatter.datatype.case", typeCase);
  }

	public GeneratedIdentifierCase getFormatterFunctionCase()
	{
		String value = getProperty("workbench.sql.formatter.functions.lowercase", null);
		if ("true".equals(value))
		{
			return GeneratedIdentifierCase.lower;
		}
		if ("false".equals(value))
		{
			return GeneratedIdentifierCase.lower;
		}
		return getIdentifierCase("workbench.sql.formatter.functions.case", GeneratedIdentifierCase.upper);
	}

	public void setFormatterFunctionCase(GeneratedIdentifierCase funcCase)
	{
		removeProperty("workbench.sql.formatter.functions.lowercase");
		setIdentifierCase("workbench.sql.formatter.functions.case", funcCase);
	}

	public boolean getFormatterSubselectInNewLine()
	{
		return getBoolProperty("workbench.sql.formatter.subselect.newline", false);
	}

	public void setFormatterSubselectInNewLine(boolean flag)
	{
		setProperty("workbench.sql.formatter.subselect.newline", flag);
	}

	public void setFormatterKeywordsCase(GeneratedIdentifierCase idCase)
	{
		removeProperty("workbench.sql.formatter.keywords.uppercase");
		setIdentifierCase("workbench.sql.formatter.keywords.case", idCase);
	}

	public GeneratedIdentifierCase getFormatterKeywordsCase()
	{
		String value = getProperty("workbench.sql.formatter.keywords.uppercase", null);
		if ("true".equals(value))
		{
			return GeneratedIdentifierCase.upper;
		}
		return getIdentifierCase("workbench.sql.formatter.keywords.case", GeneratedIdentifierCase.upper);
	}

	public GeneratedIdentifierCase getFormatterIdentifierCase()
	{
		return getIdentifierCase("workbench.sql.formatter.identifier.case", GeneratedIdentifierCase.asIs);
	}

	public void setFormatterIdentifierCase(GeneratedIdentifierCase identifierCase)
	{
		setIdentifierCase("workbench.sql.formatter.identifier.case", identifierCase);
	}

	public boolean getFormatterIndentWhereConditions()
	{
		return getBoolProperty("workbench.sql.formatter.where.condition.indent", false);
	}

	public void setFormatterIndentWhereConditions(boolean flag)
	{
		setProperty("workbench.sql.formatter.where.condition.indent", flag);
	}

	public boolean getFormatterAddSpaceAfterComma()
	{
		return getBoolProperty("workbench.sql.formatter.comma.spaceafter", false);
	}

	public void setFormatterAddSpaceAfterComma(boolean flag)
	{
		setProperty("workbench.sql.formatter.comma.spaceafter", flag);
	}

	public boolean getFormatterIndentInsert()
	{
		return getBoolProperty("workbench.sql.formatter.insert.indent", true);
	}

	public void setFormatterIndentInsert(boolean flag)
	{
		setProperty("\"workbench.sql.formatter.insert.indent", flag);
	}

	public boolean getFormatterCommaAfterLineBreak()
	{
		return getBoolProperty("workbench.sql.formatter.comma.afterLineBreak", false);
	}

	public void setFormatterCommaAfterLineBreak(boolean flag)
	{
		setProperty("workbench.sql.formatter.comma.afterLineBreak", flag);
	}

	/**
	 * Return if the SQL Formatter should add the name of the corresponding column in the VALUES part of INSERT
	 * statements.
	 *
	 */
	public boolean getFormatterAddColumnNameComment()
	{
		return getBoolProperty("workbench.sql.formatter.insert.values.columnname", false);
	}

	public void setFormatterAddColumnNameComment(boolean flag)
	{
		setProperty("workbench.sql.formatter.insert.values.columnname", flag);
	}

	public void setFormatterJoinWrapStyle(JoinWrapStyle style)
	{
		setProperty("workbench.sql.formatter.join.condition.wrapstyle", style.name());
	}

	public JoinWrapStyle getFormatterJoinWrapStyle()
	{
    return getEnumProperty("workbench.sql.formatter.join.condition.wrapstyle", JoinWrapStyle.onlyMultiple);
	}

	public void setFormatterWrapMultipleJoinConditions(boolean flag)
	{
	    setProperty("workbench.sql.formatter.join.condition.multiple.wrap", flag);
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

	public boolean getGenerateInsertIgnoreIdentity()
	{
		return getBoolProperty("workbench.sql.generate.insert.ignoreidentity",true);
	}

	public void setGenerateInsertIgnoreIdentity(boolean flag)
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

	public boolean getDoFormatDeletes()
	{
		return getBoolProperty("workbench.sql.generate.delete.doformat",true);
	}

	public void setDoFormatDeletes(boolean flag)
	{
		setProperty("workbench.sql.generate.delete.doformat", flag);
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
			this.props.remove(baseKey + TOOLS_NAME);
			this.props.remove(baseKey + ".size");
			this.props.remove(baseKey + ".style");
			return;
		}

		String name = aFont.getFamily();
		String size = Integer.toString(aFont.getSize());
		int style = aFont.getStyle();
		this.props.setProperty(baseKey + TOOLS_NAME, name);
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

	public boolean getEditorDetectEncoding()
	{
		return getBoolProperty("workbench.editor.drop.detect.encoding", true);
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
		return getMonospacedFont(PROPERTY_EDITOR_FONT, returnDefault);
	}

	public void setMsgLogFont(Font f)
	{
		this.setFont(PROPERTY_MSGLOG_FONT, f);
	}

	public Font getMsgLogFont()
	{
		return getMonospacedFont(PROPERTY_MSGLOG_FONT, true);
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
		return getMonospacedFont(PROPERTY_DATA_FONT, returnDefault);
	}

	private Font getMonospacedFont(String property, boolean returnDefault)
	{
    boolean isDefault = false;
		Font f = this.getFont(property);
		if (f == null && returnDefault)
		{
			f = new Font("Monospaced", Font.PLAIN, 12);
      isDefault = true;
		}
		if (getScaleFonts() && isDefault)
		{
			FontScaler scaler = new FontScaler();
			f = scaler.scaleFont(f);
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
   * Return a user-configured scale factor for default fonts.
   *
   * @return -1 if nothing was configured (or an invalid value was used),
   *         the scale factor to be used otherwise
   *
   * @see #getScaleFonts()
   *
   */
  public float getScaleFactor()
  {
    String scaleFactor = Settings.getInstance().getProperty("workbench.gui.desktop.scalefonts.factor", null);
    if (scaleFactor == null) return -1;

    float factor = -1;
    try
    {
      factor = Float.parseFloat(scaleFactor);
    }
    catch (Throwable th)
    {
      LogMgr.logError("Setting.getScaleFactor()", "Invalid font scale factor specified: " + scaleFactor, th);
    }
    return factor;
  }

	/**
	 * Returns true if the fonts should be scaled based on the DPI settings
	 * of the system.
	 *
   * If the user configured a scale factor (getScaleFactor() > 0), this method will return true.
   *
   * @see #getScaleFactor()
	 */
	public boolean getScaleFonts()
	{
    // always scale if the user configured a scale factor
    if (getScaleFactor() > 0) return true;

		int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		// 120 DPI is the "125% scale" setting in Windows
		// anything bigger than that indicates we should also scale the fonts.
		// For Linux it seems Java always returns 96dpi regardless of the
		// the DPI factor defined in the system so this won't do any harm for a Linux system either
		boolean scaleDefault = (dpi >= 120);
		return getBoolProperty("workbench.gui.desktop.scalefonts", scaleDefault);
	}

	public void setScaleFonts(boolean flag)
	{
		setProperty("workbench.gui.desktop.scalefonts", flag);
	}

	/**
	 *	Returns the font configured for this keyword
	 */
	public Font getFont(String aFontName)
	{
		Font result = null;

		String baseKey = "workbench.font." + aFontName;
		String name = StringUtil.trimToNull(getProperty(baseKey + ".name", null));

    // nothing configured, use the Java defaults
		if (name == null) return null;

		String fontSize = StringUtil.trimToNull(getProperty(baseKey + ".size", "12"));
		String type = getProperty(baseKey + ".style", "Plain");
		int style = Font.PLAIN;
		StringTokenizer tok = new StringTokenizer(type);
		while (tok.hasMoreTokens())
		{
			String t = tok.nextToken();
			if ("bold".equalsIgnoreCase(t)) style |= Font.BOLD;
			if ("italic".equalsIgnoreCase(type)) style |= Font.ITALIC;
		}

    int size = 12;
		try
		{
			size = Integer.parseInt(fontSize);
		}
		catch (NumberFormatException e)
		{
			// ignore
		}
		result = new Font(name, style, size);
		return result;
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Date Formats">

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

	/**
	 *
	 * Return true if the query for WbExport should be run automatically.
	 *
	 * This method will return true if the statement following a WbExport should automatically
	 * be executed if WbExport was run using "Execute current"
	 *
	 * @see #getAutoRunVerbs()
	 */
	public boolean getAutoRunExportStatement()
	{
		return getBoolProperty("workbench.export.sql.autorun.executecurrent", true);
	}

	public void setAutoRunExportStatement(boolean flag)
	{
		setProperty("workbench.export.sql.autorun.executecurrent", flag);
	}

	/**
	 * Return a list of SQL verbs that should automatically be executed if they
	 * immediately follow a WbExport and the WbExport is run with "Execute current".
	 *
	 * @see #getAutoRunExportStatement()
	 */
	public Collection<String> getAutoRunVerbs()
	{
		List<String> list = getListProperty("workbench.export.sql.autorun.verbs", false, null);
		Set<String> verbs = CollectionUtil.caseInsensitiveSet();
		verbs.addAll(list);
		return verbs;
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Color Handling">
	public Color getColor(String aColorKey)
	{
		return getColor(aColorKey, null);
	}

	public Color getColor(String aColorKey, Color defaultColor)
	{
		String value = getProperty(aColorKey, null);
		if (value == null) return defaultColor;
    Color result = stringToColor(value);
    if (result == null) return defaultColor;
    return result;
	}

  public static Color stringToColor(String value)
  {
    if (value == null) return null;
    String[] colors = value.split(",");
    if (colors.length != 3) return null;
    try
    {
      int r = StringUtil.getIntValue(colors[0]);
      int g = StringUtil.getIntValue(colors[1]);
      int b = StringUtil.getIntValue(colors[2]);
      return new Color(r, g, b);
    }
    catch (Exception e)
    {
      return null;
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
	public final boolean getConsolidateLogMsg()
	{
		return getBoolProperty(PROPERTY_CONSOLIDATE_LOG_MESSAGES, false);
	}

	public void setConsolidateLogMsg(boolean aFlag)
	{
		this.setProperty(PROPERTY_CONSOLIDATE_LOG_MESSAGES, aFlag);
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

 	/**
	 *  Returns the key for rectangular selections in the editor
	 */
	public int getRectSelectionKey()
	{
    int modifier = getRectSelectionModifier();
		if (modifier == InputEvent.CTRL_MASK)
		{
      return KeyEvent.VK_CONTROL;
		}
		return KeyEvent.VK_ALT;
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

	public int getConsoleHistorySize()
	{
		return getIntProperty("workbench.console.sql.historysize", 50);
	}

	public int getMaxHistorySize()
	{
		return getIntProperty("workbench.sql.historysize", 15);
	}

	public void setMaxHistorySize(int aValue)
	{
		this.setProperty("workbench.sql.historysize", aValue);
	}

	public int getToolbarIconSize()
	{
		return getIntProperty("workbench.gui.iconsize", 16);
	}

	public void setToolbarIconSize(int aValue)
	{
		this.setProperty("workbench.gui.iconsize", aValue);
	}

	public boolean getScaleMenuIcons()
	{
		return getBoolProperty("workbench.gui.scale.menuicon", true);
	}

	public void setScaleMenuIcons(boolean flag)
	{
		this.setProperty("workbench.gui.scale.menuicon", flag);
	}

	public boolean getCacheIcons()
	{
		return getBoolProperty("workbench.gui.icon.cache.enabled", true);
	}

	public void setCacheIcons(boolean flag)
	{
		setProperty("workbench.gui.icon.cache.enabled", flag);
	}

	public final DelimiterDefinition getAlternateDelimiter(WbConnection con, DelimiterDefinition defaultDelim)
	{
		DelimiterDefinition delim = null;
		if (con != null && con.getProfile() != null)
		{
			delim = con.getProfile().getAlternateDelimiter();
		}
		if (delim == null && con != null)
		{
			String text = getDbDelimiter(con.getDbId());
			if (StringUtil.isNonBlank(text))
			{
				delim = new DelimiterDefinition(text);
			}
		}
		if (delim == null)
		{
			delim = getAlternateDelimiter(defaultDelim);
		}
		return delim;
	}

	public final DelimiterDefinition getAlternateDelimiter(DelimiterDefinition defaultDelim)
	{
		String delim = getProperty("workbench.sql.alternatedelimiter", null);
		if (StringUtil.isBlank(delim)) return defaultDelim;
		DelimiterDefinition def = new DelimiterDefinition(delim);
		if (def.isStandard()) return null;
		return def;
	}

	public void setAlternateDelimiter(String delimiter)
	{
		this.setProperty("workbench.sql.alternatedelimiter", delimiter);
	}

	public boolean getRightClickMovesCursor()
	{
		return this.getBoolProperty("workbench.editor.rightclickmovescursor", false);
	}

	public void setRightClickMovesCursor(boolean flag)
	{
		setProperty("workbench.editor.rightclickmovescursor", flag);
	}

	public boolean getShowIgnoredWarning()
	{
		return getBoolProperty(PROPERTY_SHOW_IGNORED_WARN, true);
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
		return getColor(PROPERTY_EDITOR_BG_COLOR, null);
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
		return getColor(PROPERTY_EDITOR_FG_COLOR, null);
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
		setColor(PROPERTY_EDITOR_SELECTION_COLOR, c);
	}

	public Color getEditorSelectionColor()
	{
		return getColor(PROPERTY_EDITOR_SELECTION_COLOR, null);
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

	public void setMinLengthForSelectionHighlight(int len)
	{
		setProperty(PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_MINLEN, len);
	}

	public int getMinLengthForSelectionHighlight()
	{
		return getIntProperty(PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_MINLEN, 2);
	}

	public boolean getSelectionHighlightIgnoreCase()
	{
		return getBoolProperty(PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_IGNORE_CASE, true);
	}

	public void setSelectionHighlightIgnoreCase(boolean flag)
	{
		setProperty(PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_IGNORE_CASE, flag);
	}

	public boolean getSelectionHighlightNoWhitespace()
	{
		return getBoolProperty(PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_NO_WHITESPACE, true);
	}

	public void setSelectionHighlightNoWhitespace(boolean flag)
	{
		setProperty(PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_NO_WHITESPACE, flag);
	}

	public void setHighlightCurrentSelection(boolean flag)
	{
		setProperty(PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT, flag);
	}

	public boolean getHighlightCurrentSelection()
	{
		return getBoolProperty(PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT, true);
	}

	public void setSelectionHighlightColor(Color color)
	{
		setColor(PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_COLOR, color);
	}

	public Color geSelectionHighlightColor()
	{
		return getColor(PROPERTY_EDITOR_OCCURANCE_HIGHLIGHT_COLOR, Color.YELLOW);
	}

	public boolean isBracketHighlightEnabled()
	{
		return getBoolProperty(PROPERTY_EDITOR_BRACKET_HILITE, true);
	}

	public void setBracketHighlight(boolean flag)
	{
		setProperty(PROPERTY_EDITOR_BRACKET_HILITE, flag);
	}

	/**
	 * Returns true if the matching bracket should be highlighted depening on the
	 * character left of the caret. If false the character to the right of
	 * the caret is taken as the "base" character
	 */
	public boolean getBracketHighlightLeft()
	{
		return getBoolProperty(PROPERTY_EDITOR_BRACKET_HILITE_LEFT, true);
	}

	public void setBracketHighlightLeft(boolean flag)
	{
		setProperty(PROPERTY_EDITOR_BRACKET_HILITE_LEFT, flag);
	}

	/**
	 * Returns true if the matching bracket be highlighted with a rectangle
	 */
	public boolean getBracketHighlightRectangle()
	{
		return getBoolProperty(PROPERTY_EDITOR_BRACKET_HILITE_REC, true);
	}

	public void setBracketHighlightRectangle(boolean flag)
	{
		setProperty(PROPERTY_EDITOR_BRACKET_HILITE_REC, flag);
	}

	/**
	 * Returns true if the matching bracket be highlighted with a rectangle
	 */
	public boolean getBracketHighlightBoth()
	{
		return getBoolProperty(PROPERTY_EDITOR_BRACKET_HILITE_BOTH, false);
	}

	public void setBracketHighlightBoth(boolean flag)
	{
		setProperty(PROPERTY_EDITOR_BRACKET_HILITE_BOTH, flag);
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
		return getBoolProperty("workbench.editor.usetab", false);
	}

	public void setEditorUseTabCharacter(boolean flag)
	{
		this.setProperty("workbench.editor.usetab", flag);
	}

	public String getEditorNoWordSep()
	{
		return getProperty("workbench.editor.nowordsep", "_$");
	}

	public void setEditorNoWordSep(String noSep)
	{
		setProperty("workbench.editor.nowordsep", noSep);
	}

	public boolean getJoinCompletionUseParens()
	{
		return getBoolProperty(PROP_JOIN_COMPLETION_USE_PARENS, false);
	}

	public void setJoinCompletionUseParens(boolean flag)
	{
		setProperty(PROP_JOIN_COMPLETION_USE_PARENS, flag);
	}

	public boolean getJoinCompletionPreferUSING()
	{
		return getBoolProperty(PROP_JOIN_COMPLETION_PREFER_USING, false);
	}

	public void setJoinCompletionPreferUSING(boolean flag)
	{
		setProperty(PROP_JOIN_COMPLETION_PREFER_USING, flag);
	}


  public boolean getUseProfileFilterForCompletion()
  {
    return getBoolProperty("workbench.editor.autocompletion.use.profilefilter", true);
  }

  public void setUseProfileFilterForCompletion(boolean flag)
  {
    setProperty("workbench.editor.autocompletion.use.profilefilter", flag);
  }

  public void setAutoCompletionPasteCase(GeneratedIdentifierCase value)
	{
		setIdentifierCase("workbench.editor.autocompletion.paste.case", value);
	}

	private void setIdentifierCase(String property, GeneratedIdentifierCase value)
	{
    setProperty(property, value.name());
	}

	public GeneratedIdentifierCase getAutoCompletionPasteCase()
	{
		return getIdentifierCase("workbench.editor.autocompletion.paste.case", GeneratedIdentifierCase.asIs);
	}

	private GeneratedIdentifierCase getIdentifierCase(String property, GeneratedIdentifierCase defaultValue)
	{
    return getEnumProperty(property, defaultValue);
	}

	public boolean getAutoCompletionUseCurrentNameSpace()
	{
		return getBoolProperty("workbench.editor.autocompletion.current.schema", true);
	}

	public void setAutoCompletionUseCurrentNameSpace(boolean flag)
	{
		setProperty("workbench.editor.autocompletion.current.schema", flag);
	}

	public ColumnSortType getAutoCompletionColumnSortType()
	{
		return getEnumProperty("workbench.editor.autocompletion.paste.sort", ColumnSortType.name);
	}

	public void setAutoCompletionColumnSort(ColumnSortType sort)
	{
		setProperty("workbench.editor.autocompletion.paste.sort", sort.name());
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

	public AutoFileSaveType getAutoSaveExternalFiles()
	{
    AutoFileSaveType defaultValue = AutoFileSaveType.never;
    if (getAutoSaveWorkspace())
    {
      defaultValue = AutoFileSaveType.always;
    }

    String type = getProperty("workbench.editor.autosave", defaultValue.name());

    try
    {
      return AutoFileSaveType.valueOf(type);
    }
    catch (Throwable ex)
    {
      return defaultValue;
    }
	}

	public void setAutoSaveExternalFiles(AutoFileSaveType type)
	{
    if (type == null) return;
		setProperty("workbench.editor.autosave", type.name());
	}

	public boolean getAutoSaveWorkspace()
	{
		return getBoolProperty("workbench.workspace.autosave", false);
	}

	public void setAutoSaveWorkspace(boolean flag)
	{
		this.setProperty("workbench.workspace.autosave", flag);
	}

	public int getMaxBackupFiles()
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

	public void setBackupDir(String dir)
	{
		setProperty("workbench.workspace.backup.dir", dir);
	}

	public String getBackupDir()
	{
		return getProperty("workbench.workspace.backup.dir", null);
	}

	public boolean getCreateProfileBackup()
	{
		return getBoolProperty("workbench.profiles.createbackup", true);
	}

	public void setCreateProfileBackup(boolean flag)
	{
		setProperty("workbench.profiles.createbackup", flag);
	}

	public boolean getCreateDriverBackup()
	{
		return getBoolProperty("workbench.drivers.createbackup", true);
	}

	public void setCreateDriverBackup(boolean flag)
	{
		setProperty("workbench.drivers.createbackup", flag);
	}

	public boolean getCreateMacroBackup()
	{
		return getBoolProperty("workbench.macros.createbackup", true);
	}

	public void setCreateMacroBackup(boolean flag)
	{
		setProperty("workbench.macro.createbackup", flag);
	}

	public boolean getCreateSettingsBackup()
	{
		return getBoolProperty("workbench.settings.createbackup", false);
	}

	public void setCreateSettingsBackup(boolean flag)
	{
		setProperty("workbench.settings.createbackup", flag);
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

  public boolean hideIgnoreWarnings()
  {
    return getBoolProperty("workbench.db.warnings.ignored.hide", true);
  }

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

  public boolean retrieveDbmsOutputAfterExec()
  {
    return getBoolProperty("workbench.db.oracle.dbmsoutput.automatic", true);
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

	public void setDebugMetadataSql(boolean flag)
	{
		setProperty("workbench.dbmetadata.debugmetasql", flag);
	}

	public boolean getCheckPreparedStatements()
	{
		return getBoolProperty("workbench.sql.checkprepared", false);
	}

	public void setCheckPreparedStatements(boolean flag)
	{
		this.setProperty("workbench.sql.checkprepared", flag);
	}

	public boolean getCheckEditableColumns()
	{
		return getBoolProperty("workbench.db.edit.verify.updateable", false);
	}

	public void setCheckEditableColumns(boolean flag)
	{
		setProperty("workbench.db.edit.verify.updateable", flag);
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

	public String getExportNullString()
	{
		return getProperty("workbench.export.general.nullstring", null);
	}

	public void setExportNullString(String value)
	{
		setProperty("workbench.export.general.nullstring", value);
	}

	public String getSystemFileEncoding()
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

	public boolean getTrimTrailingSpaces()
	{
		return getBoolProperty(PROP_EDITOR_TRIM, false);
	}

	public void setTrimTrailingSpaces(boolean flag)
	{
		this.props.setProperty(PROP_EDITOR_TRIM, flag);
	}

	public String getDefaultTextDelimiter(boolean readable)
	{
		return getDelimiter("workbench.export.text.fielddelimiter", "\\t", readable);
	}

  public String getCssForClipboardHtml(String defaultCss)
  {
    return getProperty("workbench.copy.clipboard.html.css", defaultCss);
  }

  public boolean copyToClipboardAsHtml()
  {
    return getBoolProperty("workbench.copy.clipboard.html.enabled", true);
  }

	public void setClipboardDelimiter(String delim)
	{
		setDelimiter("workbench.import.clipboard.fielddelimiter", delim);
	}

	public String getClipboardDelimiter(boolean readable)
	{
		return getDelimiter("workbench.import.clipboard.fielddelimiter", "\\t", readable);
  }

  public boolean getUseMultirowInsertForClipboard()
  {
    return getBoolProperty("workbench.copy.clipboard.insert.multirow", false);
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
	public String getLastImportDelimiter(boolean readable)
	{
		return getDelimiter("workbench.import.text.fielddelimiter", "\\t", readable);
	}

	public File getDefaultXsltDirectory()
	{
		// this can happen if the Settings instance is accessed by
		// a component that is instantiated in the NetBeans GUI editor
		if (WbManager.getInstance() == null) return new File(".");

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

	public boolean getStoreScriptDirInWksp()
	{
		return getBoolProperty("workbench.scriptdir.store.in.workspace", false);
	}

	public void setStoreScriptDirInWksp(boolean flag)
	{
		setProperty("workbench.scriptdir.store.in.workspace", flag);
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

  public boolean useVariableLengthTimeFractions()
  {
    return getBoolProperty(PROPERTY_VARIABLE_LENGTH_TS_FRACTION, false);
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

	public boolean getUsedFixedDigits()
	{
		return getBoolProperty(PROPERTY_FIXED_DIGITS, false);
	}

	public void setUsedFixedDigits(boolean flag)
	{
		setProperty(PROPERTY_FIXED_DIGITS, flag);
	}

	public void setDefaultExportTextDelimiter(String aDelimit)
	{
		if (aDelimit.equals("\t")) aDelimit = "\\t";

		this.props.setProperty("workbench.export.text.fielddelimiter", aDelimit);
	}

  public void setLastExportDecimalSeparator(String decimal)
  {
    setProperty("workbench.export.text.decimal.last", decimal);
  }

  public String getLastExportDecimalSeparator()
  {
    return getProperty("workbench.export.text.decimal.last", getDecimalSymbol());
  }

	public int getLastExportMaxFractionDigits()
	{
		return getIntProperty("workbench.export.text.digits.last", getMaxFractionDigits());
	}

  public void setLastExportMaxFractionDigits(int digits)
	{
		setProperty("workbench.export.text.digits.last", digits);
	}

	public int getMaxFractionDigits()
	{
		return getIntProperty(PROPERTY_DECIMAL_DIGITS, 0);
	}

	public void setMaxFractionDigits(int aValue)
	{
		this.props.setProperty(PROPERTY_DECIMAL_DIGITS, Integer.toString(aValue));
	}

	public WbNumberFormatter createDefaultDecimalFormatter(int maxDigits)
	{
		String sep = this.getDecimalSymbol();
		return new WbNumberFormatter(maxDigits, sep.charAt(0));
	}

	public WbNumberFormatter createDefaultDecimalFormatter()
	{
		String sep = this.getDecimalSymbol();
		int maxDigits = this.getMaxFractionDigits();
		return new WbNumberFormatter(maxDigits, sep.charAt(0));
	}

	public String getDecimalSymbol()
	{
		return getProperty(PROPERTY_DECIMAL_SEP, ".");
	}

	public void setDecimalSymbol(String aSep)
	{
		this.props.setProperty(PROPERTY_DECIMAL_SEP, aSep);
	}

	public String getDecimalGroupCharacter()
	{
    String val = getProperty(PROPERTY_DECIMAL_GROUP, ",");
    if (StringUtil.isEmptyString(val))
    {
      val = ",";
    }
		return val;
	}

	public void setDecimalGroupCharacter(String aSep)
	{
		this.props.setProperty(PROPERTY_DECIMAL_GROUP, aSep);
	}

	public void setDecimalFormatString(String format)
	{
		this.props.setProperty(PROPERTY_DECIMAL_FORMAT, format);
	}

	public String getDecimalFormatString()
	{
		return getProperty(PROPERTY_DECIMAL_FORMAT, null);
	}


	public void setIntegerFormatString(String format)
	{
		this.props.setProperty(PROPERTY_INTEGER_FORMAT, format);
	}

	public String getIntegerFormatString()
	{
		return getProperty(PROPERTY_INTEGER_FORMAT, null);
	}

	// </editor-fold>


	public int getMaxErrorStatementLength()
	{
		return getIntProperty("workbench.gui.log.error.sql.maxlength", 150);
	}

	public final ErrorReportLevel getStatementErrorReportLevel()
	{
    return getEnumProperty(PROPERTY_ERROR_STATEMENT_LOG_LEVEL, ErrorReportLevel.limited);
	}

	public boolean getSaveProfilesImmediately()
	{
		return getBoolProperty("workbench.profiles.saveonclose", false);
	}

	public void setSaveProfilesImmediately(boolean flag)
	{
		setProperty("workbench.profiles.saveonclose", flag);
	}

	public int getSyncChunkSize()
	{
		return getIntProperty("workbench.sql.sync.chunksize", 25);
	}

	public boolean getCleanupVariableValues()
	{
		return getBoolProperty(PROPERTY_VAR_CLEANUP, false);
	}

	public boolean getSortPromptVariables()
	{
		return getBoolProperty(PROPERTY_SORT_VARS, true);
	}

	public String getSqlParameterPrefix()
	{
		String value = getProperty(PROPERTY_VAR_PREFIX, "$[");
		if (StringUtil.isEmptyString(value)) value = "$[";
		return value;
	}

	public String getSqlParameterSuffix()
	{
		if (this.props.containsKey(PROPERTY_VAR_SUFFIX) || System.getProperties().containsKey(PROPERTY_VAR_SUFFIX))
		{
			// The built-in default suffix is stored in default.properties thus is can be "deleted"
			// by adding an empty property in workbench.settings
			// so if the key is present, use whatever is stored there.
			return getProperty(PROPERTY_VAR_SUFFIX, "");
		}
		// returning a value in case the key is not present, ensures that the default is applied in case the
		// property was completely remove e.g. by using WbSetConfig.
		return "]";
	}

  public final boolean getLogParameterSubstitution()
  {
    return getBoolProperty(PROP_LOG_VARIABLE_SUBSTITUTION, false);
  }

	/**
	 * Return the maximum size of the log file (when using the built-in logging)
	 * If this size is exceeded a new log file is created
	 * <br/>
	 * The default max. size is 2MB
	 * @see workbench.log.SimpleLogger#setOutputFile(java.io.File, int)
	 */
	public int getMaxLogfileSize()
	{
		return this.getIntProperty("workbench.log.maxfilesize", 10 * 1024 * 1024);
	}

	public int getLogfileBackupCount()
	{
		return this.getIntProperty("workbench.log.backup.count", 5);
	}

	public boolean getDelimiterDefaultSingleLine()
	{
		return getBoolProperty("workbench.delimiter.newline.default", true);
	}

	public boolean getIncludeDefaultContinue()
	{
		return getBoolProperty("workbench.include.continue.default", false);
  }

	public boolean useNonStandardQuoteEscaping(String dbId)
	{
    boolean value = getBoolProperty("workbench.sql.checkescapedquotes", false);
    if (dbId != null)
    {
      return getBoolProperty("workbench.db." + dbId + ".sql.checkescapedquotes", value);
    }
    return value;
	}

	public boolean useNonStandardQuoteEscaping(WbConnection conn)
	{
    return useNonStandardQuoteEscaping(conn == null ? null : conn.getDbId());
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

		// Do not remember profiles defined on the commandline
		if (prof.isTemporaryProfile()) return;

		this.props.setProperty(key, prof.getName());
		this.props.setProperty(key + ".group", prof.getGroup());
	}
	// </editor-fold>

	public void setGeneratedSqlTableCase(GeneratedIdentifierCase value)
	{
		setIdentifierCase("workbench.sql.generate.table.case", value);
	}

	public GeneratedIdentifierCase getGeneratedSqlTableCase()
	{
		return getIdentifierCase("workbench.sql.generate.table.case", GeneratedIdentifierCase.asIs);
	}

	public int getInMemoryScriptSizeThreshold()
	{
		// Process scripts up to 2 MB in memory
		// this is used by the ScriptParser
		return getIntProperty("workbench.sql.script.inmemory.maxsize", 2 * 1024 * 1024);
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

	public boolean isUTF8Language(Locale l)
	{
		return getBoolProperty("workbench.locale.use.utf8."+ l.getLanguage(), false);
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
		this.props.removeProperty(property);
	}

	public boolean getBoolProperty(String property)
	{
		return getBoolProperty(property, false);
	}

	@Override
	public final boolean getBoolProperty(String property, boolean defaultValue)
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

	public void setTemporaryProperty(String property, String newValue)
	{
		this.props.setTemporaryProperty(property, newValue);
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
	public String getProperty(String property, String aDefault)
	{
		return System.getProperty(property, this.props.getProperty(property, aDefault));
	}

  public final <E extends Enum<E>> E getEnumProperty(String key, E defaultValue)
  {
    String value = getProperty(key, null);
    return getEnumValue(value, defaultValue);
  }

  public final <E extends Enum<E>> E getEnumValue(String value, E defaultValue)
  {
    if (value != null)
    {
      try
      {
       return (E)Enum.valueOf(defaultValue.getClass(), value);
      }
      catch (Throwable e)
      {
        LogMgr.logWarning("Settings.getEnumProperty()", "Invalid enum value '" + value + "' specified");
      }
    }
    return defaultValue;
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

	@Override
	public final int getIntProperty(String aProperty, int defaultValue)
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
		this.setProperty(key, colorToString(c));
	}

  public static String colorToString(Color c)
  {
    String value = null;
    if (c != null)
    {
      int r = c.getRed();
      int g = c.getGreen();
      int b = c.getBlue();
      value = Integer.toString(r) + "," + Integer.toString(g) + "," + Integer.toString(b);
    }
    return value;
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
		if (StringUtil.isEmptyString(id)) return false;

		boolean result = false;
		final int w = this.getWindowWidth(id);
		final int h = this.getWindowHeight(id);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		if (w > 0 && h > 0 && w <= screen.getWidth() && h <= screen.getHeight())
		{
			result = true;
			WbSwingUtilities.invoke(() ->
      {
        target.setSize(new Dimension(w, h));
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
		if (target == null) return false;

		final int x = this.getWindowPosX(id);
		final int y = this.getWindowPosY(id);

		// nothing stored, nothing to do
		if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE) return false;

    Rectangle screen = WbSwingUtilities.getVirtualBounds();
    Rectangle toDisplay = new Rectangle(x, y, target.getWidth(), target.getHeight());
		if (WbSwingUtilities.isOutsideOfScreen(toDisplay))
		{
			LogMgr.logInfo("Settings.restoreWindowPosition()", "Stored window position " + WbSwingUtilities.displayString(toDisplay) + " not restored because it is outside the current screen size: " + WbSwingUtilities.displayString(screen));
			return false;
		}

		LogMgr.logDebug("Settings.restoreWindowPosition()", "Restoring window position for '" + id + "', " +
			"current screen size: " + WbSwingUtilities.displayString(screen)  + ", requested position: " + WbSwingUtilities.displayString(toDisplay));

		WbSwingUtilities.invoke(() ->
    {
      target.setLocation(new Point(x, y));
    });

		return true;
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
		Set<String> currentProps = new HashSet<>();
		currentProps.addAll(currentList);

		String defValue = defProps.getProperty(key, "");
		List<String> defList = StringUtil.stringToList(defValue, ",", true, true, false);
		currentProps.addAll(defList);
		this.setProperty(key, StringUtil.listToString(currentProps,','));
	}

	public void replacePartialKey(String oldKey, String newKey)
	{
		Map<String, String> toChange = new HashMap<>();
		for (Object keyObj : props.keySet())
		{
			String key = keyObj.toString();
			if (key.contains(oldKey))
			{
				toChange.put(key, key.replace(oldKey, newKey));
			}
		}
		for (Map.Entry<String, String> entry : toChange.entrySet())
		{
			LogMgr.logDebug("Settings.replacePartialKey", "Renaming: " + entry.getKey() + " to " + entry.getValue());
			renameProperty(entry.getKey(), entry.getValue());
		}
	}

	private void renameOldProps()
	{
		renameProperty("workbench.worspace.recent", "workbench.workspace.recent");
		renameProperty("workbench.sql.search.lastValue", "workbench.sql.search.lastvalue");
		renameProperty("workbench.sql.maxcolwidth","workbench.gui.optimalwidth.maxsize");
		renameProperty("workbench.sql.mincolwidth","workbench.gui.optimalwidth.minsize");
		renameProperty("sort.language", "workbench.sort.language");
		renameProperty("sort.country", "workbench.sort.country");
		renameProperty("connection.last", "workbench.connection.last");
		renameProperty("drivers.lastlibdir", "workbench.drivers.lastlibdir");
		renameProperty("workbench.db.debugger", "workbench.db.previewsql");

		// Fix typos from incorrect default.properties
		renameProperty("workbench.db.objecttype.data.postgres", "workbench.db.objecttype.data.postgresql");
		renameProperty("workbench.db.objecttype.selectable.postgres", "workbench.db.objecttype.selectable.postgresql");
		renameProperty("workbench.ignoretypes.postgres", "workbench.ignoretypes.postgresql");
		String s = getProperty("workbench.db.truncatesupported",null);
		if (s!=null)
		{
			s = s.replace(",postgres,",",postgresql,");
			setProperty("workbench.db.truncatesupported",s);
		}
		renameProperty("workbench.history.tablelist", "workbench.quickfilter.tablelist.history");
		renameProperty("workbench.history.columnlist", "workbench.quickfilter.columnlist.history");
		renameProperty("workbench.gui.dbobjects.ProcedureListPanel.lastsearch", "workbench.quickfilter.procedurelist.history");
		renameProperty("workbench.blob.text.encoding", "workbench.gui.blob.text.encoding");
		renameProperty("workbench.javacode.includenewline", "workbench.clipcreate.includenewline");
		renameProperty("workbench.javacode.codeprefix", "workbench.clipcreate.codeprefix");

		renameProperty("workbench.sql.replace.criteria", "workbench.sql.replace.criteria.lastvalue");
		renameProperty("workbench.sql.replace.replacement", "workbench.sql.replace.replacement.lastvalue");
		renameProperty("workbench.db.nullkeyword.ingres", "workbench.db.ingres.nullkeyword");
		renameProperty("workbench.db.defaultbeforenull.ingres", "workbench.db.ingres.defaultbeforenull");
		renameProperty("workbench.db.defaultbeforenull.firebird", "workbench.db.firebird.defaultbeforenull");
		renameProperty("workbench.db.defaultbeforenull.oracle", "workbench.db.oracle.defaultbeforenull");

		renameProperty("workbench.db.procversiondelimiter.microsoft_sql_server", "workbench.db.microsoft_sql_server.procversiondelimiter");
		renameProperty("workbench.db.procversiondelimiter.adaptive_server_enterprise", "workbench.db.adaptive_server_enterprise.procversiondelimiter");
		renameProperty("workbench.sql.searchsearch.history", "workbench.sql.search.history");
		renameProperty("workbench.sql.searchsearch.lastvalue", "workbench.sql.search.lastvalue");
		renameProperty("workbench.datasearch.history", "workbench.data.search.history");
		renameProperty("workbench.datasearch.lastvalue", "workbench.data.search.lastvalue");
		renameProperty("workbench.editor.autocompletion.sql.emptylineseparator", "workbench.editor.sql.emptyline.delimiter");
		removeProperty("workbench.gui.animatedicon.name");
		removeProperty("workbench.gui.animatedicon");
		String value = props.getProperty("workbench.db.postgresql..inlineconstraints", null);
		if ("true".equals(value))
		{
			removeProperty("workbench.db.postgresql..inlineconstraints");
			props.setProperty("workbench.db.postgresql.pk.inline", "true");
			props.setProperty("workbench.db.postgresql.fk.inline", "true");
		}
    renameProperty("workbench.gui.selection.summar", "workbench.gui.data.selection.summary");
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
			this.props.remove("workbench.db.microsoft_sql_server.dropindex.needstable");

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
			props.remove("workbench.db.nonullkeyword");

			String v = props.getProperty("workbench.db.objecttype.selectable.apache_derby", "");
			if ("table,view,system table,system view,sequence,synonym".equalsIgnoreCase(v))
			{
				props.remove("workbench.db.objecttype.selectable.apache_derby");
			}
			v = props.getProperty("workbench.db.objecttype.selectable.postgresql", null);
			if ("table,view,system table,system view,sequence".equalsIgnoreCase(v))
			{
				props.remove("workbench.db.objecttype.selectable.postgresql");
			}

			this.props.remove("workbench.import.general.xml.verbosexml");
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

		if (time <= 0)
		{
			LogMgr.logWarning("Settings.wasExternallyModified()", "ConfigFile lastModified(): " + time);
		}

		if (time < this.fileTime)
		{
			LogMgr.logWarning("Settings.wasExternallyModified()", "Current modified time: " + time + " original modified time: " + fileTime);
		}
		boolean wasModified = time > this.fileTime;
		if (wasModified)
		{
			LogMgr.logInfo("Settings.wasExternallyModified()", "Config file was externally modified. Current time: " + time + " original time: " + fileTime);
		}
		return wasModified;
	}

	public WbFile getConfigFile()
	{
		return this.configfile;
	}

	/**
	 * Save all properties to the configuration file.
	 *
	 * @param renameExistingFile if true, the existing file will be renamed before the current properties are written
	 */
	public void saveSettings(boolean renameExistingFile)
	{
		if (this.props == null)
		{
			LogMgr.logWarning("Settings.saveSettings()", "saveSettings() called but properties are null!");
			return;
		}

		// Never save settings in test mode
		if (isTestMode())
		{
			LogMgr.logTrace("Settings.saveSettings()", "Test mode active. Settings are not saved.");
			return;
		}

		for (SettingsListener l : saveListener)
		{
			if (l != null) l.beforeSettingsSave();
		}

		ShortcutManager.getInstance().saveSettings();

		boolean makeVersionedBackups = getCreateSettingsBackup();
		if (renameExistingFile || (createBackup && !makeVersionedBackups))
		{
      WbFile bck = this.configfile.makeBackup();
			LogMgr.logInfo("Settings.saveSettings()", "Created backup of global settings file: " + bck);
		}

		if (makeVersionedBackups)
		{
			// versioned backups are something different than renaming the existing file
			// renameExistingFile will be true if an out of memory error occurred at some point.
			// If that happened FileVersioning might not work properly (because the JVM acts strange once an OOME occurred)
			// So both things are needed.
      createBackup(configfile);
		}

		File cfd = configfile.getParentFile();
		if (!cfd.exists())
		{
			// this can happen in console mode
			LogMgr.logInfo("Settings.saveSettings()", "Creating config directory to store settings");
			cfd.mkdirs();
		}

		try
		{
			WbProperties defaults = getDefaultProperties();
			LogMgr.logDebug("Settings.saveSettings()", "Saving global settings to: " + configfile.getFullPath());
			this.props.saveToFile(this.configfile, defaults);
			fileTime = configfile.lastModified();
		}
		catch (Throwable th)
		{
			LogMgr.logError(this, "Error saving Settings file '" + configfile.getFullPath() + "'", th);
		}

		if (this.getPKMappingFilename() != null && PkMapping.isInitialized())
		{
			PkMapping.getInstance().saveMapping(this.getPKMappingFilename());
		}
	}

	public String getDbDelimiter(String dbId)
	{
		return getProperty("workbench.db." + dbId + ".alternatedelimiter", null);
	}

	public void setDbDelimiter(String dbId, String delimiterText)
	{
    if (StringUtil.isBlank(delimiterText))
    {
      setProperty("workbench.db." + dbId + ".alternatedelimiter", null);
    }
		else
    {
      setProperty("workbench.db." + dbId + ".alternatedelimiter", delimiterText);
    }
	}

  public Set<String> getInformixProductNames()
  {
    String defaultList = getProperty("workbench.db.informix.productnames.default", null);
    List<String> propKeys = getListProperty("workbench.db.informix.productnames", false, defaultList);
    return CollectionUtil.caseInsensitiveSet(propKeys);
  }

	public Map<String, String> getDbIdMapping()
	{
		Map<String, String> mapping = new LinkedHashMap<>();

		List<String> propKeys = this.props.getKeysWithPrefix("workbench.db.name");
		for (String key : propKeys)
		{
			int pos = key.lastIndexOf('.');
			String dbid = key.substring(pos + 1);
			String name = getProperty(key, null);
			if (name != null)
			{
				mapping.put(dbid, name);
			}
		}
		return mapping;
	}

	public String getDbmsForDbId(String dbId)
	{
		return getProperty("workbench.db.name." + dbId, StringUtil.capitalize(dbId));
	}

	@Override
	public String toString()
	{
		return "[Settings]";
	}

  public PasswordTrimType getPassworTrimType()
  {
    String type = getProperty(PROPERTY_TRIM_PWD, PasswordTrimType.never.name());
    try
    {
      return PasswordTrimType.valueOf(type);
    }
    catch (Throwable th)
    {
      return PasswordTrimType.always;
    }
  }

	public static void createBackup(WbFile f)
	{
    if (f == null) return;
    if (!f.exists()) return;

		int maxVersions = getInstance().getMaxBackupFiles();
		String dir = getInstance().getBackupDir();
		String sep = getInstance().getFileVersionDelimiter();
		FileVersioner version = new FileVersioner(maxVersions, dir, sep);
		try
		{
			File bck = version.createBackup(f);
      if (bck != null)
      {
        LogMgr.logInfo("Settings.createBackup()", "Created " + bck.getAbsolutePath() + " as a backup of: " + f.getFullPath());
      }
		}
		catch (Exception e)
		{
			LogMgr.logWarning("Settings.createBackup()", "Error when creating backup for: " + f.getAbsolutePath(), e);
		}
	}

}
