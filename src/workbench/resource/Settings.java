/*
 * Settings.java
 *
 * Created on December 1, 2001, 7:00 PM
 */
package workbench.resource;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.swing.JOptionPane;
import javax.swing.text.DateFormatter;
import workbench.WbManager;
import workbench.db.DbMetadata;
import workbench.interfaces.FontChangedListener;
import workbench.log.LogMgr;
import workbench.print.PrintUtil;
import workbench.util.StringUtil;
import workbench.util.WbProperties;


/**
 *
 *	@author  workbench@kellerer.org
 */
public class Settings
{
	public static final String ANIMATED_ICONS_KEY = "workbench.gui.animatedicon";
	
	public static final String EDITOR_FONT_KEY = "editor";
	public static final String STANDARD_FONT_KEY = "standard";
	public static final String MSGLOG_FONT_KEY = "msglog";
	public static final String DATA_FONT_KEY = "data";
	public static final String PRINTER_FONT_KEY = "printer";

	private WbProperties props;
	private Font printerFont;
	private Font standardFont;
	private Font editorFont;
	private Font msgLogFont;
	private Font dataFont;
	private String filename;
	private ArrayList fontChangeListeners = new ArrayList();
	private String configDir;

	public Settings()
	{
		if (WbManager.trace) System.out.println("Settings.<init> - start");
		this.props = new WbProperties();
		this.filename = System.getProperty("workbench.settings.file", null);
		fillDefaults();

		this.configDir = this.props.getProperty("workbench.configdir", null);
		if (configDir == null)
		{
			this.configDir = System.getProperty("workbench.configdir", "");
		}

		if (configDir == null || configDir.trim().length() == 0)
		{
			File f = new File("");
			configDir = f.getAbsolutePath();
		}
	
		if (WbManager.trace) System.out.println("Settings.<init> - using configDir: " + configDir);
		String sep = System.getProperty("file.separator");
		if (!this.configDir.endsWith(sep))
		{
			configDir = configDir + sep;
		}
		
		if (filename == null) this.filename = this.configDir + "workbench.settings";

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

	  if (WbManager.trace) System.out.println("Settings.<init> - Done reading settings");

    try
    {
			String logfile = System.getProperty("workbench.log.filename", null);
			if (logfile == null)
			{
				logfile = this.props.getProperty("workbench.log.filename", "Workbench.log");
			}
			LogMgr.setOutputFile(logfile);
    }
    catch (Throwable e)
    {
      System.err.println("Error initializing Log system!");
      e.printStackTrace(System.err);
    }

		LogMgr.logInfo("Settings.<init>", "Using configDir: " + configDir);

		if (WbManager.trace) System.out.println("Setting server lists for MetaData");
		DbMetadata.setServersWhichNeedReconnect(this.getCancelWithReconnectServers());
		DbMetadata.setCaseSensitiveServers(this.getCaseSensitivServers());
		DbMetadata.setServersWhereDDLNeedsCommit(this.getServersWhereDDLNeedsCommit());
		if (WbManager.trace) System.out.println("Done setting server lists for MetaData");

		String level = this.props.getProperty("workbench.log.level", "INFO");
		LogMgr.setLevel(level);
		
		// init settings for datastore
		System.setProperty("org.kellerer.sort.language", this.getSortLanguage());
		System.setProperty("org.kellerer.sort.country", this.getSortCountry());

		if (WbManager.trace) System.out.println("Settings.<init> - done");
	}

	public String getConfigDir() { return this.configDir; }
	public void setConfigDir(String aDir) { this.configDir = aDir; }

	public String getProfileFileName()
	{
		return this.configDir + "WbProfiles.xml";
	}

	public String getDriverConfigFileName()
	{
		return this.configDir + "WbDrivers.xml";
	}
	public void showOptionsDialog()
	{
		JOptionPane.showMessageDialog(null, "Not yet implemented. Please edit workbench.settings");
	}

	public void addFontChangedListener(FontChangedListener aListener)
	{
		this.fontChangeListeners.add(aListener);
	}

	public void removeFontChangedListener(FontChangedListener aListener)
	{
		this.fontChangeListeners.remove(aListener);
	}

