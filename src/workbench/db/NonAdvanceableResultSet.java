/*
 * NonAdvanceableResultSet.java
 *
 * Created on October 4, 2002, 10:42 AM
 */

package workbench.db;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 *
 * @author  kellererth
 */
public class NonAdvanceableResultSet
	implements ResultSet
{
	private ResultSet result;

	public NonAdvanceableResultSet(ResultSet original)
	{
		this.result = original;
	}

	public boolean absolute(int row) throws SQLException
	{
		return false;
	}

	public void afterLast() throws SQLException
	{
	}

	public void beforeFirst() throws SQLException
	{
	}

	public void cancelRowUpdates() throws SQLException
	{
	}

	public void clearWarnings() throws SQLException
	{
		this.result.clearWarnings();
	}

	public void close() throws SQLException
	{
	}

	public void deleteRow() throws SQLException
	{
	}

	public int findColumn(String columnName) throws SQLException
	{
		return this.result.findColumn(columnName);
	}

	public boolean first() throws SQLException
	{
		return false;
	}

	public Array getArray(int i) throws SQLException
	{
		return this.result.getArray(i);
	}

	public Array getArray(String colName) throws SQLException
	{
		return this.getArray(colName);
	}

	public InputStream getAsciiStream(String columnName) throws SQLException
	{
		return this.result.getAsciiStream(columnName);
	}

	public InputStream getAsciiStream(int columnIndex) throws SQLException
	{
		return this.result.getAsciiStream(columnIndex);
	}

	public BigDecimal getBigDecimal(String columnName) throws SQLException
	{
		return this.result.getBigDecimal(columnName);
	}

	public BigDecimal getBigDecimal(int columnIndex) throws SQLException
	{
		return this.result.getBigDecimal(columnIndex);
	}

	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException
	{
		return this.result.getBigDecimal(columnIndex, scale);
	}

	public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException
	{
		return this.result.getBigDecimal(columnName, scale);
	}

	public InputStream getBinaryStream(int columnIndex) throws SQLException
	{
		return this.result.getBinaryStream(columnIndex);
	}

	public InputStream getBinaryStream(String columnName) throws SQLException
	{
		return this.result.getBinaryStream(columnName);
	}

	public Blob getBlob(int i) throws SQLException
	{
		return this.result.getBlob(i);
	}

	public Blob getBlob(String colName) throws SQLException
	{
		return this.result.getBlob(colName);
	}

	public boolean getBoolean(int columnIndex) throws SQLException
	{
		return this.result.getBoolean(columnIndex);
	}

	public boolean getBoolean(String columnName) throws SQLException
	{
		return this.result.getBoolean(columnName);
	}

	public byte getByte(int columnIndex) throws SQLException
	{
		return this.result.getByte(columnIndex);
	}

	public byte getByte(String columnName) throws SQLException
	{
		return this.result.getByte(columnName);
	}

	public byte[] getBytes(int columnIndex) throws SQLException
	{
		return this.result.getBytes(columnIndex);
	}

	public byte[] getBytes(String columnName) throws SQLException
	{
		return this.result.getBytes(columnName);
	}

	public Reader getCharacterStream(int columnIndex) throws SQLException
	{
		return this.result.getCharacterStream(columnIndex);
	}

	public Reader getCharacterStream(String columnName) throws SQLException
	{
		return this.result.getCharacterStream(columnName);
	}

	public Clob getClob(int i) throws SQLException
	{
		return this.result.getClob(i);
	}

	public Clob getClob(String colName) throws SQLException
	{
		return this.result.getClob(colName);
	}

	public int getConcurrency() throws SQLException
	{
		return this.result.getConcurrency();
	}

	public String getCursorName() throws SQLException
	{
		return this.result.getCursorName();
	}

	public Date getDate(int columnIndex) throws SQLException
	{
		return this.result.getDate(columnIndex);
	}

	public Date getDate(String columnName) throws SQLException
	{
		return this.result.getDate(columnName);
	}

	public Date getDate(int columnIndex, Calendar cal) throws SQLException
	{
		return this.result.getDate(columnIndex, cal);
	}

	public Date getDate(String columnName, Calendar cal) throws SQLException
	{
		return this.result.getDate(columnName, cal);
	}

	public double getDouble(int columnIndex) throws SQLException
	{
		return this.result.getDouble(columnIndex);
	}

	public double getDouble(String columnName) throws SQLException
	{
		return this.result.getDouble(columnName);
	}

	public int getFetchDirection() throws SQLException
	{
		return this.result.getFetchDirection();
	}

	public int getFetchSize() throws SQLException
	{
		return this.result.getFetchSize();
	}

	public float getFloat(int columnIndex) throws SQLException
	{
		return this.result.getFloat(columnIndex);
	}

	public float getFloat(String columnName) throws SQLException
	{
		return this.result.getFloat(columnName);
	}

	public int getInt(String columnName) throws SQLException
	{
		return this.result.getInt(columnName);
	}

	public int getInt(int columnIndex) throws SQLException
	{
		return this.result.getInt(columnIndex);
	}

	public long getLong(int columnIndex) throws SQLException
	{
		return this.result.getLong(columnIndex);
	}

	public long getLong(String columnName) throws SQLException
	{
		return this.result.getLong(columnName);
	}

	public ResultSetMetaData getMetaData() throws SQLException
	{
		return this.result.getMetaData();
	}

	public Object getObject(int columnIndex) throws SQLException
	{
		return this.result.getObject(columnIndex);
	}

	public Object getObject(String columnName) throws SQLException
	{
		return this.result.getObject(columnName);
	}

	public Object getObject(int i, Map map) throws SQLException
	{
		return this.result.getObject(i, map);
	}

	public Object getObject(String colName, Map map) throws SQLException
	{
		return this.result.getObject(colName, map);
	}

	public Ref getRef(int i) throws SQLException
	{
		return this.result.getRef(i);
	}

	public Ref getRef(String colName) throws SQLException
	{
		return this.result.getRef(colName);
	}

	public int getRow() throws SQLException
	{
		return this.result.getRow();
	}

	public short getShort(String columnName) throws SQLException
	{
		return this.result.getShort(columnName);
	}

	public short getShort(int columnIndex) throws SQLException
	{
		return this.result.getShort(columnIndex);
	}

	public Statement getStatement() throws SQLException
	{
		return result.getStatement();
	}

	public String getString(String columnName) throws SQLException
	{
		return result.getString(columnName);
	}

	public String getString(int columnIndex) throws SQLException
	{
		return result.getString(columnIndex);
	}

	public Time getTime(String columnName) throws SQLException
	{
		return result.getTime(columnName);
	}

	public Time getTime(int columnIndex) throws SQLException
	{
		return result.getTime(columnIndex);
	}

	public Time getTime(String columnName, Calendar cal) throws SQLException
	{
		return result.getTime(columnName, cal);
	}

	public Time getTime(int columnIndex, Calendar cal) throws SQLException
	{
		return result.getTime(columnIndex, cal);
	}

	public Timestamp getTimestamp(String columnName) throws SQLException
	{
		return result.getTimestamp(columnName);
	}

	public Timestamp getTimestamp(int columnIndex) throws SQLException
	{
		return result.getTimestamp(columnIndex);
	}

	public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException
	{
		return result.getTimestamp(columnName, cal);
	}

	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException
	{
		return result.getTimestamp(columnIndex, cal);
	}

	public int getType() throws SQLException
	{
		return result.getType();
	}

	public URL getURL(int columnIndex) throws SQLException
	{
		return result.getURL(columnIndex);
	}

	public URL getURL(String columnName) throws SQLException
	{
		return result.getURL(columnName);
	}

	public InputStream getUnicodeStream(String columnName) throws SQLException
	{
		return result.getUnicodeStream(columnName);
	}

	public InputStream getUnicodeStream(int columnIndex) throws SQLException
	{
		return result.getUnicodeStream(columnIndex);
	}

	public SQLWarning getWarnings() throws SQLException
	{
		return result.getWarnings();
	}

	public void insertRow() throws SQLException
	{
	}

	public boolean isAfterLast() throws SQLException
	{
		return result.isAfterLast();
	}

	public boolean isBeforeFirst() throws SQLException
	{
		return result.isBeforeFirst();
	}
	
	public boolean isFirst() throws SQLException
	{
		return result.isFirst();
	}

	public boolean isLast() throws SQLException
	{
		return result.isLast();
	}

	public boolean last() throws SQLException
	{
		return false;
	}

	public void moveToCurrentRow() throws SQLException
	{
	}

	public void moveToInsertRow() throws SQLException
	{
	}

	public boolean next() throws SQLException
	{
		return false;
	}

	public boolean previous() throws SQLException
	{
		return false;
	}

	public void refreshRow() throws SQLException
	{
		result.refreshRow();
	}

	public boolean relative(int rows) throws SQLException
	{
		return false;
	}

	public boolean rowDeleted() throws SQLException
	{
		return result.rowDeleted();
	}

	public boolean rowInserted() throws SQLException
	{
		return result.rowInserted();
	}

	public boolean rowUpdated() throws SQLException
	{
		return result.rowUpdated();
	}

	public void setFetchDirection(int direction) throws SQLException
	{
	}

	public void setFetchSize(int rows) throws SQLException
	{
	}

	public void updateArray(String columnName, Array x) throws SQLException
	{
	}

	public void updateArray(int columnIndex, Array x) throws SQLException
	{
	}

	public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException
	{
	}

	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException
	{
	}

	public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException
	{
	}

	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException
	{
	}

	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException
	{
	}

	public void updateBinaryStream(String columnName, InputStream x, int length) throws SQLException
	{
	}

	public void updateBlob(int columnIndex, Blob x) throws SQLException
	{
	}

	public void updateBlob(String columnName, Blob x) throws SQLException
	{
	}

	public void updateBoolean(int columnIndex, boolean x) throws SQLException
	{
	}

	public void updateBoolean(String columnName, boolean x) throws SQLException
	{
	}

	public void updateByte(int columnIndex, byte x) throws SQLException
	{
	}

	public void updateByte(String columnName, byte x) throws SQLException
	{
	}

	public void updateBytes(int columnIndex, byte[] x) throws SQLException
	{
	}

	public void updateBytes(String columnName, byte[] x) throws SQLException
	{
	}

	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException
	{
	}

	public void updateCharacterStream(String columnName, Reader reader, int length) throws SQLException
	{
	}

	public void updateClob(String columnName, Clob x) throws SQLException
	{
	}

	public void updateClob(int columnIndex, Clob x) throws SQLException
	{
	}

	public void updateDate(int columnIndex, Date x) throws SQLException
	{
	}

	public void updateDate(String columnName, Date x) throws SQLException
	{
	}

	public void updateDouble(int columnIndex, double x) throws SQLException
	{
	}

	public void updateDouble(String columnName, double x) throws SQLException
	{
	}

	public void updateFloat(String columnName, float x) throws SQLException
	{
	}

	public void updateFloat(int columnIndex, float x) throws SQLException
	{
	}

	public void updateInt(String columnName, int x) throws SQLException
	{
	}

	public void updateInt(int columnIndex, int x) throws SQLException
	{
	}

	public void updateLong(int columnIndex, long x) throws SQLException
	{
	}

	public void updateLong(String columnName, long x) throws SQLException
	{
	}

	public void updateNull(String columnName) throws SQLException
	{
	}

	public void updateNull(int columnIndex) throws SQLException
	{
	}

	public void updateObject(String columnName, Object x) throws SQLException
	{
	}

	public void updateObject(int columnIndex, Object x) throws SQLException
	{
	}

	public void updateObject(int columnIndex, Object x, int scale) throws SQLException
	{
	}

	public void updateObject(String columnName, Object x, int scale) throws SQLException
	{
	}

	public void updateRef(int columnIndex, Ref x) throws SQLException
	{
	}

	public void updateRef(String columnName, Ref x) throws SQLException
	{
	}

	public void updateRow() throws SQLException
	{
	}

	public void updateShort(int columnIndex, short x) throws SQLException
	{
	}

	public void updateShort(String columnName, short x) throws SQLException
	{
	}

	public void updateString(int columnIndex, String x) throws SQLException
	{
	}

	public void updateString(String columnName, String x) throws SQLException
	{
	}

	public void updateTime(String columnName, Time x) throws SQLException
	{
	}

	public void updateTime(int columnIndex, Time x) throws SQLException
	{
	}
	
	public void updateTimestamp(String columnName, Timestamp x) throws SQLException
	{
	}

	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException
	{
	}

	public boolean wasNull() throws SQLException
	{
		return result.wasNull();
	}

}
