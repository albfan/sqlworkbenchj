/* SQLLexer.java is a generated file.  You probably want to
 * edit SQLLexer.lex to make changes.  Use JFlex to generate it.
 * To generate SQLLexer.java
 * Install <a href="http://jflex.de/">JFlex</a> v1.3.2 or later.
 * Once JFlex is in your classpath run<br>
 * <code>java JFlex.Main SQLLexer.lex</code><br>
 * You will then have a file called SQLLexer.java
 */

/*
 * This file is part of a <a href="http://ostermiller.org/syntax/">syntax
 * highlighting</a> package.
 * Copyright (C) 2002 Stephen Ostermiller
 * http://ostermiller.org/contact.pl?regarding=Syntax+Highlighting
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * See COPYING.TXT for details.
 */

package workbench.sql.formatter;

import java.io.*;
import workbench.util.CharSequenceReader;

/**
 * SQLLexer is a SQL language lexer.  Created with JFlex.  An example of how it is used:
 *  <CODE>
 *  <PRE>
 *  SQLLexer shredder = new SQLLexer(System.in);
 *  SQLToken t;
 *  while ((t = shredder.getNextToken()) != null){
 *      System.out.println(t);
 *  }
 *  </PRE>
 *  </CODE>
 *
 * @see SQLToken
 */

%%

%public
%class SQLLexer
%function getNextToken
%type SQLToken
%line
%column
%char
%unicode
%ignorecase
%state COMMENT

