/*
 * Created on 27. August 2002, 21:17
 */
package workbench.db.importer;

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
import java.sql.PreparedStatement;
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
import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
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
	implements Interruptable, RowDataReceiver
{
	public static final int IMPORT_XML = 1;
	public static final int IMPORT_TXT = 2;
	
	private Connection dbConn;
	private String sql;
	private String fullInputFileName;
	private String inputFile;
	
	private int importType;
	private String tableName;

	private PreparedStatement updateStatement;
	
	/** Needed for later Text import */
	private String delimiter = "\t";
	private String quoteChar = null;
	private String dateFormat = null;
	private String dateTimeFormat = null;
	private char decimalSymbol = '.';
	private String chrFunc = null;
	private String concatString = "||";

	private int commitEvery=0;
	private int colCount;
	
	private boolean showProgress = false;
	private ProgressPanel progressPanel;
	private JFrame progressWindow;
	private boolean keepRunning = true;

	private boolean success = false;
	private boolean hasWarning = false;
	private boolean textWithHeaders = false;

	private long totalRows = 0;
	private int currentImportRow = 0;
	
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

	public void setConnection(Connection aConn)
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
	
	public void setTextContainsHeaders(boolean aFlag) { this.textWithHeaders = aFlag; }
	public boolean getTextContainsHeaders() { return this.textWithHeaders; }

	public void setImportTypeXml() { this.importType = IMPORT_XML; }
	public void setImportTypeText() { this.importType = IMPORT_TXT; }
	
	public boolean isImportTypeText() { return this.importType == IMPORT_TXT; }
	public boolean isImportTypeXml() { return this.importType == IMPORT_TXT; }
	
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
		throws IOException, SQLException, Exception
	{
		if (this.importType == this.IMPORT_XML)
		{
			this.importXml();
		}
	}
	
	private void importXml()
		throws Exception
	{
		try
		{
			XmlDataFileParser parser = new XmlDataFileParser(this.inputFile);
			parser.setRowDataReceiver(this);
			parser.parse();
		}
		catch (IOException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.importXml()", "Error when parsing the XML file", e);
			throw e;
		}
	}
	
	public boolean isSuccess() { return this.errors.size() == 0; }
	public boolean hasWarning() { return this.warnings.size() > 0; }
	public long getAffectedRow() { return this.totalRows; }

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

	/**
	 *	Callback function from the import file reader/parser 
	 */
	public void processRow(Object[] row) throws SQLException
	{
		if (row == null) return;
		if (row.length != this.colCount) return;
		StringBuffer values = new StringBuffer(row.length * 20);
		values.append("[");
		try
		{
			currentImportRow++;
			this.updateStatement.clearParameters();

			for (int i=0; i < row.length; i++)
			{
				if (i > 0) values.append(",");
				if (row[i] == null)
				{
					this.updateStatement.setNull(i + 1, Types.OTHER);
					values.append("NULL");
				}
				else
				{
					this.updateStatement.setObject(i + 1, row[i]);
					values.append(row[i].toString());
				}
			}
			values.append("]");
			int rows = this.updateStatement.executeUpdate();
			this.totalRows += rows;
		}
		catch (SQLException e)
		{
			LogMgr.logError("DataImporter.processRow()", "Error importing row " + this.totalRows, e);
			this.errors.add(ResourceMgr.getString("ErrorImportingRow") + " " + currentImportRow);
			this.errors.add(ResourceMgr.getString("ErrorImportErrorMsg") + " " + e.getMessage());
			this.errors.add(ResourceMgr.getString("ErrorImportValues") + " " + values);
			this.errors.add("");
			throw e;
		}
		if (this.commitEvery > 0 && ((this.totalRows % this.commitEvery) == 0))
		{
			try
			{
				this.dbConn.commit();
			}
			catch (SQLException e)
			{
				String error = ExceptionUtil.getDisplay(e);
				this.errors.add(error);
				throw e;
			}
		}
		
	}

	/**
	 *	Callback function from the import file reader/parser 
	 */
	public void setTargetTable(String tableName, String[] columns)
	{
		StringBuffer text = new StringBuffer(columns.length * 50);
		StringBuffer parms = new StringBuffer(columns.length * 20);
		
		text.append("INSERT INTO ");
		text.append(tableName);
		text.append(" (");
		this.colCount = columns.length;
		for (int i=0; i < columns.length; i++)
		{
			if (i > 0) 
			{
				text.append(",");
				parms.append(",");
			}
			text.append(columns[i]);
			parms.append('?');
		}
		text.append(") VALUES (");
		text.append(parms);
		text.append(")");
		try
		{
			this.sql = text.toString();
			this.updateStatement = this.dbConn.prepareStatement(this.sql);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.setTargetTable", "Error when creating SQL statement", e);
			this.updateStatement = null;
		}
	}
	
	public void importFinished()
	{
		try
		{
			this.dbConn.commit();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.importFinished()", "Error commiting changes", e);
		}
	}	

}
