/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.mssql;

import workbench.db.DbMetadata;
import workbench.db.IndexDefinition;
import workbench.db.JdbcIndexReader;
import workbench.db.TableIdentifier;
import workbench.db.sqltemplates.TemplateHandler;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerIndexReader
	extends JdbcIndexReader
{
	public static final String CLUSTERED_PLACEHOLDER = "%clustered_attribute%";

	public SqlServerIndexReader(DbMetadata meta)
	{
		super(meta);
	}

	@Override
	protected String getUniqueConstraint(TableIdentifier table, IndexDefinition indexDefinition)
	{
		String sql = super.getUniqueConstraint(table, indexDefinition);
		return replaceClustered(sql, indexDefinition);
	}

	@Override
	public String getSQLKeywordForType(String type)
	{
		if (type == null) return "";
		if (type.equals("NORMAL")) return "NONCLUSTERED";
		return type;
	}
	
	private String replaceClustered(String sql, IndexDefinition indexDefinition)
	{
		if (StringUtil.isEmptyString(sql)) return sql;
		if (indexDefinition == null) return sql;

		String type = indexDefinition.getIndexType();
		String clustered = "CLUSTERED";
		if ("NORMAL".equals(type))
		{
			clustered = "NONCLUSTERED";
		}

		if (StringUtil.isBlank(type))
		{
			sql = TemplateHandler.removePlaceholder(sql, CLUSTERED_PLACEHOLDER, true);
		}
		else
		{
			sql = TemplateHandler.replacePlaceholder(sql, CLUSTERED_PLACEHOLDER, clustered);
		}
		return sql;
	}
}
