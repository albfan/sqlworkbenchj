/*
 * HtmlUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.util;

/**
 * @author Thomas Kellerer
 */
public class HtmlUtil
{

  public static String escapeXML(CharSequence s)
  {
    return escapeXML(s, true);
  }

  @SuppressWarnings("fallthrough")
  public static String escapeXML(CharSequence s, boolean replaceSingleQuotes)
  {
    if (s == null) return StringUtil.EMPTY_STRING;
    StringBuilder sb = new StringBuilder(s.length() + 100);
    int n = s.length();
    for (int i = 0; i < n; i++)
    {
      char c = s.charAt(i);
      switch (c)
      {
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        case '&':
          sb.append("&amp;");
          break;
        case '"':
          sb.append("&quot;");
          break;
        case '\'':
          if (replaceSingleQuotes)
          {
            sb.append("&apos;");
            break;
          }
          // single quotes should not be replaced
          // in that case the fall through to the default is intended
        default:
          sb.append(c);
          break;
      }
    }
    return sb.toString();
  }

  public static String escapeHTML(String s)
  {
    if (s == null) return null;
    StringBuilder sb = new StringBuilder(s.length() + 100);
    int n = s.length();
    for (int i = 0; i < n; i++)
    {
      char c = s.charAt(i);
      switch (c)
      {
        case '<': sb.append("&lt;"); break;
        case '>': sb.append("&gt;"); break;
        case '&': sb.append("&amp;"); break;
        case '"': sb.append("&quot;"); break;
        case '\'': sb.append("&apos;"); break;
        case '\u00e0': sb.append("&agrave;");break;
        case '\u00c0': sb.append("&Agrave;");break;
        case '\u00e2': sb.append("&acirc;");break;
        case '\u00c2': sb.append("&Acirc;");break;
        case '\u00e4': sb.append("&auml;");break;
        case '\u00c4': sb.append("&Auml;");break;
        case '\u00e5': sb.append("&aring;");break;
        case '\u00c5': sb.append("&Aring;");break;
        case '\u00e6': sb.append("&aelig;");break;
        case '\u00c6': sb.append("&AElig;");break;
        case '\u00e7': sb.append("&ccedil;");break;
        case '\u00c7': sb.append("&Ccedil;");break;
        case '\u00e9': sb.append("&eacute;");break;
        case '\u00c9': sb.append("&Eacute;");break;
        case '\u00e8': sb.append("&egrave;");break;
        case '\u00c8': sb.append("&Egrave;");break;
        case '\u00ea': sb.append("&ecirc;");break;
        case '\u00ca': sb.append("&Ecirc;");break;
        case '\u00eb': sb.append("&euml;");break;
        case '\u00cb': sb.append("&Euml;");break;
        case '\u00ef': sb.append("&iuml;");break;
        case '\u00cf': sb.append("&Iuml;");break;
        case '\u00f4': sb.append("&ocirc;");break;
        case '\u00d4': sb.append("&Ocirc;");break;
        case '\u00f6': sb.append("&ouml;");break;
        case '\u00d6': sb.append("&Ouml;");break;
        case '\u00f8': sb.append("&oslash;");break;
        case '\u00d8': sb.append("&Oslash;");break;
        case '\u00df': sb.append("&szlig;");break;
        case '\u00f9': sb.append("&ugrave;");break;
        case '\u00d9': sb.append("&Ugrave;");break;
        case '\u00fb': sb.append("&ucirc;");break;
        case '\u00db': sb.append("&Ucirc;");break;
        case '\u00fc': sb.append("&uuml;");break;
        case '\u00dc': sb.append("&Uuml;");break;
        case '\u00ae': sb.append("&reg;");break;
        case '\u00a9': sb.append("&copy;");break;
        case '\u20ac': sb.append("&euro;"); break;

        default:  sb.append(c); break;
      }
    }
    return sb.toString();
  }

  public static String unescapeHTML(String s)
  {
    String [][] escape =
    {
      {  "&lt;"     , "<" } ,
      {  "&gt;"     , ">" } ,
      {  "&amp;"    , "&" } ,
      {  "&quot;"   , "\"" } ,
      {  "&agrave;" , "\u00e0" } ,
      {  "&Agrave;" , "\u00c0" } ,
      {  "&acirc;"  , "\u00e2" } ,
      {  "&auml;"   , "\u00e4" } ,
      {  "&Auml;"   , "\u00c4" } ,
      {  "&Acirc;"  , "\u00c2" } ,
      {  "&aring;"  , "\u00e5" } ,
      {  "&Aring;"  , "\u00c5" } ,
      {  "&aelig;"  , "\u00e6" } ,
      {  "&AElig;"  , "\u00c6" } ,
      {  "&ccedil;" , "\u00e7" } ,
      {  "&Ccedil;" , "\u00c7" } ,
      {  "&eacute;" , "\u00e9" } ,
      {  "&Eacute;" , "\u00c9" } ,
      {  "&egrave;" , "\u00e8" } ,
      {  "&Egrave;" , "\u00c8" } ,
      {  "&ecirc;"  , "\u00ea" } ,
      {  "&Ecirc;"  , "\u00ca" } ,
      {  "&euml;"   , "\u00eb" } ,
      {  "&Euml;"   , "\u00cb" } ,
      {  "&iuml;"   , "\u00ef" } ,
      {  "&Iuml;"   , "\u00cf" } ,
      {  "&ocirc;"  , "\u00f4" } ,
      {  "&Ocirc;"  , "\u00d4" } ,
      {  "&ouml;"   , "\u00f6" } ,
      {  "&Ouml;"   , "\u00d6" } ,
      {  "&oslash;" , "\u00f8" } ,
      {  "&Oslash;" , "\u00d8" } ,
      {  "&szlig;"  , "\u00df" } ,
      {  "&ugrave;" , "\u00f9" } ,
      {  "&Ugrave;" , "\u00d9" } ,
      {  "&ucirc;"  , "\u00fb" } ,
      {  "&Ucirc;"  , "\u00db" } ,
      {  "&uuml;"   , "\u00fc" } ,
      {  "&Uuml;"   , "\u00dc" } ,
      {  "&nbsp;"   , " " } ,
      {  "&reg;"    , "\u00a9" } ,
      {  "&copy;"   , "\u00ae" } ,
      {  "&euro;"   , "\u20a0" }
    };

    int i, j, k;

    i = s.indexOf('&');
    if (i > -1)
    {
      j = s.indexOf(';');
      if (j > i)
      {
        String temp = s.substring(i , j + 1);
        // search in escape[][] if temp is there
        k = 0;
        while (k < escape.length)
        {
          if (escape[k][0].equals(temp)) break;
          else k++;
        }
        s = s.substring(0 , i) + escape[k][1] + s.substring(j + 1);
        return unescapeHTML(s); // recursive call
      }
    }
    return s;
  }

  public static String cleanHTML(String input)
  {
    if (input == null) return input;
    return input.replaceAll("\\<.*?\\>", "");
  }

  public static String convertToMultiline(String orig)
  {
    return orig.replaceAll(StringUtil.REGEX_CRLF, "<br>");
  }

}
