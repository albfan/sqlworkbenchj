/*
 * Created on 27. August 2002, 21:17
 */
package workbench.db.importer;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import workbench.db.WbConnection;
import workbench.exception.ExceptionUtil;
import workbench.interfaces.Interruptable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.storage.RowActionMonitor;


/**
 *
 * @author  workbench@kellerer.org
 */
public class DataImporter
	implements Interruptable, RowDataReceiver
{
	public static final int IMPORT_XML = 1;
	public static final int IMPORT_TXT = 2;
	
	private WbConnection dbConn;
	private String sql;
	private String fullInputFileName;
	private String inputFile;
	private String encoding = "UTF-8";
	
	private int importType;
	private String tableName;

	private PreparedStatement updateStatement;
	
	/** Needed for Text import */
	private String delimiter = "\t";
	private String quoteChar = null;
	private String dateFormat = null;
	private String dateTimeFormat = null;
	private char decimalSymbol = '.';
	private String targetTable = null;

	private int commitEvery=0;
	private int colCount;
	
	private boolean keepRunning = true;

	private boolean success = false;
	private boolean hasWarning = false;
	private boolean textWithHeaders = false;

	private long totalRows = 0;
	private int currentImportRow = 0;
	
	private List columns;
	
	private ArrayList warnings = new ArrayList();
	private ArrayList errors = new ArrayList();
	private int[] columnTypes = null;
	
	private XmlDataFileParser xmlParser;
	private TextFileParser textParser;
	
	private RowActionMonitor progressMonitor;
	
	public DataImporter()
	{
	}

	public void setConnection(WbConnection aConn)
	{
		this.dbConn = aConn;
	}

	public void setTableName(String aTablename)
	{
		this.tableName = aTablename;
	}

	public void setEncoding(String anEncoding) 
	{ 
		if (anEncoding != null)
		{
			this.encoding = anEncoding; 
		}
	}
	
	public void setRowActionMonitor(RowActionMonitor rowMonitor)
	{
		this.progressMonitor = rowMonitor;
		if (this.progressMonitor != null)
		{
			this.progressMonitor.setMonitorType(RowActionMonitor.MONITOR_INSERT);
		}
	}
	
	public void setCommitEvery(int aCount) { this.commitEvery = aCount; }
	public int getCommitEvery() { return this.commitEvery; }
	
	public void setTargetTable(String tableName) { this.targetTable = tableName; }
	public String getTargetTable() { return this.targetTable; }

	/**
	 *	Define the columns for a text file import.
	 *	This is ignored when a an XML import is done.
	 */
	public void setTextFileColumns(List aColumnList) { this.columns = aColumnList; }
	
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
	
	public void startBackgroundImport()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try { startImport(); } catch (Throwable th) {}
			}
		};
		t.setDaemon(true);
		t.setName("Wb-Import Thread");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	public void startImport()
		throws IOException, SQLException, Exception
	{
		if (this.importType == IMPORT_XML)
		{
			this.importXml();
		}
		else
		{
			this.importText();
		}
	}
	
	private void importText()
		throws Exception
	{
		try
		{
			this.textParser = new TextFileParser(this.inputFile);
			this.textParser.setEncoding(this.encoding);
			this.textParser.setTableName(this.tableName);
			this.textParser.setConnection(this.dbConn);
			this.textParser.setContainsHeader(this.textWithHeaders);
			this.textParser.setDelimiter(this.delimiter);
			this.textParser.setQuoteChar(this.quoteChar);
			this.textParser.setColumns(this.columns);
			this.textParser.setDateFormat(this.dateFormat);
			this.textParser.setTimeStampFormat(this.dateTimeFormat);
			
			this.textParser.setReceiver(this);
			this.textParser.parse();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.importText()", "Error when importing", e);
			throw e;
		}
	}
	
	private void importXml()
		throws Exception
	{
		try
		{
			this.xmlParser = new XmlDataFileParser(this.inputFile);
			this.xmlParser.setEncoding(this.encoding);
			if (this.tableName != null)
			{
				this.xmlParser.setTableName(this.tableName);
			}
			this.xmlParser.setRowDataReceiver(this);
			this.xmlParser.parse();
		}
		catch (IOException e)
		{
			throw e;
		}
		catch (ParsingInterruptedException i)
		{
			LogMgr.logDebug("DataImporter.importXml()", "Import cancelled");
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
	
	public void cancelExecution()
	{
		this.keepRunning = false;
		if (this.xmlParser != null)
		{
			this.xmlParser.cancel();
		}
		else if (this.textParser != null)
		{
			this.textParser.cancel();
		}
		this.warnings.add(ResourceMgr.getString("MsgImportCancelled"));
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
			if (this.progressMonitor != null)
			{		
				progressMonitor.setCurrentRow(currentImportRow, -1);
			}

			for (int i=0; i < row.length; i++)
			{
				if (i > 0) values.append(",");
				if (row[i] == null)
				{
					this.updateStatement.setNull(i + 1, this.columnTypes[i]);
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
				LogMgr.logDebug("DataImporter.processRow()", "Committing changes (commitEvery=" + this.commitEvery + ")");
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
	 *	Callback function from the import file parser 
	 */
	public void setTargetTable(String tableName, String[] columns, int[] colTypes)
	{
		StringBuffer text = new StringBuffer(columns.length * 50);
		StringBuffer parms = new StringBuffer(columns.length * 20);
		
		text.append("INSERT INTO ");
		text.append(tableName);
		text.append(" (");
		this.colCount = columns.length;
		this.columnTypes = colTypes;
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
			this.updateStatement = this.dbConn.getSqlConnection().prepareStatement(this.sql);
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
			LogMgr.logDebug("DataImporter.importFinished()", "Committing changes");
			this.dbConn.commit();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.importFinished()", "Error commiting changes", e);
			this.errors.add(ExceptionUtil.getDisplay(e));
		}
	}	

	public void importCancelled()
	{
		try
		{
			LogMgr.logDebug("DataImporter.importCancelled()", "Rollback changes");
			this.dbConn.rollback();
		}
		catch (Exception e)
		{
			LogMgr.logError("DataImporter.importCancelled()", "Error on rollback", e);
			this.errors.add(ExceptionUtil.getDisplay(e));
		}
	}
	
}
