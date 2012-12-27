/*
 * SequenceDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import workbench.storage.DataStore;
import workbench.util.CaseInsensitiveComparator;
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
	private String typeName = SequenceReader.DEFAULT_TYPE_NAME;

	private Map<String, Object> properties = new TreeMap<String, Object>(CaseInsensitiveComparator.INSTANCE);

	public SequenceDefinition(String seqSchema, String seqName)
	{
		sequenceName = seqName;
		schema = seqSchema;
	}

	public SequenceDefinition(String seqCatalog, String seqSchema, String seqName)
	{
		catalog = seqCatalog;
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

	@Override
	public CharSequence getSource(WbConnection con)
		throws SQLException
	{
		if (con == null) return null;
		SequenceReader reader = con.getMetadata().getSequenceReader();
		if (reader == null) return null;
		return reader.getSequenceSource(catalog, schema, sequenceName);
	}

	@Override
	public String getComment()
	{
		return comment;
	}

	@Override
	public void setComment(String cmt)
	{
		comment = cmt;
	}

	@Override
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

	@Override
	public String getObjectNameForDrop(WbConnection con)
	{
		return getFullyQualifiedName(con);
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(this.sequenceName);
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return SqlUtil.fullyQualifiedName(conn, this);
	}

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return SqlUtil.buildExpression(conn, catalog, schema, sequenceName);
	}

	@Override
	public String getObjectType()
	{
		return typeName;
	}

	public void setObjectTypeName(String name)
	{
		typeName = name;
	}

	@Override
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

	@Override
	public String toString()
	{
		return getSequenceName();
	}

}