	public void addChangeListener(PropertyChangeListener l)
	{
		this.props.addChangeListener(l);
	}
	
	public void removeChangeLister(PropertyChangeListener l)
	{
		this.props.removeChangeListener(l);
	}
	
	public void saveSettings()
	{
		this.removeObsolete();
		try
		{
			this.props.saveToFile(this.filename);
		}
		catch (IOException e)
		{
			LogMgr.logError(this, "Error saving Settings file '" + filename + "'", e);
		}
	}

	private void removeObsolete()
	{
		try
		{
			// remove settings which are no longer needed
			this.props.remove("workbench.sql.lasttab");
			for (int i=0; i < 10; i++)
			{
				this.props.remove("workbench.gui.sql.lastdivider" + i);
				this.props.remove("workbench.gui.sql.divider" + i);
			}
			this.props.remove("workbench.workspace.lastfile");
			this.props.remove("workbench.workspace.restorelast");
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

	public Font getStandardFont()
	{
		if (this.standardFont == null)
		{
			this.standardFont = this.getFont(STANDARD_FONT_KEY);
		}
		return this.standardFont;
	}

	public Font getEditorFont()
	{
		if (this.editorFont == null)
		{
			this.editorFont = this.getFont(EDITOR_FONT_KEY);
		}
		return editorFont;
	}

	public Font getMsgLogFont()
	{
		if (this.msgLogFont == null)
		{
			this.msgLogFont = this.getFont(MSGLOG_FONT_KEY);
		}
		return this.msgLogFont;
	}

	public Font getDataFont()
	{
		if (this.dataFont == null)
		{
			this.dataFont = this.getFont(DATA_FONT_KEY);
		}
		return this.dataFont;
	}

	public Font getPrinterFont()
	{
		if (this.printerFont == null)
		{
			this.printerFont = this.getFont(PRINTER_FONT_KEY);
			if (this.printerFont == null)
			{
				this.printerFont = this.getDataFont();
			}
		}
		return this.printerFont;
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
		if (WbManager.trace) System.out.println("Setting.getFont() - start");
		Font result;

		String baseKey = new StringBuffer("workbench.font.").append(aFontName).toString();
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
		if (WbManager.trace) System.out.println("Setting.getFont() - done");
		return result;
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

	private int getPrintOrientation()
	{
		return StringUtil.getIntValue(this.props.getProperty("print.paper.orientation"), PageFormat.PORTRAIT);
	}
	
	private void setPrintOrientation(int aValue)
	{
		this.props.setProperty("workbench.print.orientation", Integer.toString(aValue));
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
	
	public void setFont(String aFontName, Font aFont)
	{
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
		if (aFontName.equals(EDITOR_FONT_KEY))
			this.editorFont = aFont;
		else if (aFontName.equals(MSGLOG_FONT_KEY))
			this.msgLogFont = aFont;
		else if (aFontName.equals(STANDARD_FONT_KEY))
			this.standardFont = aFont;
		else if (aFontName.equals(DATA_FONT_KEY))
			this.dataFont = aFont;
		else if (aFontName.equals(PRINTER_FONT_KEY))
			this.printerFont = aFont;

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

	public boolean getEnableDbmsOutput()
	{
		return StringUtil.stringToBool(this.props.getProperty("workbench.sql.enable_dbms_output", "false"));
	}
	
	public void setEnableDbmsOutput(boolean aFlag)
	{
		this.props.setProperty("workbench.sql.enable_dbms_output", Boolean.toString(aFlag));
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

	public String getLastImportQuoteChar()
	{
		return this.props.getProperty("workbench.import.quotechar", "\"");
	}

	public void setLastImportQuoteChar(String aChar)
	{
		this.props.setProperty("workbench.import.quotechar", aChar);
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

	public String getLastTableGenerateDir()
	{
		return this.props.getProperty("workbench.persistence.lastdir.table","");
	}

	public void setLastTableGenerateDir(String aDir)
	{
		this.props.setProperty("workbench.persistence.lastdir.table", aDir);
	}

	public String getLastValueGenerateDir()
	{
		return this.props.getProperty("workbench.persistence.lastdir.value","");
	}

	public void setLastValueGenerateDir(String aDir)
	{
		this.props.setProperty("workbench.persistence.lastdir.value", aDir);
	}
	public boolean getCleanupUnderscores()
	{
		return "true".equalsIgnoreCase(this.props.getProperty("workbench.persistence.cleanupunderscores", "false"));
	}

	public void setCleanupUnderscores(boolean useEncryption)
	{
		this.props.setProperty("workbench.persistence.cleanupunderscores", Boolean.toString(useEncryption));
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


	public String toString()
	{
		return "[Settings]";
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
		if (w > 0 && h > 0)
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
		if (x > 0 && y > 0)
		{
			target.setLocation(new Point(x, y));
			result = true;
		}
		return result;
	}

	public void setEditorFile(int anEditorId, String aFilename)
	{
		if (aFilename == null) aFilename = "";
		this.props.setProperty("workbench.editor.lastfile" + anEditorId, aFilename);
	}

	public String getEditorFile(int anEditorId)
	{
		return this.props.getProperty("workbench.editor.lastfile" + anEditorId, null);
	}

	public void setLastSqlDividerLocation(int aDividerId, int y)
	{
		this.props.setProperty("workbench.gui.sql.lastdivider" + aDividerId, Integer.toString(y));
	}

	public int getLastSqlDividerLocation(int aDividerId)
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.gui.sql.lastdivider" + aDividerId, "-1"));
	}

	public void setSqlDividerLocation(int aDividerId, int y)
	{
		this.props.setProperty("workbench.gui.sql.divider" + aDividerId, Integer.toString(y));
	}

	public int getSqlDividerLocation(int aDividerId)
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.gui.sql.divider" + aDividerId, "-1"));
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
		return StringUtil.getIntValue(this.props.getProperty("workbench.editor.tabwidth", "2"));
	}

	public void setEditorTabWidth(int aWidth)
	{
		this.props.setProperty("workbench.editor.tabwidth", Integer.toString(aWidth));
	}

	public String getLastConnection()
	{
		return this.props.getProperty("connection.last");
	}

	public void setLastConnection(String aName)
	{
		if (aName == null) aName = "";
		this.props.setProperty("connection.last", aName);
	}

	public String getLastLibraryDir()
	{
		return this.props.getProperty("drivers.lastlibdir", "");
	}
	public void setLastLibraryDir(String aDir)
	{
		this.props.setProperty("drivers.lastlibdir", aDir);
	}

	public int getMaxHistorySize()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.historysize", "15"));
	}

	public void setMaxHistorySize(int aValue)
	{
		this.props.setProperty("workbench.sql.historysize", Integer.toString(aValue));
	}

	public int getDefaultTabCount()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.defaulttabcount", "4"));
	}

	public void setDefaultTabCount(int aCount)
	{
		this.props.setProperty("workbench.sql.defaulttabcount", Integer.toString(aCount));
	}

	public void setLookAndFeelClass(String aClassname)
	{
		this.props.setProperty("workbench.gui.lookandfeelclass", aClassname);
	}

	public String getLookAndFeelClass()
	{
		return this.props.getProperty("workbench.gui.lookandfeelclass", "");
	}

	public int getPreferredColumnWidth()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.preferredcolwidth", "80"));
	}
	public void setPreferredColumnWidth(int aWidth)
	{
		this.props.setProperty("workbench.sql.preferredcolwidth", Integer.toString(aWidth));
	}

	public int getMinColumnWidth()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.mincolwidth", "50"));
	}
	public void setMinColumnWidth(int aWidth)
	{
		this.props.setProperty("workbench.sql.mincolwidth", Integer.toString(aWidth));
	}

	public int getMaxColumnWidth()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.maxcolwidth", "500"));
	}

	public void setMaxColumnWidth(int aWidth)
	{
		this.props.setProperty("workbench.sql.maxcolwidth", Integer.toString(aWidth));
	}

	private DateFormat defaultDateFormatter = null;
	
	public DateFormat getDefaultDateFormatter()
	{
		if (this.defaultDateFormatter == null)
		{
			this.defaultDateFormatter = new SimpleDateFormat(this.getDefaultDateFormat());
		}
		return this.defaultDateFormatter;
	}
	
	public String getDefaultDateFormat()
	{
		return this.props.getProperty("workbench.gui.display.dateformat", "yyyy-MM-dd");
	}

	public void setDefaultDateFormat(String aFormat)
	{
		this.defaultDateFormatter = null;
		this.props.setProperty("workbench.gui.display.dateformat", aFormat);
	}

	public int getMaxFractionDigits()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.gui.display.maxfractiondigits", "2"));
	}
	public void setMaxFractionDigits(int aValue)
	{
		this.props.setProperty("workbench.gui.display.maxfractiondigits", Integer.toString(aValue));
		this.initFormatter();
	}

