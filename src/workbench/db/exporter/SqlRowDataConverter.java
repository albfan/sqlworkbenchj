/*
 * SqlRowDataConverter.java
 *
 * Created on August 26, 2004, 10:54 PM
 */

package workbench.db.exporter;

import java.sql.Types;
import java.text.SimpleDateFormat;
import workbench.db.report.ReportColumn;
import workbench.db.report.ReportTable;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;
import workbench.storage.*;

/**
 *
 * @author  workbench@kellerer.org
 */
public class SqlRowDataConverter
	extends RowDataConverter
{
	// if this is false, we will generate update statements
	private boolean createInsert = true;
	private int commitEvery;
	private String concatString;
	private String chrFunction;
	private DataStore ds;
	
	public SqlRowDataConverter(ResultInfo info)
	{
		super(info);
	}
	
	public StrBuffer convertData()
	{
		return null;
	}

	public StrBuffer getEnd()
	{
		return null;
	}

	void init()
	{
		//this.ds = new DataStore( aConn)
	}
	
	public String getFormatName()
	{
		if (createInsert)
			return "SQL INSERT";
		else
			return "SQL UPDATE";
	}

	public StrBuffer convertRowData(RowData row, int rowIndex)
	{
		StrBuffer result = new StrBuffer();
		return result;
	}

	public StrBuffer getStart()
	{
		return null;
	}

	public boolean isCreateInsert()
	{
		return createInsert;
	}

	public void setCreateInsert(boolean createInsert)
	{
		this.createInsert = createInsert;
	}

	public int getCommitEvery()
	{
		return commitEvery;
	}

	public void setCommitEvery(int commitEvery)
	{
		this.commitEvery = commitEvery;
	}

	public String getConcatString()
	{
		return concatString;
	}

	public void setConcatString(String concatString)
	{
		this.concatString = concatString;
	}

	public String getChrFunction()
	{
		return chrFunction;
	}

	public void setChrFunction(String chrFunction)
	{
		this.chrFunction = chrFunction;
	}

}