/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.mssql;

import workbench.db.DefaultViewReader;
import workbench.db.NoConfigException;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlServerViewReader
	extends DefaultViewReader
{

	public SqlServerViewReader(WbConnection con)
	{
		super(con);
	}

	@Override
	public CharSequence getViewSource(TableIdentifier viewId)
		throws NoConfigException
	{
		SpHelpTextRunner runner = new SpHelpTextRunner();
		CharSequence sql = runner.getSource(connection, viewId.getRawCatalog(), viewId.getRawSchema(), viewId.getRawTableName());
		if (!StringUtil.endsWith(sql, ';'))
		{
			StringBuilder full = new StringBuilder(sql.length() + 1);
			full.append(sql);
			full.append(';');
			return full;
		}
		return sql;
	}

}
