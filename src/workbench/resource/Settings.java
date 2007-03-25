/*
 * Settings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import workbench.util.WbProperties;


/**
 *
 *	@author  support@sql-workbench.net
 */
public class Settings
	implements PropertyStorage
{
	public static final String PROPERTY_ANIMATED_ICONS = "workbench.gui.animatedicon";
	public static final String PROPERTY_ENCRYPT_PWD = "workbench.profiles.encryptpassword";
	public static final String PROPERTY_DATE_FORMAT = "workbench.gui.display.dateformat";
	public static final String PROPERTY_DATETIME_FORMAT = "workbench.gui.display.datetimeformat";
	public static final String PROPERTY_TIME_FORMAT = "workbench.gui.display.timeformat";
	public static final String PROPERTY_PDF_READER_PATH = "workbench.gui.pdfreader.path";

	public static final String PROPERTY_EDITOR_FONT = "editor";
	public static final String PROPERTY_STANDARD_FONT = "standard";
	public static final String PROPERTY_MSGLOG_FONT = "msglog";
	public static final String PROPERTY_DATA_FONT = "data";
	public static final String PROPERTY_PRINTER_FONT = "printer";

	public static final String PROPERTY_PROFILE_STORAGE = "workbench.settings.profilestorage";
	public static final String PROPERTY_EDITOR_TAB_WIDTH = "workbench.editor.tabwidth";

	private WbProperties props;
	private String filename;
	private ArrayList fontChangeListeners = new ArrayList();
	private String configDir;

	private ShortcutManager keyManager;

	private static Settings settings;

	public synchronized static final Settings getInstance()
	{
		if (settings == null)
		{
			settings = new Settings();
		}
		return settings;
	}

	private Settings()
	{
		WbManager.trace("Settings.<init> - start");
		this.props = new WbProperties();
		String configFile = System.getProperty("workbench.settings.file", "workbench.settings");

		// first read the built-in defaults
		// this ensures that new defaults will be applied automatically.
		fillDefaults();

		this.configDir = getProperty("workbench.configdir", null);
		if (configDir == null)
		{
			this.configDir = System.getProperty("workbench.configdir", "");
		}

		File cfd = null;
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
					cfd = f.getParentFile();//new File(System.getProperty("user.dir"));
					WbManager.trace("Settings.<init> - Using 'user.dir'");
				}
				else
				{
					cfd = new File(WbManager.getInstance().getJarPath());
					WbManager.trace("Settings.<init> - Using Directory of JAR file");
				}
				//cfd = new File(System.getProperty("user.dir"));
				configDir = cfd.getAbsolutePath();
			}
			else
			{
				if ("${user.home}".equals(configDir))
				{
					this.configDir = System.getProperty("user.home");
				}
				cfd = new File(configDir);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("Settings.<init>", "Error when initializing configdir", e);
			cfd = new File(System.getProperty("user.dir"));
		}

		configDir = cfd.getAbsolutePath();

		WbManager.trace("Settings.<init> - using configDir: " + configDir);

		File cf = new File(this.configDir, configFile);
		this.filename = cf.getAbsolutePath();

		WbManager.trace("Settings.<init> - using configfile: " + this.filename);

	  WbManager.trace("Settings.<init> - Reading settings");
		BufferedInputStream in = null;
	  try
		{
			in = new BufferedInputStream(new FileInputStream(this.filename));
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
		
	  WbManager.trace("Settings.<init> - Done reading settings. Initializing LogMgr");

		boolean logSysErr = getBoolProperty("workbench.log.console", false);
		String sysLog = System.getProperty("workbench.log.console", null);
		if (sysLog != null)
		{
			logSysErr = "true".equals(sysLog);
		}

		LogMgr.logToSystemError(logSysErr);

		String format = getProperty("workbench.log.format", "{type} {timestamp} {message} {error}");
		LogMgr.setMessageFormat(format);

		String level = getProperty("workbench.log.level", "info");
		LogMgr.setLevel(level);

		String logfile = null;
    try
    {
			logfile = getProperty("workbench.log.filename", FileDialogUtil.CONFIG_DIR_KEY + "/workbench.log");
			int maxSize = this.getMaxLogfileSize();
			if (logfile.indexOf("%configdir%") > -1)
			{
				logfile = StringUtil.replace(logfile, "%configdir%", configDir);
			}
			else
			{
				logfile = StringUtil.replace(logfile, FileDialogUtil.CONFIG_DIR_KEY, configDir);
			}

			// Replace old System.out or System.err settings
			if (logfile.equalsIgnoreCase("System.out") || logfile.equalsIgnoreCase("System.err"))
			{
				File f = new File(configDir, "workbench.log");
				logfile = f.getAbsolutePath();
				this.props.setProperty("workbench.log.filename", FileDialogUtil.CONFIG_DIR_KEY + "/workbench.log");
			}

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
	}

	public ShortcutManager getShortcutManager()
	{
		if (this.keyManager == null)
		{
			this.keyManager = new ShortcutManager(this.getShortcutFilename());
		}
		return this.keyManager;
	}

	public String getConfigDir() { return this.configDir; }
	public void setConfigDir(String aDir) { this.configDir = aDir; }

	public String getLibDir()
	{
		return System.getProperty("workbench.libdir", getProperty("workbench.libdir", null));
	}

	public void setLastUsedBlobTool(String name)
	{
		setProperty("workbench.tools.last.blob", name);
	}

	public String getLastUsedBlobTool()
	{
		return getProperty("workbench.tools.last.blob", null);
	}

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

	public static final String PK_MAPPING_FILENAME_PROPERTY = "workbench.pkmapping.file";
	public String getPKMappingFilename()
	{
		String fName = System.getProperty(PK_MAPPING_FILENAME_PROPERTY, getProperty(PK_MAPPING_FILENAME_PROPERTY, null));
		if (StringUtil.isEmptyString(fName)) return null;
		String dir = getConfigDir();
		return StringUtil.replace(fName, FileDialogUtil.CONFIG_DIR_KEY, dir);
	}

	public void setPKMappingFilename(String file)
	{
		setProperty(PK_MAPPING_FILENAME_PROPERTY,file);
	}

	private String getShortcutFilename()
	{
		return new File(this.configDir, "WbShortcuts.xml").getAbsolutePath();
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
	
	public boolean getIncludeHeaderInOptimalWidth()
	{
		return getBoolProperty("workbench.gui.optimalwidth.includeheader", false);
	}

	public void setIncludeHeaderInOptimalWidth(boolean flag)
	{
		setProperty("workbench.gui.optimalwidth.includeheader", flag);
	}

	public boolean getAutomaticOptimalWidth()
	{
		return getBoolProperty("workbench.gui.optimalwidth.automatic", false);
	}

	public void setAutomaticOptimalWidth(boolean flag)
	{
		setProperty("workbench.gui.optimalwidth.automatic", flag);
	}

	public String getPDFReaderPath()
	{
		return getProperty(PROPERTY_PDF_READER_PATH, null);
	}

	public void setPDFReaderPath(String path)
	{
		setProperty(PROPERTY_PDF_READER_PATH, path);
	}

	public String getManualPath()
	{
		String pdfManual = getProperty("workbench.pdfmanual.filename", "SQLWorkbench-Manual.pdf");
		File f = new File(pdfManual);
		// This allows to overwrite the location of the manual completely...
		if (f.isAbsolute() && f.exists() && f.canRead())
		{
			return f.getAbsolutePath();
		}
		String jarDir = WbManager.getInstance().getJarPath();
		File pdf = new File(jarDir, pdfManual);
		if (pdf.exists() && pdf.canRead())
		{
			return pdf.getAbsolutePath();
		}
		else
		{
			return null;
		}
	}

	public static final String UNIX_LINE_TERMINATOR_PROP_VALUE = "lf";
	public static final String DOS_LINE_TERMINATOR_PROP_VALUE = "crlf";
	public static final String DEFAULT_LINE_TERMINATOR_PROP_VALUE = "default";

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
			return new File(this.configDir, "WbProfiles.xml").getAbsolutePath();
		}
		String realFilename = FileDialogUtil.replaceConfigDir(profiles);

		// Check if filename contains a directory
		File f = new File(realFilename);
		if (f.getParent() == null)
		{
			// no directory in filename -> use config directory
			f = new File(this.configDir, realFilename);
		}
		LogMgr.logInfo("Settings.getProfileFilename()", "Using profiles from " + f.getAbsolutePath());
		return f.getAbsolutePath();
	}

	public void removeProperty(String property)
	{
		this.props.remove(property);
	}

	public boolean getAutoGeneratePKName()
	{
		return getBoolProperty("workbench.db.createpkname", true);
	}

	public void setAutoGeneratePKName(boolean flag)
	{
		setProperty("workbench.db.createpkname", flag);
	}

	public String getDriverConfigFilename()
	{
		return new File(this.configDir, "WbDrivers.xml").getAbsolutePath();
	}

	public void addFontChangedListener(FontChangedListener aListener)
	{
		this.fontChangeListeners.add(aListener);
	}

	public void removeFontChangedListener(FontChangedListener aListener)
	{
		this.fontChangeListeners.remove(aListener);
	}

	public void addPropertyChangeListener(PropertyChangeListener l)
	{
		this.props.addPropertyChangeListener(l);
	}

	public void removePropertyChangeLister(PropertyChangeListener l)
	{
		this.props.removePropertyChangeListener(l);
	}

	public void saveSettings()
	{
		if (this.props == null) return;
		if (keyManager!= null) this.keyManager.saveSettings();
		this.removeObsolete();
		try
		{
			this.props.saveToFile(this.filename);
		}
		catch (IOException e)
		{
			LogMgr.logError(this, "Error saving Settings file '" + filename + "'", e);
		}
		if (this.getPKMappingFilename() != null)
		{
			if (PkMapping.isInitialized())
			{
				PkMapping.getInstance().saveMapping(this.getPKMappingFilename());
			}
		}
	}

	private void migrateProps()
	{
		List servers = getServersWhereDDLNeedsCommit();
		if (!servers.contains("Microsoft SQL Server"))
		{
			servers.add("Microsoft SQL Server");
		}
		String val = 	StringUtil.listToString(servers, ',');
		setProperty("workbench.db.ddlneedscommit", val);
		
		// Fix incorrectly distributed defaults
		String defaultSelectable = getProperty("workbench.db.objecttype.selectable.default", null);
		if (defaultSelectable != null)
		{
			List types = StringUtil.stringToList(defaultSelectable.toLowerCase(), ",", true, true, false);
			if (!types.contains("synonym"))
			{
				types.add("synonym");
				setProperty("workbench.db.objecttype.selectable.default", StringUtil.listToString(types, ','));
			}
		}
		upgradeListProp("workbench.db.oracle.syntax.functions");
	}

	private void upgradeListProp(String key)
	{
		String currentValue = getProperty(key, "");
		List currentList = StringUtil.stringToList(currentValue, ",", true, true, false);
		
		WbProperties defProps = getDefaultProperties();
		String defValue = defProps.getProperty(key, "");
		List defList = StringUtil.stringToList(defValue, ",", true, true, false);
		
		currentList.addAll(defList);
		this.setProperty(key, StringUtil.listToString(currentList,','));
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
			// added for build 82
			this.props.remove("workbench.db.fetchsize");
			
			// added for build 84
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

			// not needed any longer
			this.props.remove("workbench.db.oracle.quotedigits");
			this.props.remove("workbench.gui.macros.replaceonrun");
			this.props.remove("workbench.db.cancelneedsreconnect");
			this.props.remove("workbench.db.trigger.replacenl");
			this.props.remove("workbench.sql.multipleresultsets");
			
			this.props.remove("workbench.db.keywordlist.oracle");
			this.props.remove("workbench.db.keywordlist.thinksql_relational_database_management_system");
		}
		catch (Throwable e)
		{
			LogMgr.logWarning("Settings.removeObsolete()", "Error when removing obsolete properties", e);
		}
	}

	private WbProperties getDefaultProperties()
	{
		WbProperties defProps = new WbProperties();
		try
		{
			defProps.load(ResourceMgr.getDefaultSettings());
		}
		catch (IOException e)
		{
			LogMgr.logError(this, "Could not read default settings", e);
		}
		return defProps;
	}
	
	private void fillDefaults()
	{
		WbManager.trace("Setting.fillDefaults() - start");

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
		WbManager.trace("Setting.fillDefaults() - done");
	}

	public int getMaxMacrosInMenu()
	{
		return getIntProperty("workbench.gui.macro.maxmenuitems", 9);
	}

	public Font getStandardFont()
	{
		Font f = this.getFont(PROPERTY_STANDARD_FONT,false);
		if (f == null)
		{
			UIDefaults def = UIManager.getLookAndFeelDefaults();
			f = def.getFont("Menu.font");
		}
		return f;
	}

	public void setStandardFont(Font f)
	{
		setFont(PROPERTY_STANDARD_FONT,f);
	}

	public Font getStandardLabelFont()
	{
		Font f = this.getStandardFont();
		if (f == null)
		{
			UIDefaults def = UIManager.getLookAndFeelDefaults();
			f = def.getFont("Label.font");
		}
		return f;
	}

	public Font getStandardMenuFont()
	{
		Font f = this.getStandardFont();
		if (f == null)
		{
			UIDefaults def = UIManager.getLookAndFeelDefaults();
			f = def.getFont("Menu.font");
		}
		return f;
	}

	public void setEditorFont(Font f)
	{
		this.setFont(PROPERTY_EDITOR_FONT, f);
	}

	public Font getEditorFont()
	{
		return this.getFont(PROPERTY_EDITOR_FONT);
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
		Font f = this.getFont(PROPERTY_DATA_FONT, false);
		if (f == null) f = this.getStandardFont();
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

	public Font getFont(String aFontName)
	{
		return this.getFont(aFontName, true);
	}

	/**
	 *	Returns the font configured for this keyword
	 */
	public Font getFont(String aFontName, boolean returnDefault)
	{
		Font result;

		String baseKey = "workbench.font." + aFontName;
		String name = null;

		if (returnDefault)
			name = this.props.getProperty(baseKey + ".name", "Dialog");
		else
			name = this.props.getProperty(baseKey + ".name", null);

		if (name == null) return null;

		String sizeS = this.props.getProperty(baseKey + ".size", "11");
		String type = this.props.getProperty(baseKey + ".style", "Plain");
		int style = Font.PLAIN;
		int size = 11;
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
	
	public boolean getCopySelectedIsDefault()
	{
		String value = getProperty("workbench.gui.table.copydefault", "all");
		return "selected".equalsIgnoreCase(value);
	}

	public boolean getUseAlternateRowColor()
	{
		return getBoolProperty("workbench.gui.table.alternate.use", false);
	}

	public void setUseAlternateRowColor(boolean flag)
	{
		setProperty("workbench.gui.table.alternate.use", flag);
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

	public void setPrintFont(Font aFont)
	{
		this.setFont(PROPERTY_PRINTER_FONT, aFont);
	}

	public void setFont(String aFontName, Font aFont)
	{
		if (aFont == null) return;

		String baseKey = new StringBuilder("workbench.font.").append(aFontName).toString();
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
		for (int i=0; i < this.fontChangeListeners.size(); i++)
		{
			FontChangedListener listener = (FontChangedListener)this.fontChangeListeners.get(i);
			if (listener != null)	listener.fontChanged(aKey, aFont);
		}
	}

	public static final int SHOW_NO_FILENAME = 0;
	public static final int SHOW_FILENAME = 1;
	public static final int SHOW_FULL_PATH = 2;

	public boolean getAllowRowHeightResizing()
	{
		return getBoolProperty("workbench.gui.display.rowheightresize", false);
	}

	public void setAllowRowHeightResizing(boolean flag)
	{
		setProperty("workbench.gui.display.rowheightresize", flag);
	}

	public boolean getShowRowNumbers()
	{
		return getBoolProperty("workbench.data.rownumbers", false);
	}

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

	public String getCodeSnippetPrefix()
	{
		String value = getProperty("workbench.editor.codeprefix", "String sql = ");
		return value;
	}

	public static final String PROPERTY_SHOW_LINE_NUMBERS = "workbench.editor.showlinenumber";

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
		return getBoolProperty("workbench.editor.autojumpnext", false);
	}

	public void setAutoJumpNextStatement(boolean show)
	{
		this.setProperty("workbench.editor.autojumpnext", show);
	}

	public boolean getIgnoreErrors()
	{
		return StringUtil.stringToBool(this.props.getProperty("workbench.sql.ignoreerror", "false"));
	}

	public void setIgnoreErrors(boolean ignore)
	{
		this.setProperty("workbench.sql.ignoreerror", ignore);
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

	public boolean getHighlightCurrentStatement()
	{
		return getBoolProperty("workbench.editor.highlightcurrent", false);
	}

	public void setHighlightCurrentStatement(boolean show)
	{
		this.setProperty("workbench.editor.highlightcurrent", show);
	}

	public boolean getIncludeOwnerInSqlExport()
	{
		return this.getBoolProperty("workbench.export.sql.includeowner", true);
	}

	public void setIncludeOwnerInSqlExport(boolean flag)
	{
		setProperty("workbench.export.sql.includeowner", flag);
	}

	public boolean getEnableDbmsOutput()
	{
		return getBoolProperty("workbench.sql.enable_dbms_output", false);
	}

	public void setEnableDbmsOutput(boolean aFlag)
	{
		this.setProperty("workbench.sql.enable_dbms_output", aFlag);
	}

	public int getDbmsOutputDefaultBuffer()
	{
		return getIntProperty("workbench.sql.dbms_output.defaultbuffer", -1);
	}

	public void setDbmsOutputDefaultBuffer(int aSize)
	{
		this.setProperty("workbench.sql.dbms_output.defaultbuffer", aSize);
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

	public String getLastImportDir()
	{
		return getProperty("workbench.import.lastdir", this.getLastExportDir());
	}

	public void setLastImportDir(String aDir)
	{
		this.props.setProperty("workbench.import.lastdir", aDir);
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
		return getProperty("workbench.workspace.lastdir", this.getConfigDir());
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

	public boolean getIncludeNewLineInCodeSnippet()
	{
		return getBoolProperty("workbench.javacode.includenewline", true);
	}

	public void setIncludeNewLineInCodeSnippet(boolean useEncryption)
	{
		this.setProperty("workbench.javacode.includenewline", useEncryption);
	}

	public String getLastSqlDir()
	{
		return getProperty("workbench.sql.lastscriptdir","");
	}

	public void setLastSqlDir(String aDir)
	{
		this.props.setProperty("workbench.sql.lastscriptdir", aDir);
	}

	public String getLastJavaDir()
	{
		return getProperty("workbench.editor.java.lastdir","");
	}

	public void setLastJavaDir(String aDir)
	{
		this.props.setProperty("workbench.editor.java.lastdir", aDir);
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

	public String toString()
	{
		return "[Settings]";
	}

	public boolean getCheckEscapedQuotes()
	{
		return getBoolProperty("workbench.sql.checkescapedquotes", false);
	}

	public void setCheckEscapedQuotes(boolean flag)
	{
		this.setProperty("workbench.sql.checkescapedquotes", flag);
	}

	public boolean getAutoConnectDataPumper()
	{
		return getBoolProperty("workbench.datapumper.autoconnect", true);
	}

	public void setAutoConnectDataPumper(boolean flag)
	{
		this.setProperty("workbench.datapumper.autoconnect", flag);
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

	private void setColor(String key, Color c)
	{
		int r = c.getRed();
		int g = c.getGreen();
		int b = c.getBlue();
		String value = Integer.toString(r) + "," + Integer.toString(g) + "," + Integer.toString(b);
		this.setProperty(key, value);
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

	public int getElectricScroll()
	{
		return this.getIntProperty("workbench.editor.electricscroll", 3);
	}

	public void setElectricScroll(int value)
	{
		setProperty("workbench.editor.electricscroll", (value < 0 ? 3 : value));
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

	public int getEditorTabWidth()
	{
		return getIntProperty(PROPERTY_EDITOR_TAB_WIDTH, 2);
	}

	public void setEditorTabWidth(int aWidth)
	{
		this.setProperty(PROPERTY_EDITOR_TAB_WIDTH, aWidth);
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

	public String getLastLibraryDir()
	{
		return getProperty("workbench.drivers.lastlibdir", "");
	}

	public void setLastLibraryDir(String aDir)
	{
		this.props.setProperty("workbench.drivers.lastlibdir", aDir);
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

	public void setLookAndFeelClass(String aClassname)
	{
		this.props.setProperty("workbench.gui.lookandfeelclass", aClassname);
	}

	public String getLookAndFeelClass()
	{
		return getProperty("workbench.gui.lookandfeelclass", "");
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

	public int getFormatterMaxColumnsInSelect()
	{
		return getIntProperty("workbench.sql.formatter.select.columnsperline", 1);
	}

	public void setFormatterMaxColumnsInSelect(int value)
	{
		setProperty("workbench.sql.formatter.select.columnsperline", value);
	}

	public int getFormatterMaxSubselectLength()
	{
		return getIntProperty("workbench.sql.formatter.subselect.maxlength", 60);
	}

	public void setFormatterMaxSubselectLength(int value)
	{
		setProperty("workbench.sql.formatter.subselect.maxlength", value);
	}

	public boolean getRightClickMovesCursor()
	{
		return this.getBoolProperty("workbench.editor.rightclickmovescursor", false);
	}

	public void setRightClickMovesCursor(boolean flag)
	{
		setProperty("workbench.editor.rightclickmovescursor", flag);
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

	public int getProfileDividerLocation()
	{
		return getIntProperty("workbench.gui.profiles.divider", -1);
	}

	public void setProfileDividerLocation(int aValue)
	{
		this.props.setProperty("workbench.gui.profiles.divider", Integer.toString(aValue));
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

	public int getIntProperty(String aProperty, int defaultValue)
	{
		String sysValue = System.getProperty(aProperty, null);
		if (sysValue != null)
		{
			return StringUtil.getIntValue(sysValue, defaultValue);
		}
		return this.props.getIntProperty(aProperty, defaultValue);
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

	public void setShowDbExplorerInMainWindow(boolean showWindow)
	{
		this.setProperty("workbench.dbexplorer.mainwindow", showWindow);
	}

	public boolean getShowDbExplorerInMainWindow()
	{
		return this.getBoolProperty("workbench.dbexplorer.mainwindow", true);
	}

	public boolean getUseEncryption()
	{
		return getBoolProperty(PROPERTY_ENCRYPT_PWD, false);
	}

	public void setUseEncryption(boolean useEncryption)
	{
		this.setProperty(PROPERTY_ENCRYPT_PWD, useEncryption);
	}

	public boolean getRetrieveDbExplorer()
	{
		return getBoolProperty("workbench.dbexplorer.retrieveonopen", true);
	}

	public void setRetrieveDbExplorer(boolean aFlag)
	{
		this.setProperty("workbench.dbexplorer.retrieveonopen", aFlag);
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

	public boolean getVerifyDriverUrl()
	{
		return getBoolProperty("workbench.db.verifydriverurl", false);
	}

	public boolean getShowMnemonics()
	{
		return getBoolProperty("workbench.gui.showmnemonics", true);
	}

	public boolean getShowSplash()
	{
		return getBoolProperty("workbench.gui.showsplash", false);
	}

	public boolean getRetrievePKList()
	{
		return getBoolProperty("workbench.db.retrievepklist", true);
	}

	public List getServersWhereDDLNeedsCommit()
	{
		String list = getProperty("workbench.db.ddlneedscommit", "");
    return StringUtil.stringToList(list, ",");
	}

	public List getServersWithInlineConstraints()
	{
		String list = getProperty("workbench.db.inlineconstraints", "");
		return StringUtil.stringToList(list, ",");
	}
	public List getServersWhichNeedJdbcCommit()
	{
		String list = getProperty("workbench.db.usejdbccommit", "");
    return StringUtil.stringToList(list, ",");
	}

	public List getServersWithNoNullKeywords()
	{
		String list = getProperty("workbench.db.nonullkeyword", "");
		return StringUtil.stringToList(list, ",");
	}

	public List getCaseSensitivServers()
	{
		String list = getProperty("workbench.db.casesensitive", "");
		return StringUtil.stringToList(list, ",");
	}


}
