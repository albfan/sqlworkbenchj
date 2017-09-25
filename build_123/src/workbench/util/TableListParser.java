/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;
import workbench.sql.parser.ParserType;


/**
 *
 * @author Thomas Kellerer
 */
public class TableListParser
{
  private char catalogSeparator = '.';
  private char schemaSeparator = '.';
  private ParserType parserType = ParserType.Standard;

  public TableListParser()
  {
  }

  public TableListParser(ParserType type)
  {
    this('.', '.', type);
  }

  public TableListParser(char catSep, char schemaSep, ParserType type)
  {
    parserType = type;
    catalogSeparator = catSep;
    schemaSeparator = schemaSep;
  }

  public List<Alias> getTables(String sql, boolean includeAlias)
  {
    SQLLexer lexer = SQLLexerFactory.createLexer(parserType, "");

    String fromPart = SqlParsingUtil.getFromPart(sql, lexer);
    if (StringUtil.isBlank(fromPart)) return Collections.emptyList();
    List<Alias> result = new ArrayList<>();

    try
    {
      lexer.setInput(fromPart);
      SQLToken t = lexer.getNextToken(false, false);

      boolean collectTable = true;
      int bracketCount = 0;
      boolean subSelect = false;
      int subSelectBracketCount = -1;

      while (t != null)
      {
        String s = t.getContents();

        if (s.equals("SELECT") && bracketCount > 0)
        {
          subSelect = true;
          subSelectBracketCount = bracketCount;
        }

        if ("(".equals(s))
        {
          bracketCount ++;
        }
        else if (")".equals(s))
        {
          if (subSelect && bracketCount == subSelectBracketCount)
          {
            subSelect = false;
          }
          bracketCount --;
          t = lexer.getNextToken(false, false);

          // An AS keyword right after a closing ) means this introduces the
          // alias for the derived table. We can skip this token
          if (t != null && t.getContents().equals("AS"))
          {
            t = lexer.getNextToken(false, false);
            collectTable = true;
          }
          else
          {
            collectTable = bracketCount == 0;
          }
          continue;
        }

        if (!subSelect)
        {
          if (SqlUtil.getJoinKeyWords().contains(s))
          {
            collectTable = true;
          }
          else if (",".equals(s))
          {
            collectTable = true;
          }
          else if ("ON".equals(s) || "USING".equals(s))
          {
            collectTable = false;
          }
          else if (collectTable && !s.equals("("))
          {
            collectTable = false;
            Alias table = new Alias();

            t = collectToWhiteSpace(lexer, t, table);
            if (t != null && t.isWhiteSpace())
            {
              t = lexer.getNextToken(false, false);
            }

            if (t != null && t.getContents().equals("AS"))
            {
              table.setAsKeyword(t.getText());
              // the next item must be the alias
              t = lexer.getNextToken(false, false);
              table.setAlias(t != null ? t.getText() : null);
              result.add(table);
            }
            else if (t != null && t.isIdentifier())
            {
              table.setAlias(t.getText());
              result.add(table);
            }
            else
            {
              result.add(table);
              continue;
            }
          }
        }
        t = lexer.getNextToken(false, false);
      }

      if (!includeAlias)
      {
        for (Alias a : result)
        {
          a.setAlias(null);
        }
      }
    }
    catch (Exception e)
    {
      LogMgr.logError("SqlUtil.getTable()", "Error parsing sql", e);
    }
    return result;
  }

  private SQLToken collectToWhiteSpace(SQLLexer lexer, SQLToken current, Alias table)
  {
    if (!current.isWhiteSpace())
    {
      table.appendObjectName(current.getText());
    }
    SQLToken token = lexer.getNextToken(false, true);
    while (token != null)
    {
      String text = token.getContents();
      if (!isSeparator(text) && (token.isWhiteSpace() || token.isOperator() || token.isReservedWord() || text.equals(",")))
      {
        break;
      }
      table.appendObjectName(token.getText());
      token = lexer.getNextToken(false, true);
    }
    return token;
  }

  private boolean isSeparator(String text)
  {
    if (text == null || text.isEmpty()) return false;
    return text.charAt(0) == schemaSeparator || text.charAt(0) == catalogSeparator;
  }
}
