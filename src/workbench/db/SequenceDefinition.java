/*
 * SequenceDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class SequenceDefinition
	implements DbObject
{
	private String sequenceName;
	private String schema;
	private String catalog;
	private CharSequence source;
	private String comment;
	private TableIdentifier relatedTable;
	private String relatedColumn;
	
	private Map<String, Object> properties = new LinkedHashMap<String, Object>();
	
	public SequenceDefinition(String seqSchema, String seqName)
	{
		sequenceName = seqName;
		schema = seqSchema;
	}

	public void setRelatedTable(TableIdentifier table, String column)
	{
		relatedTable = table;
		relatedColumn = column;
	}

	public TableIdentifier getRelatedTable()
	{
		return relatedTable;
	}

	public String getRelatedColumn()
	{
		return relatedColumn;
	}
	
	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		if (con == null) return null;
		SequenceReader reader = con.getMetadata().getSequenceReader();
		if (reader == null) return null;
		return reader.getSequenceSource(catalog, schema, sequenceName);
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String cmt)
	{
		comment = cmt;
	}
	
	public String getSchema()
	{
		return schema;
	}
	
	public void setCatalog(String cat)
	{
		catalog = cat;
	}
	
	@Override
	public String getCatalog()
	{
		return catalog;
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		return null;
	}
	
	public String getObjectNameForDrop(WbConnection con)
	{
		return getObjectName(con);
	}

	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(this.sequenceName);
	}

	public String getFullyQualifiedName(WbConnection conn)
	{
		return getObjectExpression(conn);
	}

	public String getObjectExpression(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, sequenceName);
	}
	
	public String getObjectType()
	{
		return "SEQUENCE";
	}
	
	public String getObjectName()
	{
		return getSequenceName();
	}
	
	public String getSequenceName() 
	{
		return this.sequenceName;
	}
	
	public String getSequenceOwner()
	{
		return this.schema;
	}
	
	/**
	 * As each Database has a different set of attributes for a sequence
	 * these attributes are stored as key/value pairs to be as generic
	 * as possible
	 */
	public void setSequenceProperty(String property, Object value)
	{
		this.properties.put(property.toUpperCase().trim(), value);
	}
	
	public Object getSequenceProperty(String property)
	{
		return properties.get(property.toUpperCase().trim());
	}
	
	public CharSequence getSource()
	{
		return source;
	}
	
	public void setSource(CharSequence src)
	{
		source = src;
	}
	
	public Iterator<String> getProperties()
	{
		return this.properties.keySet().iterator();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof SequenceDefinition)
		{
			SequenceDefinition od = (SequenceDefinition)other;
			if (od.properties.size() != this.properties.size()) return false;
			
			for (Map.Entry<String, Object> entry : properties.entrySet())
			{
				Object ov = od.properties.get(entry.getKey());
				if (ov == null) return false;
				if (!ov.equals(entry.getValue())) return false; 
			}
			return true;
		}
		return false;
	}

	/**
	 * Converts the internally stored sequence properties into a
	 * DataStore with a single row and one column per property, similar
	 * to the display of a Sequence in the DbExplorer (obtained through
	 * {@link SequenceReader#getRawSequenceDefinition(java.lang.String, java.lang.String) }
	 * <br/>
	 * Note that some sequence readers might not put all retrieved attributes
	 * into the properties, so this is not necessarily the same as retrieving
	 * the raw definition directly from the sequence reader.
	 * 
	 * @return a DataStore with the sequence properties
	 */
	public DataStore getRawDefinition()
	{
		int count = this.properties.size() + 1;
		String[] colnames = new String[count];
		int[] types = new int[count];

		colnames[0] = "SEQUENCE_NAME";
		int i = 1;
		for (String name : properties.keySet())
		{
			colnames[i] = name;
			Object o = properties.get(name);
			if (o instanceof String)
			{
				types[i] = Types.VARCHAR;
			}
			else if (o instanceof Integer || o instanceof Long)
			{
				types[i] = Types.INTEGER;
			}
			else if (o instanceof Number)
			{
				types[i] = Types.DECIMAL;
			}
			i++;
		}
		DataStore ds = new DataStore(colnames, types);
		ds.addRow();
		
		i = 1;
		ds.setValue(0, 0, getSequenceName());
		for (String name : properties.keySet())
		{
			Object value = properties.get(name);
			ds.setValue(0, i, value);
			i++;
		}
		ds.resetStatus();
		
		return ds;
	}
	
	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 67 * hash + (this.properties != null ? this.properties.hashCode() : 0);
		return hash;
	}
	
}