	private DecimalFormat defaultFormatter = null;
	private DecimalFormatSymbols decSymbols = new DecimalFormatSymbols();
	
	public DecimalFormat getDefaultDecimalFormatter()
	{
		this.initFormatter();
		return this.defaultFormatter;
	}

	private void initFormatter()
	{
		if (this.defaultFormatter == null)
		{
			this.defaultFormatter = new DecimalFormat("0.#");
		}
		String sep = this.getDecimalSymbol();
		int maxDigits = this.getMaxFractionDigits();
		this.decSymbols.setDecimalSeparator(sep.charAt(0));
		this.defaultFormatter.setDecimalFormatSymbols(this.decSymbols);
		this.defaultFormatter.setMaximumFractionDigits(maxDigits);
	}
	public String getDecimalSymbol()
	{
		return this.props.getProperty("workbench.gui.display.decimal.separator", ".");
	}

	public void setDecimalSymbol(String aSep)
	{
		this.props.setProperty("workbench.gui.display.decimal.separator", aSep);
		this.initFormatter();
	}

	public String getDecimalGroupingSeparator()
	{
		return this.props.getProperty("workbench.gui.display.decimal.groupseparator", ",");
	}

	public void getDecimalGroupingSeparator(String aSep)
	{
		this.props.setProperty("workbench.gui.display.decimal.groupseparator", aSep);
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
	
	public String getSortLanguage()
	{
		return this.props.getProperty("sort.language", System.getProperty("user.language"));
	}
	
	public String getSortCountry()
	{
		return this.props.getProperty("sort.country", System.getProperty("user.country"));
	}
	
	public void setLastImportDelimiter(String aDelimit)
	{
		if (aDelimit.equals("\t")) aDelimit = "\\t";

		this.props.setProperty("workbench.import.text.fielddelimiter", aDelimit);
	}

	public boolean getLastImportWithHeaders()
	{
		return "true".equals(this.props.getProperty("workbench.import.text.containsheader", "true"));
	}

	public void setLastImportWithHeaders(boolean aFlag)
	{
		this.props.setProperty("workbench.import.text.containsheader", Boolean.toString(aFlag));
	}

	public boolean getDbDebugMode()
	{
		return "true".equals(this.props.getProperty("workbench.db.debugger", "true"));
	}
	public void setDbDebugMode(boolean aFlag)
	{
		this.props.setProperty("workbench.db.debugger", Boolean.toString(aFlag));
	}

	public int getProfileDividerLocation()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.gui.profiles.divider", "-1"));
	}

