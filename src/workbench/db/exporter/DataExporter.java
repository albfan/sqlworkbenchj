/*
 * Created on 27. August 2002, 21:17
 */
package workbench.db.exporter;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.transform.TransformerException;

import workbench.WbManager;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.db.importer.RowDataProducer;
import workbench.db.importer.RowDataReceiver;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.dbobjects.ProgressPanel;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.XsltTransformer;


/**
 *
 * @author  workbench@kellerer.org
 */
public class DataExporter 
	implements Interruptable
{
	public static final int EXPORT_SQL = 1;
	public static final int EXPORT_TXT = 2;
	public static final int EXPORT_XML = 3;
	public static final int EXPORT_HTML = 4;

	private WbConnection dbConn;
	private String sql;
	private String htmlTitle = null;
	private String outputfile;
	private String fullOutputFileName;
	private String xsltFile = null;
	private String transformOutputFile = null;
	private int exportType;
	private boolean exportHeaders;
	private boolean includeCreateTable = false;
	private boolean headerOnly = false;
	private boolean useSqlUpdate = false;
	private String tableName;
	private String encoding = "UTF-8";

	private String delimiter = "\t";
	private String quoteChar = null;
	private String dateFormat = null;
	private String dateTimeFormat = null;
	private char decimalSymbol = '.';
	private String chrFunc = null;
	private String concatString = "||";
	private int commitEvery=0;

	private SimpleDateFormat dateFormatter = null;
	private	SimpleDateFormat dateTimeFormatter = null;
	private DecimalFormat numberFormatter = null;
	
	/** If true, then cr/lf characters will be removed from
	 *  character columns
	 */
	private boolean cleancr = false;
	private boolean append = false;
	private boolean escapeHtml = true;
	private boolean createFullHtmlPage = true;

	private boolean showProgress = false;
	private ProgressPanel progressPanel;
	private JFrame progressWindow;
	private boolean keepRunning = true;
	private boolean cancelJobs = false;
	private int pendingJobs = 0;
	private boolean jobsRunning = false;
	private RowActionMonitor rowMonitor;

	private ArrayList warnings = new ArrayList();
	private ArrayList errors = new ArrayList();
	private ArrayList jobQueue;

	public DataExporter()
	{
	}

	/**
	 *	Open the progress monitor window.
	 */
	private void openProgressMonitor()
	{
		File f = new File(this.outputfile);
		String fname = f.getName();

		progressPanel = new ProgressPanel(this);
		this.progressPanel.setFilename(this.outputfile);
		this.progressPanel.setInfoText(ResourceMgr.getString("MsgSpoolingRow"));

		this.progressWindow = new JFrame();
		this.progressWindow.getContentPane().add(progressPanel);
		this.progressWindow.pack();
		this.progressWindow.setTitle(ResourceMgr.getString("MsgSpoolWindowTitle"));
		this.progressWindow.setIconImage(ResourceMgr.getPicture("SpoolData16").getImage());
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

	public void addJob(String anOutputfile, String aStatement)
	{
		if (this.jobQueue == null)
		{
			this.jobQueue = new ArrayList();
		}
		JobEntry job = new DataExporter.JobEntry();
		job.outputFile = anOutputfile;
		job.sqlStatement = aStatement;
		this.jobQueue.add(job);
	}

	public void setConnection(WbConnection aConn)
	{
		this.dbConn = aConn;
	}

	public boolean confirmCancel()
	{
		if (!this.jobsRunning) return true;
		String msg = ResourceMgr.getString("MsgCancelAllCurrent");
		String current = ResourceMgr.getString("LabelCancelCurrentExport");
		String all = ResourceMgr.getString("LabelCancelAllExports");
		int answer = WbSwingUtilities.getYesNo(this.progressWindow, msg, new String[] { current, all });
		if (answer == 1)
		{
			this.cancelJobs = true;
		}
		return true;
	}

	public void cancelExecution()
	{
		this.keepRunning = false;
	}

	public void setTableName(String aTablename) { this.tableName = aTablename; }
	public String getTableName() { return this.tableName; }
	
	public void setEncoding(String enc) { this.encoding = enc; }
	public String getEncoding() { return this.encoding; }
	
	public void setRowMonitor(RowActionMonitor monitor)
	{
		this.rowMonitor = monitor;
		if (this.rowMonitor != null)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_EXPORT);
		}
	}

	public void setAppendToFile(boolean aFlag) { this.append = aFlag; }
	public boolean getAppendToFile() { return this.append; }
	
	public void setExportHeaderOnly(boolean aFlag) { this.headerOnly = aFlag; }
	public boolean getExportHeaderOnly() { return this.headerOnly; }
	
	public void setCommitEvery(int aCount) { this.commitEvery = aCount; }
	public int getCommitEvery() { return this.commitEvery; }

	public void setShowProgress(boolean aFlag)
	{
	  if (!WbManager.getInstance().isBatchMode())
	  {
	    this.showProgress = aFlag;
	  }
	}
	public boolean getShowProgress() { return this.showProgress; }

	public void setXsltTransformation(String xsltFileName)
	{
		this.xsltFile = xsltFileName;
	}
	public String getXsltTransformation() { return this.xsltFile; }
	
	public void setXsltTransformationOutput(String aFilename)
	{
		this.transformOutputFile = aFilename;
	}
	public String getXsltTransformationOutput() { return this.transformOutputFile; }
	
	public void setExportHeaders(boolean aFlag) { this.exportHeaders = aFlag; }
	public boolean getExportHeaders() { return this.exportHeaders; }

	public void setCreateFullHtmlPage(boolean aFlag) { this.createFullHtmlPage = aFlag; }
	public void setEscapeHtml(boolean aFlag) { this.escapeHtml = aFlag; }

	public void setTextDelimiter(String aDelimiter) { this.delimiter = aDelimiter; }
	public String getTextDelimiter() { return this.delimiter; }

	public void setTextQuoteChar(String aQuote) { this.quoteChar = aQuote; }
	public String getTextQuoteChar() { return this.quoteChar; }

	public void setDateFormat(String aFormat) 
	{ 
		this.dateFormat = aFormat; 
		if (this.dateFormat != null)
		{
			try
			{
				dateFormatter = new SimpleDateFormat(this.dateFormat);
			}
			catch (IllegalArgumentException i)
			{
				this.addWarning(ResourceMgr.getString("ErrorWrongDateFormat") + " " + this.dateFormat);
				dateFormatter = null;
			}
		}
	}
	
	public DateFormat getDateFormatter() 
	{
		return this.dateFormatter;
	}
	
	public String getDateFormat() 
	{ 
		return this.dateFormat; 
	}

	public void setTimestampFormat(String aFormat) 
	{ 
		this.dateTimeFormat = aFormat; 
		if (this.dateTimeFormat != null)
		{
			try
			{
				dateTimeFormatter = new SimpleDateFormat(this.dateTimeFormat);
			}
			catch (Exception e)
			{
				this.warnings.add(ResourceMgr.getString("ErrorWrongDateFormat") + " " + this.dateTimeFormat);
				dateTimeFormatter = null;
			}
		}
	}
	
	public String getTimestampFormat() { return this.dateTimeFormat; }
	public DateFormat getTimestampFormatter()
	{
		return this.dateTimeFormatter;
	}
	
	public void setHtmlTitle(String aTitle) { this.htmlTitle = aTitle; }
	public String getHtmlTitle() { return this.htmlTitle; }
	
	public void setOutputTypeHtml() { this.exportType = EXPORT_HTML; }
	public void setOutputTypeXml() { this.exportType = EXPORT_XML; }
	public void setOutputTypeText() { this.exportType = EXPORT_TXT; }
	
	public void setOutputTypeSqlInsert()
	{
		this.exportType = EXPORT_SQL;
		this.useSqlUpdate = false;
	}

	public void setOutputTypeSqlUpdate()
	{
		this.exportType = EXPORT_SQL;
		this.useSqlUpdate = true;
	}

	public void setOutputFilename(String aFilename) { this.outputfile = aFilename; }
	
	public String getOutputFilename() { return this.outputfile; }
	public String getFullOutputFilename() { return this.fullOutputFileName; }

	public void setCleanupCarriageReturns(boolean aFlag) { this.cleancr = aFlag; }
	public boolean getCleanupCarriageReturns() { return this.cleancr; }
	
	public void setConcatString(String aConcatString)
	{
		if (aConcatString == null) return;
		this.concatString = aConcatString;
	}
	public String getConcatString() { return this.concatString; }
	
	public void setChrFunction(String aFunc) { this.chrFunc = aFunc; }
	public String getChrFunction() { return this.chrFunc; }

	public void setDecimalSymbol(char aSymbol)
	{
		this.decimalSymbol = aSymbol;
		if (this.decimalSymbol != 0)
		{
			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setDecimalSeparator(this.decimalSymbol);
			numberFormatter = new DecimalFormat("0.#", symbols);
			numberFormatter.setGroupingUsed(false);
			numberFormatter.setMaximumFractionDigits(999);
		}
	}
	public DecimalFormat getDecimalFormatter() { return this.numberFormatter; }
	

	public void setDecimalSymbol(String aSymbol)
	{
		if (aSymbol == null || aSymbol.length() == 0) return;
		this.setDecimalSymbol(aSymbol.charAt(0));
	}
	
	public char getDecimalSymbol() { return this.decimalSymbol; }
	

	public void setSql(String aSql) { this.sql = aSql; }
	public String getSql() { return this.sql; }

	private void startBackgroundThread()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try { startExport(); } catch (Throwable th) {}
			}
		};
		t.setDaemon(true);
		t.setName("WbExport Thread");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	public void startExportJobs()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try { runJobs(); } catch (Throwable th) {}
			}
		};
		t.setDaemon(true);
		t.setName("WbExport Job Thread");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	private void runJobs()
	{
		if (this.jobQueue == null) return;
		int count = this.jobQueue.size();
		this.pendingJobs = count;
		this.jobsRunning = true;
		this.cancelJobs = false;
		for (int i=0; i < count; i++)
		{
			JobEntry job = (JobEntry)this.jobQueue.get(i);
			this.sql = job.sqlStatement;
			this.outputfile = job.outputFile;
			if (this.progressPanel != null)
			{
				this.progressPanel.setFilename(this.outputfile);
				this.progressPanel.setRowInfo(0);
			}
			try
			{
				this.startExport();
			}
			catch (Throwable th)
			{
				LogMgr.logError("DataSpooler.runJobs()", "Error spooling data for [" + this.sql + "] to file: " + this.outputfile, th);
			}
			this.pendingJobs --;
			if (this.cancelJobs) break;
		}
		this.pendingJobs = 0;
		this.jobsRunning = false;
		this.closeProgress();
	}

	public void setCurrentRow(int currentRow)
	{
		if (showProgress)
		{
			progressPanel.setRowInfo(currentRow);
		}
		if (this.rowMonitor != null)
		{
			this.rowMonitor.setCurrentRow(currentRow, -1);
		}
	}
	public void startExport()
		throws IOException, SQLException
	{
		Statement stmt = this.dbConn.createStatement();
		ResultSet rs = null;

		try
		{
			stmt.setFetchSize(1500);
		}
		catch (Exception e)
		{
		}

		try
		{
			stmt.execute(this.sql);
			rs = stmt.getResultSet();
			this.startExport(rs);
		}
		catch (Exception e)
		{
			LogMgr.logError("DataSpooler.startExport()", "Could not execute SQL statement: " + e.getMessage(), e);
			if (this.showProgress)
			{
				WbManager.getInstance().showErrorMessage(this.progressWindow, ResourceMgr.getString("MsgExecuteError") + ": " + e.getMessage());
			}
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { stmt.close(); } catch (Throwable th) {}
		}
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

	public void addWarning(String msg)
	{
		if (this.warnings == null) this.warnings = new ArrayList();
		this.warnings.add(msg);
	}
	
	public void addError(String msg)
	{
		if (this.errors == null) this.errors = new ArrayList();
		this.errors.add(msg);
	}
	
	/**
	 *	Export a table to an external file.
	 *	The data will be "piped" through a DataStore in order to use
	 *	the SQL scripting built into that object.
	 */
	public void startExport(ResultSet rs)
		throws IOException, SQLException, Exception
	{
		this.warnings.clear();
		this.errors.clear();

		ResultSetMetaData meta = rs.getMetaData();
		ResultInfo info = new ResultInfo(meta);
		ExportWriter exporter = null;
		
		switch (this.exportType)
		{
			case EXPORT_HTML:
				exporter = new HtmlExportWriter(this);
				break;
			case EXPORT_SQL:
				exporter = new SqlExportWriter(this);
				break;
			case EXPORT_TXT:
				exporter = new TextExportWriter(this);
				break;
			case EXPORT_XML:
				exporter = new XmlExportWriter(this);
		}

		if (this.showProgress)
		{
			if (this.progressPanel == null) this.openProgressMonitor();
		}

		BufferedWriter pw = null;

		try
		{
			File f = new File(this.outputfile);
			this.fullOutputFileName = f.getAbsolutePath();
			if (this.encoding != null)
			{
				OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(f), this.encoding);
				pw = new BufferedWriter(out);
			}
			else
			{
				pw = new BufferedWriter(new FileWriter(f,this.append), 16*1024);
			}
			exporter.writeExport(pw, rs, info);
		}
		catch (IOException e)
		{
			this.errors.add(e.getMessage());
			LogMgr.logError("DataSpooler", "Error writing data file", e);
			throw e;
		}
		catch (SQLException e)
		{
			this.errors.add(e.getMessage());
			LogMgr.logError("DataSpooler", "SQL Error", e);
			throw e;
		}
		finally
		{
			try { if (pw != null) pw.close(); } catch (Throwable th) {}
			try { rs.close(); } catch (Throwable th) {}
			if (!jobsRunning) this.closeProgress();
		}
		exporter.exportFinished();
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

	public void executeStatement(WbConnection aConnection, String aSql)
	{
		this.executeStatement(null, aConnection, aSql);
	}

	public void executeStatement(Window aParent, WbConnection aConnection, String aSql)
	{
		String cleanSql = SqlUtil.makeCleanSql(aSql, false);
		List tables = SqlUtil.getTables(cleanSql);
		boolean includeSqlExport = (tables.size() == 1);
		String tablename = null;
		if (includeSqlExport)
		{
			tablename = (String)tables.get(0);
		}
		String filename = WbManager.getInstance().getExportFilename(aParent, includeSqlExport);
		if (filename != null)
		{
			try
			{
				this.setConnection(aConnection);
				this.setOutputFilename(filename);
				this.setShowProgress(true);
				this.setSql(cleanSql);

				if (ExtensionFileFilter.hasSqlExtension(filename))
				{
					this.setOutputTypeSqlInsert();
					this.setTableName(tablename);
				}
				else if (ExtensionFileFilter.hasXmlExtension(filename))
				{
					this.setOutputTypeXml();
					this.setTableName(tablename);
				}
				else
				{
					this.setOutputTypeText();
				}
				this.startBackgroundThread();
			}
			catch (Exception e)
			{
				LogMgr.logError("DataSpoolThread", "Could not export data", e);
			}
		}
	}

	public boolean isIncludeCreateTable()
	{
		return includeCreateTable;
	}

	public void setIncludeCreateTable(boolean includeCreateTable)
	{
		this.includeCreateTable = includeCreateTable;
	}

	private class JobEntry
	{
		private String outputFile;
		private String sqlStatement;
	}

}