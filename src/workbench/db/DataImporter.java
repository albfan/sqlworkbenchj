/*
 * Created on 27. August 2002, 21:17
 */
package workbench.db;

import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;

import workbench.WbManager;
import workbench.exception.WbException;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.dbobjects.ProgressPanel;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 *
 * @author  workbench@kellerer.org
 */
public class DataImporter
	implements Interruptable
{
	public static final int IMPORT_XML = 1;
	public static final int IMPORT_TXT = 2;
	
	private WbConnection dbConn;
	private String sql;
	private String fullInputFileName;
	private String inputFile;
	
	private int importType;
	private String tableName;

	/** Needed for later Text import */
	private String delimiter = "\t";
	private String quoteChar = null;
	private String dateFormat = null;
	private String dateTimeFormat = null;
	private char decimalSymbol = '.';
	private String chrFunc = null;
	private String concatString = "||";

	private int commitEvery=0;
	
	private boolean showProgress = false;
	private ProgressPanel progressPanel;
	private JFrame progressWindow;
	private boolean keepRunning = true;

	private boolean success = false;
	private boolean hasWarning = false;

	private ArrayList warnings = new ArrayList();
	private ArrayList errors = new ArrayList();
	
	public DataImporter()
	{
	}

	/**
	 *	Open the progress monitor window.
	 */
	private void openProgressMonitor()
	{
		File f = new File(this.inputFile);
		String fname = f.getName();
		
		progressPanel = new ProgressPanel(this);
		this.progressPanel.setFilename(this.inputFile);
		this.progressPanel.setInfoText(ResourceMgr.getString("MsgImportingRecord"));
	
		this.progressWindow = new JFrame();
		this.progressWindow.getContentPane().add(progressPanel);
		this.progressWindow.pack();
		this.progressWindow.setTitle(ResourceMgr.getString("MsgImportWindowTitle"));
		this.progressWindow.setIconImage(ResourceMgr.getPicture("ImportData16").getImage());
		this.progressWindow.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				keepRunning = false;
			}
		});
		
		WbSwingUtilities.center(this.progressWindow, null);
		this.progressWindow.show();
	}

	public void setConnection(WbConnection aConn)
	{
		this.dbConn = aConn;
	}

	public void setTableName(String aTablename)
	{
		this.tableName = aTablename;
	}

	public void setCommitEvery(int aCount) { this.commitEvery = aCount; }
	public int getCommitEvery() { return this.commitEvery; }
	
	public void setShowProgress(boolean aFlag) { this.showProgress = aFlag; }
	public boolean getShowProgress() { return this.showProgress; }

	public void setTextDelimiter(String aDelimiter) { this.delimiter = aDelimiter; }
	public String getTextDelimiter() { return this.delimiter; }

	public void setTextQuoteChar(String aQuote) { this.quoteChar = aQuote; }
	public String getTextQuoteChar() { return this.quoteChar; }

	public void setTextDateFormat(String aFormat) { this.dateFormat = aFormat; }
	public String getTextDateFormat() { return this.dateFormat; }
	
	public void setTextTimestampFormat(String aFormat) { this.dateTimeFormat = aFormat; }
	public String getTextTimestampFormat() { return this.dateTimeFormat; }

	public void setInputTypeXml() { this.importType = IMPORT_XML; }
	public void setInputTypeText() { this.importType = IMPORT_TXT; }
	
	public boolean isInputTypeText() { return this.importType == IMPORT_TXT; }
	public boolean isInputTypeXml() { return this.importType == IMPORT_TXT; }
	
	public void setInputFilename(String aFilename) { this.inputFile = aFilename; }
	public String getInputFilename() { return this.inputFile; }
	public String getFullInputFilename() { return this.fullInputFileName; }
	
	public void setDecimalSymbol(char aSymbol)
	{
		this.decimalSymbol = aSymbol;
	}

	public void setDecimalSymbol(String aSymbol)
	{
		if (aSymbol == null || aSymbol.length() == 0) return;
		this.decimalSymbol = aSymbol.charAt(0);
	}
	
	private void startBackgroundThread()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try { startImport(); } catch (Throwable th) {}
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	public void startImport()
		throws IOException, SQLException, WbException
	{
		
	}
	
	public boolean isSuccess() { return this.errors.size() == 0; }
	public boolean hasWarning() { return this.warnings.size() > 0; }

	public String[] getErrors()
	{
		int count = this.errors.size();
		String[] result = new String[count];
		for (int i=0; i < count; i++)
		{
			result[i] = (String)this.errors.get(i);
		}
		return result;
	}
	
	public String[] getWarnings()
	{
		int count = this.warnings.size();
		String[] result = new String[count];
		for (int i=0; i < count; i++)
		{
			result[i] = (String)this.warnings.get(i);
		}
		return result;
	}
	
	public void closeProgress()
	{
		if (this.progressWindow != null)
		{
			this.progressWindow.hide();
			this.progressWindow.dispose();
			this.progressPanel = null;
		}
	}

	public void cancelExecution()
	{
		this.keepRunning = false;
	}

	public static void main(String[] args)
	{
	}
}