	public void setProfileDividerLocation(int aValue)
	{
		this.props.setProperty("workbench.gui.profiles.divider", Integer.toString(aValue));
	}

	public void setProperty(String aClass, String aProperty, String aValue)
	{
		this.props.setProperty(aClass + "." + aProperty.toLowerCase(), aValue);
	}

	public void setProperty(String aClass, String aProperty, int aValue)
	{
		this.props.setProperty(aClass + "." + aProperty.toLowerCase(), Integer.toString(aValue));
	}

	public String getProperty(String aClass, String aProperty, String aDefault)
	{
		return this.props.getProperty(aClass + "." + aProperty.toLowerCase(), aDefault);
	}

	public int getIntProperty(String aClass, String aProperty, int aDefault)
	{
		String value = this.getProperty(aClass, aProperty, Integer.toString(aDefault));
		return StringUtil.getIntValue(value);
	}
	
	public int getIntProperty(String aClass, String aProperty)
	{
		String value = this.getProperty(aClass, aProperty, "0");
		return StringUtil.getIntValue(value);
	}

	public void setDbExplorerVisible(boolean aFlag)
	{
		this.props.setProperty("workbench.dbexplorer.visible", Boolean.toString(aFlag));
	}

	public boolean getDbExplorerVisible()
	{
		return "true".equals(this.props.getProperty("workbench.dbexplorer.visible", "false"));
	}