%{
	private int lastToken;
	private int nextState=YYINITIAL;
	private StringBuilder commentBuffer = new StringBuilder();
	private int commentNestCount = 0;
	private int commentStartLine = 0;
	private int commentStartChar = 0;

	/**
	 * next Token method that allows you to control if whitespace and comments are
	 * returned as tokens.
	 */
	public SQLToken getNextToken(boolean returnComments, boolean returnWhiteSpace)
	{
		try
		{
			SQLToken t = getNextToken();
			while (t != null && ((!returnWhiteSpace && t.isWhiteSpace()) || (!returnComments && t.isComment())))
			{
				t = getNextToken();
			}
			return (t);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}


  /*
	public SQLToken getNextToken()
		throws IOException
  {
    SQLToken t = _getNextToken();
    if (t != null) t.setKeywordHelper(keywordHelper);
    return t;
  }
	*/

	/**
	 * Closes the current input stream, and resets the scanner to read from a new input stream.
	 * All internal variables are reset, the old input stream  cannot be reused
	 * (content of the internal buffer is discarded and lost).

	 * The lexical state is set to the initial state.
	 * Subsequent tokens read from the lexer will start with the line, char, and column
	 * values given here.
	 *
	 * @param reader The new input.
	 * @param yyline The line number of the first token.
	 * @param yychar The position (relative to the start of the stream) of the first token.
	 * @param yycolumn The position (relative to the line) of the first token.
	 * @throws IOException if an IOExecption occurs while switching readers.
	 */
	public void reset(java.io.Reader reader, int yyline, int yychar, int yycolumn)
		throws IOException
	{
		yyreset(reader);
		this.yyline = yyline;
		this.yychar = yychar;
		this.yycolumn = yycolumn;
	}

	public SQLLexer(String source)
	{
	  this(new StringReader(source));
	}

	public SQLLexer(CharSequence source)
	{
	  this(new CharSequenceReader(source));
	}
%}


wsp = [ \r\n\t\f]+

keyword=(
(ALTER{wsp}SESSION)|
(CONNECT{wsp}BY)|
(PRIMARY{wsp}KEY)|
(FOREIGN{wsp}KEY)|
(UNION{wsp}ALL)|
(PARTITION{wsp}BY)|
(GROUP{wsp}BY)|
(ORDER{wsp}BY)|
(PACKAGE{wsp}BODY)|
(CREATE{wsp}OR{wsp}REPLACE)|
(IS{wsp}NOT{wsp}NULL)|
(FLASHBACK{wsp}ARCHIVE)|
(MATERIALIZED{wsp}VIEW)|
(MATERIALIZED{wsp}VIEW{wsp}LOG)|
(START{wsp}WITH)|
(OUTER{wsp}JOIN)|
(CROSS{wsp}JOIN)|
(FULL{wsp}JOIN)|
(FULL{wsp}OUTER{wsp}JOIN)|
(INNER{wsp}JOIN)|
(LEFT{wsp}JOIN)|
(LEFT{wsp}OUTER{wsp}JOIN)|
(RIGHT{wsp}JOIN)|
(RIGHT{wsp}OUTER{wsp}JOIN)|
(NATURAL{wsp}JOIN)|
(IF{wsp}NOT{wsp}EXISTS)|
(IF{wsp}EXISTS)|
(CHARACTER{wsp}VARYING)|
(DISTINCT{wsp}ON)|
(PRIMARY{wsp}KEY)|
(SNAPSHOT{wsp}LOG)|
(START{wsp}WITH)|
"ABORT"|
"ABS"|
"ABSOLUTE"|
"ACCESS"|
"ACTION"|
"ADD"|
"ADMIN"|
"AFTER"|
"AGGREGATE"|
"ALIAS"|
"ALL"|
"ALLOCATE"|
"ALTER"|
"ANALYSE"|
"ANALYZE"|
"AND"|
"ANY"|
"ARE"|
"ARRAY"|
"AS"|
"ASC"|
"ASSERTION"|
"ASSIGNMENT"|
"ASYMMETRIC"|
"AT"|
"ATOMIC"|
"AUTHORIZATION"|
"AVG"|
"BACKWARD"|
"BEFORE"|
"BEGIN"|
"BETWEEN"|
"BIGINT"|
"BINARY"|
"BIT"|
"BITMAP"|
"BIT_LENGTH"|
"BITVAR"|
"BLOB"|
"BOOLEAN"|
"BOTH"|
"BREADTH"|
"CACHE"|
"CALL"|
"CALLED"|
"CARDINALITY"|
"CASCADE"|
"CASCADED"|
"CASE"|
"CAST"|
"CATALOG"|
"CATALOG_NAME"|
"CHAR"|
"CHAR_LENGTH"|
"CHARACTER"|
"CHARACTER_LENGTH"|
"CHARACTER_SET_CATALOG"|
"CHARACTER_SET_NAME"|
"CHARACTER_SET_SCHEMA"|
"CHARACTERISTICS"|
"CHECK"|
"CHECKED"|
"CHECKPOINT"|
"CLASS"|
"CLOB"|
"CLOSE"|
"CLUSTER"|
"COALESCE"|
"COBOL"|
"COLLATE"|
"COLLATION"|
"COLLATION_CATALOG"|
"COLLATION_NAME"|
"COLLATION_SCHEMA"|
"COLUMN"|
"COLUMN_NAME"|
"COMMAND_FUNCTION"|
"COMMAND_FUNCTION_CODE"|
"COMMENT"|
"COMMIT"|
"COMMITTED"|
"COMPLETION"|
"CONDITION_NUMBER"|
"CONNECT"|
"CONNECTION"|
"CONNECTION_NAME"|
"CONSTRAINT"|
"CONSTRAINT_CATALOG"|
"CONSTRAINT_NAME"|
"CONSTRAINT_SCHEMA"|
"CONSTRAINTS"|
"CONSTRUCTOR"|
"CONTAINS"|
"CONTINUE"|
"CONVERT"|
"COPY"|
"CORRESPONDING"|
"COUNT"|
"CREATE"|
"CREATEUSER"|
"CROSS"|
"CUBE"|
"CURRENT"|
"CURRENT_DATE"|
"CURRENT_PATH"|
"CURRENT_ROLE"|
"CURRENT_TIME"|
"CURRENT_TIMESTAMP"|
"CURRENT_USER"|
"CURSOR"|
"CURSOR_NAME"|
"CYCLE"|
"DATA"|
"DATABASE"|
"DATE"|
"DATETIME_INTERVAL_CODE"|
"DATETIME_INTERVAL_PRECISION"|
"DAY"|
"DEALLOCATE"|
"DEC"|
"DECIMAL"|
"DECLARE"|
"DEFAULT"|
"DEFERRABLE"|
"DEFERRED"|
"DEFINED"|
"DEFINER"|
"DELETE"|
"DELIMITER"|
"DELIMITERS"|
"DEPTH"|
"DEREF"|
"DENSE_RANK"|
"DESC"|
"DESCRIBE"|
"DESCRIPTOR"|
"DESTROY"|
"DESTRUCTOR"|
"DETERMINISTIC"|
"DIAGNOSTICS"|
"DICTIONARY"|
"DISABLEOUT"|
"DISCONNECT"|
"DISPATCH"|
"DISTINCT"|
"DO"|
"DOMAIN"|
"DOUBLE"|
"DROP"|
"DYNAMIC"|
"DYNAMIC_FUNCTION"|
"DYNAMIC_FUNCTION_CODE"|
"EACH"|
"ELSE"|
"ELSIF"|
"ENABLEOUT"|
"ENCODING"|
"ENCRYPTED"|
"END CASE"|
"END"|
"END-EXEC"|
"EQUALS"|
"ESCAPE"|
"EVERY"|
"EXCEPT"|
"EXCEPTION"|
"EXCLUSIVE"|
"EXEC"|
"EXECUTE"|
"EXISTING"|
"EXISTS"|
"EXPLAIN"|
"EXTERNAL"|
"EXTRACT"|
"FALSE"|
"FETCH"|
"FINAL"|
"FIRST"|
"FLASHBACK"|
"FLOAT"|
"FOR"|
"FORCE"|
"FORTRAN"|
"FORWARD"|
"FOUND"|
"FREE"|
"FREEZE"|
"FROM"|
"FUNCTION"|
"GENERAL"|
"GENERATED"|
"GET"|
"GLOBAL"|
"GO"|
"GOTO"|
"GRANT"|
"GRANTED"|
"GROUPING"|
"HANDLER"|
"HAVING"|
"HIERARCHY"|
"HOLD"|
"HOST"|
"HOUR"|
"IIF"|
"IDENTITY"|
"IGNORE"|
"ILIKE"|
"IMMEDIATE"|
"IMPLEMENTATION"|
"IN"|
"INCREMENT"|
"INDEX"|
"INDICATOR"|
"INFIX"|
"INHERITS"|
"INITIALIZE"|
"INITIALLY"|
"INOUT"|
"INPUT"|
"INSENSITIVE"|
"INSERT"|
"INSTANCE"|
"INSTANTIABLE"|
"INSTEAD"|
"INT"|
"INT4"|
"INT8"|
"INTEGER"|
"INTERSECT"|
"INTERSECT"|
"INTERVAL"|
"INTO"|
"INVOKER"|
"IS"|
"ISNULL"|
"ISOLATION"|
"ITERATE"|
"JOIN"|
"KEY"|
"KEY_MEMBER"|
"KEY_TYPE"|
"LANCOMPILER"|
"LANGUAGE"|
"LARGE"|
"LAST"|
"LATERAL"|
"LEADING"|
"LEFT"|
"LENGTH"|
"LESS"|
"LEVEL"|
"LIKE"|
"LIMIT"|
"LISTEN"|
"LOAD"|
"LOCAL"|
"LOCALTIME"|
"LOCALTIMESTAMP"|
"LOCATION"|
"LOCATOR"|
"LOCK"|
"LOWER"|
"MAP"|
"MATCH"|
"MAX"|
"MAXVALUE"|
"MERGE"|
"MESSAGE_LENGTH"|
"MESSAGE_OCTET_LENGTH"|
"MESSAGE_TEXT"|
"METHOD"|
"MIN"|
"MINUS"|
"MINUTE"|
"MINVALUE"|
"MOD"|
"MODE"|
"MODIFIES"|
"MODIFY"|
"MODULE"|
"MONTH"|
"MORE"|
"MOVE"|
"NAMES"|
"NATIONAL"|
"NCHAR"|
"NCLOB"|
"NEW"|
"NEXT"|
"NO"|
"NO CYCLE"|
"NOCREATEDB"|
"NOCREATEUSER"|
"NONE"|
"NOT"|
"NOTHING"|
"NOTIFY"|
"NOTNULL"|
"NOW"|
"NULL"|
"NULLS"|
"NULLABLE"|
"NULLIF"|
"NUMBER"|
"NUMERIC"|
"NVARCHAR"|
"OBJECT"|
"OCTET_LENGTH"|
"OF"|
"OFF"|
"OFFSET"|
"OIDS"|
"OLD"|
"ON"|
"ONLY"|
"OPEN"|
"OPERATION"|
"OPERATOR"|
"OPTION"|
"OPTIONS"|
"OR"|
"ORDINALITY"|
"ORGANIZATION"|
"OUT"|
"OUTPUT"|
"OVER"|
"OVERLAPS"|
"OVERLAY"|
"OVERRIDING"|
"OWNER"|
"PACKAGE"|
"PAD"|
"PARAMETER"|
"PARAMETER_MODE"|
"PARAMETER_NAME"|
"PARAMETER_ORDINAL_POSITION"|
"PARAMETER_SPECIFIC_CATALOG"|
"PARAMETER_SPECIFIC_NAME"|
"PARAMETER_SPECIFIC_SCHEMA"|
"PARAMETERS"|
"PARTIAL"|
"PASSWORD"|
"PATH"|
"PENDANT"|
"PLI"|
"POSITION"|
"POSTFIX"|
"PRECISION"|
"PREFIX"|
"PREORDER"|
"PREPARE"|
"PRESERVE"|
"PRIOR"|
"PRIVILEGES"|
"PROCEDURAL"|
"PROCEDURE"|
"PUBLIC"|
"READ"|
"READS"|
"REAL"|
"RECREATE"|
"RECURSIVE"|
"REF"|
"REFERENCES"|
"REFERENCING"|
"REFRESH"|
"REINDEX"|
"RELATIVE"|
"RENAME"|
"REPEATABLE"|
"REPLACE"|
"RESET"|
"RESTRICT"|
"RESULT"|
"RETURN"|
"RETURNED_LENGTH"|
"RETURNED_OCTET_LENGTH"|
"RETURNED_SQLSTATE"|
"RETURNS"|
"REVOKE"|
"RIGHT"|
"ROLLBACK"|
"ROLLUP"|
"ROUTINE"|
"ROW"|
"ROW_COUNT"|
"ROWS"|
"RULE"|
"SAVEPOINT"|
"SCALE"|
"SCHEMA"|
"SCHEMA_NAME"|
"SCOPE"|
"SCROLL"|
"SEARCH"|
"SECOND"|
"SECTION"|
"SECURITY"|
"SELECT"|
"SENSITIVE"|
"SEQUENCE"|
"SERIALIZABLE"|
"SERVER_NAME"|
"SESSION"|
"SESSION_USER"|
"SET"|
"SETOF"|
"SETS"|
"SHARE"|
"SHOW"|
"SIMILAR"|
"SIMPLE"|
"SIZE"|
"SMALLINT"|
"SNAPSHOT"|
"SOURCE"|
"SPACE"|
"SQL"|
"SQLCODE"|
"SQLERROR"|
"SQLEXCEPTION"|
"SQLSTATE"|
"SQLWARNING"|
"START"|
"STATEMENT"|
"STATIC"|
"STATISTICS"|
"STRUCTURE"|
"SUBSTRING"|
"SUM"|
"SYMMETRIC"|
"SYNONYM"|
"SYSTEM_USER"|
"SYSTIMESTAMP"|
"SYSDATE"|
"TABLE"|
"TEMPORARY"|
"THEN"|
"TIME"|
"TIMESTAMP"|
"TIMEZONE_HOUR"|
"TIMEZONE_MINUTE"|
"TO"|
"TODAY"|
"TRAILING"|
"TRANSACTION"|
"TRANSLATE"|
"TRIGGER"|
"TRIM"|
"TRUNCATE"|
"TRUE"|
"TRUSTED"|
"TYPE"|
"UNLISTEN"|
"UNION"|
"UNIQUE"|
"UNTIL"|
"UPDATE"|
"UPPER"|
"USAGE"|
"USER"|
"USING"|
"VACUUM"|
"VALID"|
"VALUES"|
"VARCHAR"|
"VARCHAR2"|
"VARYING"|
"VARIABLE"|
"VERBOSE"|
"VERSION"|
"VIEW"|
"WBCONFIRM"|
"WBCOPY"|
"WBDATADIFF"|
"WBDEFINEPK"|
"WBENDBATCH"|
"WBFEEDBACK"|
"WBEXPORT"|
"WBIMPORT"|
"WBINCLUDE"|
"WBLIST"|
"WBLISTDB"|
"WBLISTCAT"|
"WBLISTPKDEF"|
"WBLISTPROCS"|
"WBLOADPKMAP"|
"WBREPORT"|
"WBENABLEWARNINGS"|
"WBGREP"|
"WBSCHEMADIFF"|
"WBSELECTBLOB"|
"WBSTARTBATCH"|
"WBVARDEF"|
"WBVARDEFINE"|
"WBVARDELETE"|
"WBVARLIST"|
"WBXSLT"|
"WHEN"|
"WHENEVER"|
"WHERE"|
"WITH"|
"WITHOUT"|
"WORK"|
"WRITE"|
"YEAR"|
"ZONE"
)

whitespace=([ \r\n\t\f])
wbvar=(\$\[)(\&|\?)?[a-zA-Z]+(\])|(\$\{)(\&|\?)?[a-zA-Z]+(\})
identifier=([^ \"\r\n\t\f\+\-\*\/\<\>\=\~\!\#\%\^\&\'\~\?\(\)\]\,\;\:\.0-9][^ \r\n\t\f\+\-\*\/\<\>\=\~\!\%\^\&\'\"\~\?\(\)\[\,\;\:\*]*)|(\"[^\r\n\t\f\*\<\>\'\"\*]*\")
/* identifier=([^ \"\r\n\t\f\+\-\*\/\<\>\=\~\!\#\%\^\&\'\~\?\(\)\]\,\;\:\.0-9][^ \r\n\t\f\+\-\*\/\<\>\=\~\!\%\^\&\'\"\~\?\(\)\[\,\;\:\*]*)|(\"[^\r\n\t\f\+\*\<\>\!\%\^\&\'\"\?\(\)\;\:\*]*\") */
digit=([0-9])
digits=({digit}+)
separator=([\(\)\[\]\,\;\:\*])
operator=([\+\-\*\/\<\>\=\~\!\%\^\&\?]|"||"|"!="|"<>"|"*="|"=*"|"<="|">="|"(+)")
integer=({digits})
string=([\'](([^\r\n\']|[\\][\'])*)[\'])
bitstring=("B"[\']([01]+)[\'])
stringerror=([\'](([^\r\n\']|[\\][\'])*)[\r\n])
bitstringerror1=("B"[\']([^01\r\n]*)[\'])
bitstringerror2=("B"[\'](([^\r\n\']|[\\][\'])*)[\r\n])
floatpoint=(({digits}"."({digits}?)("E"[+-]{digits})?)|(({digits}?)"."{digits}("E"[+-]{digits})?)|({digits}"E"[+-]{digits}))
linecomment=("--"[^\r\n]*)
commentstart="/*"
commenttext=(([^\*\/]|([\*]+[^\*\/])|([\/]+[^\*\/]))*)
commentend=(([\*]*)"/")
%%

<YYINITIAL> {linecomment} {
    nextState = YYINITIAL;
    lastToken = SQLToken.COMMENT_END_OF_LINE;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {commentstart} {
    nextState = COMMENT;
    commentBuffer.setLength(0);
    commentBuffer.append(yytext());
    commentNestCount = 1;
    commentStartLine = yyline;
    commentStartChar = yychar;
    yybegin(nextState);
}

<COMMENT> {commentstart} {
    nextState = COMMENT;
    commentBuffer.append(yytext());
    commentNestCount++;
    yybegin(nextState);
}

<COMMENT> {commenttext} {
    nextState = COMMENT;
    commentBuffer.append(yytext());
    yybegin(nextState);
}

<COMMENT> {commentend} {
    commentNestCount--;
    commentBuffer.append(yytext());
    if (commentNestCount == 0)
    {
        nextState = YYINITIAL;
        lastToken = SQLToken.COMMENT_TRADITIONAL;
        SQLToken t = (new SQLToken(lastToken,commentBuffer.toString(),commentStartLine,commentStartChar,commentStartChar+commentBuffer.length(),nextState));
        yybegin(nextState);
        return(t);
    }
}

<COMMENT> <<EOF>> {
    nextState = YYINITIAL;
    lastToken = SQLToken.ERROR_UNCLOSED_COMMENT;
    SQLToken t = (new SQLToken(lastToken,commentBuffer.toString(),commentStartLine,commentStartChar,commentStartChar+commentBuffer.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {keyword} {
    nextState = YYINITIAL;
    lastToken = SQLToken.RESERVED_WORD;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {wbvar} {
    nextState = YYINITIAL;
    lastToken = SQLToken.WB_VAR;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {separator} {
    nextState = YYINITIAL;
    lastToken = SQLToken.SEPARATOR;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {operator} {
    nextState = YYINITIAL;
    lastToken = SQLToken.OPERATOR;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {bitstring} {
    nextState = YYINITIAL;
    lastToken = SQLToken.LITERAL_BIT_STRING;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {bitstringerror1} {
    nextState = YYINITIAL;
    lastToken = SQLToken.ERROR_UNCLOSED_BIT_STRING;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {bitstringerror2} {
    nextState = YYINITIAL;
    lastToken = SQLToken.ERROR_BAD_BIT_STRING;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {identifier} {
    nextState = YYINITIAL;
    lastToken = SQLToken.IDENTIFIER;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {integer} {
    nextState = YYINITIAL;
    lastToken = SQLToken.LITERAL_INTEGER;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {string} {
    nextState = YYINITIAL;
    lastToken = SQLToken.LITERAL_STRING;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}


<YYINITIAL> {stringerror} {
    nextState = YYINITIAL;
    lastToken = SQLToken.ERROR_UNCLOSED_STRING;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {floatpoint} {
    nextState = YYINITIAL;
    lastToken = SQLToken.LITERAL_FLOAT;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL> {whitespace}* {
    nextState = YYINITIAL;
    lastToken = SQLToken.WHITE_SPACE;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}

<YYINITIAL, COMMENT> [^] {
    nextState = YYINITIAL;
    lastToken = SQLToken.ERROR;
    String text = yytext();
    SQLToken t = (new SQLToken(lastToken,text,yyline,yychar,yychar+text.length(),nextState));
    yybegin(nextState);
    return(t);
}