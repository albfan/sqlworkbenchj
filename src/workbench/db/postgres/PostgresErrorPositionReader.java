/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.postgres;

import workbench.db.ErrorPositionReader;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresErrorPositionReader
	implements ErrorPositionReader
{

	@Override
	public int getErrorPosition(WbConnection con, String sql, Exception ex)
	{
		if (ex == null) return -1;
		String msg = ex.getMessage();
		int errorPos = -1;

		// TODO: do we need to localize this?
		String keyword = "Position:";

		int pos = msg.indexOf(keyword);
		if (pos > -1)
		{
			msg = msg.substring(pos + keyword.length());
			errorPos = StringUtil.getIntValue(msg.replaceAll("[^0-9]", ""), -1);
		}
		return errorPos;
	}

	@Override
	public String enhanceErrorMessage(String sql, String originalMessage, int errorPosition)
	{
		String indicator = SqlUtil.getErrorIndicator(sql, errorPosition-1);
		if (indicator != null)
		{
			originalMessage += "\n\n" + indicator;
		}
		return originalMessage;
	}

}