	public boolean getShowDbExplorerInMainWindow()
	{
		return "true".equalsIgnoreCase(this.props.getProperty("workbench.dbexplorer.mainwindow", "true"));
	}

	public void setShowDbExplorerInMainWindow(boolean showWindow)
	{
		this.props.setProperty("workbench.dbexplorer.mainwindow", Boolean.toString(showWindow));
	}

	public boolean getUseEncryption()
	{
		return "true".equalsIgnoreCase(this.props.getProperty("workbench.profiles.encryptpassword", "true"));
	}

	public void setUseEncryption(boolean useEncryption)
	{
		this.props.setProperty("workbench.profiles.encryptpassword", Boolean.toString(useEncryption));
	}

	public boolean getRetrieveDbExplorer()
	{
		return "true".equalsIgnoreCase(this.props.getProperty("workbench.dbexplorer.retrieveonopen", "false"));
	}

	public void setRetrieveDbExplorer(boolean aFlag)
	{
		this.props.setProperty("workbench.dbexplorer.retrieveonopen", Boolean.toString(aFlag));
	}

	public boolean getUseAnimatedIcon()
	{
		return "true".equalsIgnoreCase(this.props.getProperty(ANIMATED_ICONS_KEY, "true"));
	}
	
	public void setUseAnimatedIcon(boolean flag)
	{
		this.props.setProperty(ANIMATED_ICONS_KEY, Boolean.toString(flag));
	}
	
	public boolean getUseDynamicLayout()
	{
		return "true".equalsIgnoreCase(this.props.getProperty("workbench.gui.dynamiclayout", "false"));
	}

	public void setUseDynamicLayout(boolean useEncryption)
	{
		this.props.setProperty("workbench.gui.dynamiclayout", Boolean.toString(useEncryption));
	}

	public boolean getVerifyDriverUrl()
	{
		return "true".equalsIgnoreCase(this.props.getProperty("workbench.db.verifydriverurl", "true"));
	}
	public boolean getShowMnemonics()
	{
		return "true".equalsIgnoreCase(this.props.getProperty("workbench.gui.showmnemonics", "true"));
	}

	public boolean getShowSplash()
	{
		return "true".equalsIgnoreCase(this.props.getProperty("workbench.gui.showsplash", "false"));
	}

	public boolean getRetrievePKList()
	{
		return "true".equalsIgnoreCase(this.props.getProperty("workbench.db.retrievepklist", "true"));
	}

	public List getServersWhereDDLNeedsCommit()
	{
		String list = this.props.getProperty("workbench.db.ddlneedscommit", "PostgreSQL");
    return StringUtil.stringToList(list, ",");
	}
	
  public List getCaseSensitivServers()
  {
		String list = this.props.getProperty("workbench.db.casesensitive", null);
    return StringUtil.stringToList(list, ",");
  }

	public List getCancelWithReconnectServers()
	{
		String list = this.props.getProperty("workbench.db.cancelwithreconnect", null);
    return StringUtil.stringToList(list, ",");
	}

	public String getInstallDir()
	{
		CodeSource source = Settings.class.getProtectionDomain().getCodeSource();

		if (source == null) return null;

		File installDir;

		try
		{
			URI sourceURI = new URI(source.getLocation().toString());
			installDir = new File(sourceURI);
		}
		catch (URISyntaxException e)
		{
			return null;
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}

		if (!installDir.isDirectory())
		{
			installDir = installDir.getParentFile();
		}

		return installDir.getAbsolutePath();
	}

	public static void main(String args[])
	{
		try
		{
			DecimalFormat f = new DecimalFormat("#,#");
			Number n = f.parse("1");
			System.out.println(n.toString());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("done.");
	}

}