/*
 * Settings.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import workbench.WbManager;
import workbench.interfaces.PropertyStorage;
import workbench.util.ExceptionUtil;
import workbench.gui.actions.ActionRegistration;
import workbench.interfaces.FontChangedListener;
import workbench.log.LogMgr;
import workbench.storage.PkMapping;
import workbench.util.FileDialogUtil;
import workbench.util.StringUtil;
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

	public static final Settings getInstance()
	{
		if (settings == null)
		{
			settings = new Settings();
		}
		return settings;
	}

	private Settings()
	{
		if (WbManager.trace) System.out.println("Settings.<init> - start");
		this.props = new WbProperties();
		this.filename = System.getProperty("workbench.settings.file", null);
		
		// first read the built-in defaults
		// this ensures that new defaults will be applied automatically.
		fillDefaults();

		this.configDir = this.props.getProperty("workbench.configdir", null);
		if (configDir == null)
		{
			this.configDir = System.getProperty("workbench.configdir", "");
		}

		File cf;
		if (configDir == null || configDir.trim().length() == 0)
		{
			// use the current working directory as the configuration directory
			cf = new File("");
		}
		else
		{
			// "normalize" the directory name based on the platform
			cf = new File(configDir);
		}
		configDir = cf.getAbsolutePath();

		if (WbManager.trace) System.out.println("Settings.<init> - using configDir: " + configDir);

		if (filename == null)
		{
			File f = new File(this.configDir, "workbench.settings");
			this.filename = f.getAbsolutePath();
			if (WbManager.trace) System.out.println("Settings.<init> - using configfile: " + this.filename);
		}

	  if (WbManager.trace) System.out.println("Settings.<init> - Reading settings");
	  try
		{
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(this.filename));
			this.props.load(in);
			in.close();
		}
		catch (IOException e)
		{
			fillDefaults();
		}

	  if (WbManager.trace) System.out.println("Settings.<init> - Done reading settings. Initializing LogMgr");

		boolean logSysErr = StringUtil.stringToBool(this.props.getProperty("workbench.log.console", "false"));
		String sysLog = System.getProperty("workbench.log.console", null);
		if (sysLog != null)
		{
			logSysErr = "true".equals(sysLog);
		}

		LogMgr.logToSystemError(logSysErr);

		String format = this.props.getProperty("workbench.log.format", "{type} {timestamp} {message} {error}");
		LogMgr.setMessageFormat(format);

		String level = this.props.getProperty("workbench.log.level", "info");
		LogMgr.setLevel(level);

		String logfile = null;
    try
    {
			logfile = System.getProperty("workbench.log.filename", null);
			if (logfile == null)
			{
				logfile = this.props.getProperty("workbench.log.filename", FileDialogUtil.CONFIG_DIR_KEY + "/workbench.log");
			}
			int maxSize = this.getMaxLogfileSize();
			if (logfile.indexOf("%configdir%") > -1)
			{
				logfile = StringUtil.replace(logfile, "%configdir%", FileDialogUtil.CONFIG_DIR_KEY);
			}
			logfile = StringUtil.replace(logfile, FileDialogUtil.CONFIG_DIR_KEY, configDir);
			
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
	}

	public ShortcutManager getShortcutManager()
	{
		if (this.keyManager == null)
		{
			this.keyManager = new ShortcutManager(this.getShortcutFilename());
			// make sure actions that are not created upon startup are
			// registered with us!
			ActionRegistration.registerActions();
		}
		return this.keyManager;
	}

	public String getConfigDir() { return this.configDir; }
	public void setConfigDir(String aDir) { this.configDir = aDir; }

	public String getLibDir()
	{
		return System.getProperty("workbench.libdir", getProperty("workbench.libdir", null));
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
	
	public void setStoreExplorerSchema(boolean flag)
	{
		setProperty("workbench.dbexplorer.rememberSchema", flag);
	}
	
	public boolean getStoreExplorerSchema()
	{
		return getBoolProperty("workbench.dbexplorer.rememberSchema", true);
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
			PkMapping.getInstance().saveMapping(this.getPKMappingFilename());
		}
	}

	private void renameOldProps()
	{
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

			// not needed any longer
			this.props.remove("workbench.db.oracle.quotedigits");
			this.props.remove("workbench.gui.macros.replaceonrun");
		}
		catch (Throwable e)
		{
			LogMgr.logWarning("Settings.removeObsolete()", "Error when removing obsolete properties", e);
		}
	}

	private void fillDefaults()
	{
		if (WbManager.trace) System.out.println("Setting.fillDefaults() - start");

		try
		{
			this.props.load(ResourceMgr.getDefaultSettings());
		}
		catch (IOException e)
		{
			LogMgr.logError(this, "Could not read default settings", e);
		}
		if (WbManager.trace) System.out.println("Setting.fillDefaults() - done");
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
		return StringUtil.getIntValue(this.props.getProperty("workbench.print.orientation"), PageFormat.PORTRAIT);
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

		String baseKey = new StringBuffer("workbench.font.").append(aFontName).toString();
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
		return getBoolProperty("workbench.gui.display.rowheightresize", true);
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
		String value = this.props.getProperty("workbench.sql.parameter.prefix", "$[");
		if (value == null || value.length() == 0) value = "$[";
		return value;
	}

	public String getSqlParameterSuffix()
	{
		return this.props.getProperty("workbench.sql.parameter.suffix", "]");
	}

	public int getMaxLogfileSize()
	{
		return this.getIntProperty("workbench.log.maxfilesize", 30000);
	}

	public String getCodeSnippetPrefix()
	{
		String value = this.props.getProperty("workbench.editor.codeprefix", "String sql = ");
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
		this.props.setProperty("workbench.editor.autojumpnext", Boolean.toString(show));
	}

	public boolean getIgnoreErrors()
	{
		return StringUtil.stringToBool(this.props.getProperty("workbench.sql.ignoreerror", "false"));
	}

	public void setIgnoreErrors(boolean ignore)
	{
		this.props.setProperty("workbench.sql.ignoreerror", Boolean.toString(ignore));
	}

	public boolean useOracleCharSemanticsFix()
	{
		return getBoolProperty("workbench.db.oracle.fixcharsemantics", true);
	}
	
	public boolean getCheckPreparedStatements()
	{
		return StringUtil.stringToBool(this.props.getProperty("workbench.sql.checkprepared", "false"));
	}
	public void setCheckPreparedStatements(boolean show)
	{
		this.props.setProperty("workbench.sql.checkprepared", Boolean.toString(show));
	}

	public boolean getHighlightCurrentStatement()
	{
		return StringUtil.stringToBool(this.props.getProperty("workbench.editor.highlightcurrent", "false"));
	}
	
	public void setHighlightCurrentStatement(boolean show)
	{
		this.props.setProperty("workbench.editor.highlightcurrent", Boolean.toString(show));
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
		return StringUtil.stringToBool(this.props.getProperty("workbench.sql.enable_dbms_output", "false"));
	}

	public void setEnableDbmsOutput(boolean aFlag)
	{
		this.props.setProperty("workbench.sql.enable_dbms_output", Boolean.toString(aFlag));
	}

	public int getDbmsOutputDefaultBuffer()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.dbms_output.defaultbuffer", "-1"));
	}

	public void setDbmsOutputDefaultBuffer(int aSize)
	{
		this.props.setProperty("workbench.sql.dbms_output.defaultbuffer", Integer.toString(aSize));
	}

	public String getLastImportDateFormat()
	{
		return this.props.getProperty("workbench.import.dateformat", this.getDefaultDateFormat());
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
		String result = this.props.getProperty("workbench.import.numberformat", null);
		if (result == null)
		{
			result = "#" + this.getDecimalSymbol() + "#";
		}
		return result;
	}

	public String getLastImportDir()
	{
		return this.props.getProperty("workbench.import.lastdir", this.getLastExportDir());
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
		return this.props.getProperty("workbench.import.quotechar", "\"");
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
		return this.props.getProperty("workbench.workspace.lastdir", this.getConfigDir());
	}

	public void setLastWorkspaceDir(String aDir)
	{
		this.props.setProperty("workbench.workspace.lastdir", aDir);
	}

	public String getLastExportDir()
	{
		return this.props.getProperty("workbench.export.lastdir","");
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
		this.props.setProperty("workbench.javacode.includenewline", Boolean.toString(useEncryption));
	}

	public String getLastSqlDir()
	{
		return this.props.getProperty("workbench.sql.lastscriptdir","");
	}

	public void setLastSqlDir(String aDir)
	{
		this.props.setProperty("workbench.sql.lastscriptdir", aDir);
	}

	public String getLastJavaDir()
	{
		return this.props.getProperty("workbench.editor.java.lastdir","");
	}

	public void setLastJavaDir(String aDir)
	{
		this.props.setProperty("workbench.editor.java.lastdir", aDir);
	}

	public String getLastEditorDir()
	{
		return this.props.getProperty("workbench.editor.lastdir","");
	}

	public void setLastEditorDir(String aDir)
	{
		this.props.setProperty("workbench.editor.lastdir", aDir);
	}

	public String getLastFilterDir() 
	{
		return this.props.getProperty("workbench.filter.lastdir","");
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
		this.props.setProperty("workbench.sql.checkescapedquotes", Boolean.toString(flag));
	}

	public boolean getAutoConnectDataPumper()
	{
		return getBoolProperty("workbench.datapumper.autoconnect", true);
	}

	public void setAutoConnectDataPumper(boolean flag)
	{
		this.props.setProperty("workbench.datapumper.autoconnect", Boolean.toString(flag));
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

	public boolean restoreWindowSize(Component target, String id)
	{
		boolean result = false;
		int w = this.getWindowWidth(id);
		int h = this.getWindowHeight(id);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		if (w > 0 && h > 0 && w <= screen.getWidth() && h <= screen.getHeight())
		{
			target.setSize(new Dimension(w, h));
			result = true;
		}
		return result;
	}

	public boolean restoreWindowPosition(Component target)
	{
		return this.restoreWindowPosition(target, target.getClass().getName());
	}

	public boolean restoreWindowPosition(Component target, String id)
	{
		boolean result = false;
		int x = this.getWindowPosX(id);
		int y = this.getWindowPosY(id);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		if (x > 0 && y > 0 && x <= screen.getWidth() - 20 && y <= screen.getHeight() - 20)
		{
			target.setLocation(new Point(x, y));
			result = true;
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
		return StringUtil.getIntValue(this.props.getProperty(windowClass + ".x", "0"));
	}

	public int getWindowPosY(String windowClass)
	{
		return StringUtil.getIntValue(this.props.getProperty(windowClass + ".y", "0"));
	}

	public int getWindowWidth(String windowClass)
	{
		return StringUtil.getIntValue(this.props.getProperty(windowClass + ".width", "0"));
	}

	public int getWindowHeight(String windowClass)
	{
		return StringUtil.getIntValue(this.props.getProperty(windowClass + ".height", "0"));
	}

	public int getEditorTabWidth()
	{
		return StringUtil.getIntValue(this.props.getProperty(PROPERTY_EDITOR_TAB_WIDTH, "2"));
	}

	public void setEditorTabWidth(int aWidth)
	{
		this.props.setProperty(PROPERTY_EDITOR_TAB_WIDTH, Integer.toString(aWidth));
	}

	public String getLastConnection(String key)
	{
		if (key == null) return this.props.getProperty("workbench.connection.last");
		return this.props.getProperty(key);
	}

	public String getLastConnection()
	{
		return this.getLastConnection("workbench.connection.last");
	}

	public void setLastExplorerConnection(String aName)
	{
		if (aName == null) aName = "";
		this.props.setProperty("workbench.dbexplorer.connection.last", aName);
	}

	public void setLastConnection(String aName)
	{
		if (aName == null) aName = "";
		this.props.setProperty("workbench.connection.last", aName);
	}

	public String getLastLibraryDir()
	{
		return this.props.getProperty("workbench.drivers.lastlibdir", "");
	}
	
	public void setLastLibraryDir(String aDir)
	{
		this.props.setProperty("workbench.drivers.lastlibdir", aDir);
	}

	public int getMaxHistorySize()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.historysize", "15"));
	}

	public void setMaxHistorySize(int aValue)
	{
		this.props.setProperty("workbench.sql.historysize", Integer.toString(aValue));
	}

	public void setLookAndFeelClass(String aClassname)
	{
		this.props.setProperty("workbench.gui.lookandfeelclass", aClassname);
	}

	public String getLookAndFeelClass()
	{
		return this.props.getProperty("workbench.gui.lookandfeelclass", "");
	}


	public int getMinColumnWidth()
	{
		return this.getIntProperty("workbench.sql.mincolwidth", 50);
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

	public int getMaxColumnWidth()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.maxcolwidth", "500"));
	}

	public void setMaxColumnWidth(int aWidth)
	{
		this.props.setProperty("workbench.sql.maxcolwidth", Integer.toString(aWidth));
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

	public String getDefaultDateFormat()
	{
		return this.props.getProperty(PROPERTY_DATE_FORMAT, StringUtil.ISO_DATE_FORMAT);
	}

	public String getDefaultTimestampFormat()
	{
		return this.props.getProperty(PROPERTY_DATETIME_FORMAT, StringUtil.ISO_TIMESTAMP_FORMAT);
	}

	public void setDefaultTimestampFormat(String aFormat)
	{
		this.defaultDateFormatter = null;
		this.props.setProperty(PROPERTY_DATETIME_FORMAT, aFormat);
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
		return this.props.getProperty("workbench.gui.display.decimal.separator", ".");
	}

	public void setDecimalSymbol(String aSep)
	{
		this.props.setProperty("workbench.gui.display.decimal.separator", aSep);
		this.defaultDecimalFormatter = null;
	}

	public String getAlternateDelimiter()
	{
		return this.props.getProperty("workbench.sql.alternatedelimiter", "./");
	}

	public void setAlternateDelimiter(String aDelimit)
	{
		this.props.setProperty("workbench.sql.alternatedelimiter", aDelimit);
	}

	public String getDefaultTextDelimiter()
	{
		return this.getDefaultTextDelimiter(false);
	}

	public String getQuoteChar()
	{
		return this.props.getProperty("workbench.export.text.quotechar", "");
	}

	public void setQuoteChar(String aQuoteChar)
	{
		this.props.setProperty("workbench.export.text.quotechar", aQuoteChar);
	}

	public String getDefaultDataEncoding()
	{
		String def = System.getProperty("file.encoding");
		if ("Cp1252".equals(def)) def = "ISO-8859-15";
		return this.props.getProperty("workbench.file.data.encoding", def);
	}

//	public void setDefaultDataEncoding(String enc)
//	{
//		this.props.setProperty("workbench.file.data.encoding", enc);
//	}

	public String getDefaultFileEncoding()
	{
		String def = System.getProperty("file.encoding");
		return this.props.getProperty("workbench.file.encoding", def);
	}

	public void setDefaultFileEncoding(String enc)
	{
		this.props.setProperty("workbench.file.encoding", enc);
	}

	public String getDefaultTextDelimiter(boolean readable)
	{
		String del = this.props.getProperty("workbench.export.text.fielddelimiter", "\\t");
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
		String del = this.props.getProperty("workbench.import.text.fielddelimiter", "\\t");
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

	public boolean getConsolidateLogMsg()
	{
		return getBoolProperty("workbench.gui.log.consolidate", false);
	}

	public void setConsolidateLogMsg(boolean aFlag)
	{
		this.props.setProperty("workbench.gui.log.consolidate", Boolean.toString(aFlag));
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
		return this.getBoolProperty("workbench.sort.usecollator", false);
	}

	public String getSortLanguage()
	{
		return this.props.getProperty("workbench.sort.language", System.getProperty("user.language"));
	}

	public String getSortCountry()
	{
		return this.props.getProperty("workbench.sort.country", System.getProperty("user.country"));
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
		return this.props.getBoolProperty(property, false);
	}

	public boolean getBoolProperty(String property, boolean defaultValue)
	{
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
		return this.props.getProperty(aProperty, aDefault);
	}

	public int getIntProperty(String aProperty, int defaultValue)
	{
		String value = this.getProperty(aProperty, null);
		return StringUtil.getIntValue(value, defaultValue);
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
		this.props.setProperty(PROPERTY_ENCRYPT_PWD, Boolean.toString(useEncryption));
	}

	public boolean getRetrieveDbExplorer()
	{
		return getBoolProperty("workbench.dbexplorer.retrieveonopen", true);
	}

	public void setRetrieveDbExplorer(boolean aFlag)
	{
		this.props.setProperty("workbench.dbexplorer.retrieveonopen", Boolean.toString(aFlag));
	}

	public boolean getAutoSaveWorkspace()
	{
		return getBoolProperty("workbench.workspace.autosave", false);
	}

	public void setAutoSaveWorkspace(boolean aFlag)
	{
		this.props.setProperty("workbench.workspace.autosave", Boolean.toString(aFlag));
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
		this.props.setProperty(PROPERTY_ANIMATED_ICONS, Boolean.toString(flag));
	}

	public boolean getUseDynamicLayout()
	{
		return getBoolProperty("workbench.gui.dynamiclayout", true);
	}

	public void setUseDynamicLayout(boolean flag)
	{
		this.props.setProperty("workbench.gui.dynamiclayout", Boolean.toString(flag));
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
		String list = this.props.getProperty("workbench.db.ddlneedscommit", "");
    return StringUtil.stringToList(list, ",");
	}

	public List getServersWithInlineConstraints()
	{
		String list = this.props.getProperty("workbench.db.inlineconstraints", "");
		return StringUtil.stringToList(list, ",");
	}
	public List getServersWhichNeedJdbcCommit()
	{
		String list = this.props.getProperty("workbench.db.usejdbccommit", "");
    return StringUtil.stringToList(list, ",");
	}

	public List getServersWithNoNullKeywords()
	{
		String list = this.props.getProperty("workbench.db.nonullkeyword", "");
		return StringUtil.stringToList(list, ",");
	}
	public List getCaseSensitivServers()
	{
		String list = this.props.getProperty("workbench.db.casesensitive", "");
		return StringUtil.stringToList(list, ",");
	}

	public List getCancelWithReconnectDrivers()
	{
		String list = this.props.getProperty("workbench.db.cancelwithreconnect", "");
		return StringUtil.stringToList(list, ",");
	}

}
