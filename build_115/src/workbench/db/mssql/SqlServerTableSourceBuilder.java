/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.mssql;

import workbench.db.PkDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;
import static workbench.db.mssql.SqlServerIndexReader.CLUSTERED_PLACEHOLDER;
import workbench.db.sqltemplates.TemplateHandler;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerTableSourceBuilder
	extends TableSourceBuilder
{

	public SqlServerTableSourceBuilder(WbConnection con)
	{
		super(con);
	}

	@Override
	public CharSequence getPkSource(TableIdentifier table, PkDefinition pk, boolean forInlineUse)
	{
		CharSequence pkSource = super.getPkSource(table, pk, forInlineUse);
		if (StringUtil.isEmptyString(pkSource) || forInlineUse)
		{
			return pkSource;
		}
		String sql = pkSource.toString();

		String type = pk.getIndexType();
		String clustered = "CLUSTERED";
		if ("NORMAL".equals(type))
		{
			clustered = "NONCLUSTERED";
		}

		if (StringUtil.isBlank(clustered))
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
