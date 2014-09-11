/*
 * StringUtilTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.util;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class StringUtilTest
{

	@Test
	public void testGetFirstWord()
	{
		assertEquals("foo", StringUtil.getFirstWord("foo bar"));
		assertEquals("foobar", StringUtil.getFirstWord("foobar"));
		assertEquals("foobar", StringUtil.getFirstWord(" foobar"));
		assertEquals("foo", StringUtil.getFirstWord(" foo bar"));
	}

	@Test
	public void testRemove()
	{
		StringBuilder b = new StringBuilder("12345");
		StringUtil.removeFromEnd(b, 2);
		assertEquals("123", b.toString());

		b = new StringBuilder("12");
		StringUtil.removeFromEnd(b, 2);
		assertTrue(b.length() == 0);

		b = new StringBuilder("123");
		StringUtil.removeFromEnd(b, 5);
		assertTrue(b.length() == 0);
	}

	@Test
	public void testWildcardRegex()
	{
		String pattern = StringUtil.wildcardToRegex("*.sql", false);
		assertEquals("^.*\\.sql$", pattern);

		pattern = StringUtil.wildcardToRegex("*.sql*", false);
		assertEquals("^.*\\.sql.*$", pattern);

		pattern = StringUtil.wildcardToRegex("foo%20*.txt", false);
		assertEquals("^foo%20.*\\.txt$", pattern);

		pattern = StringUtil.wildcardToRegex("foo%", true);
		assertEquals("^foo.*$", pattern);
	}

	@Test
	public void testFormatString()
	{
		assertEquals("0001", StringUtil.formatInt(1, 4).toString());
		assertEquals("0123", StringUtil.formatInt(123, 4).toString());
		assertEquals("123", StringUtil.formatInt(123, 3).toString());
		assertEquals("123", StringUtil.formatInt(123, 1).toString());
		assertEquals("-01", StringUtil.formatInt(-1, 2).toString());
	}

	@Test
	public void testHexString()
	{
		String hex = StringUtil.hexString(4, 2);
		assertEquals("04", hex);

		hex = StringUtil.hexString(4, 3);
		assertEquals("004", hex);

		hex = StringUtil.hexString(16, 2);
		assertEquals("10", hex);

		hex = StringUtil.hexString(256, 2);
		assertEquals("100", hex);

		hex = StringUtil.hexString(256, 4);
		assertEquals("0100", hex);
	}

	@Test
	public void testStartsWith()
	{
		String input = "this is a test";
		assertTrue(StringUtil.lineStartsWith(input, 0, "this"));
		assertFalse(StringUtil.lineStartsWith(input, 0, "thisx"));
		assertTrue(StringUtil.lineStartsWith(input, 10, "test"));
		assertTrue(StringUtil.lineStartsWith(input, 9, "test"));
		assertFalse(StringUtil.lineStartsWith(input, 13, "test"));
	}

	@Test
	public void testRemoveQuotes()
	{
		String name = "test";
		String trimmed = StringUtil.removeQuotes(name, "\"");
		assertEquals(name, trimmed);

		name = "\"test";
		trimmed = StringUtil.removeQuotes(name, "\"");
		assertEquals(name, trimmed);

		name = "\"test\"";
		trimmed = StringUtil.removeQuotes(name, "\"");
		assertEquals("test", trimmed);

		name = "\"test'";
		trimmed = StringUtil.removeQuotes(name, "\"");
		assertEquals(name, trimmed);

		name = "'test'";
		trimmed = StringUtil.removeQuotes(name, "\"");
		assertEquals(name, trimmed);

		name = "'test'";
		trimmed = StringUtil.removeQuotes(name, "'");
		assertEquals("test", trimmed);

		name = "`test`";
		trimmed = StringUtil.removeQuotes(name, "`");
		assertEquals("test", trimmed);
	}

	@Test
	public void testLongestLine()
	{
		String s = "this\na test for\nseveral lines";
		String line = StringUtil.getLongestLine(s, 10);
		assertEquals("several lines", line);

		s = "this\na test for\nseveral lines\nand another long line that is even longer\na short end";
		line = StringUtil.getLongestLine(s, 3);
		assertEquals("several lines", line);
		line = StringUtil.getLongestLine(s, 10);
		assertEquals("and another long line that is even longer", line);

		s = "this\na test for\nseveral lines\na long line at the end of the string";
		line = StringUtil.getLongestLine(s, 10);
		assertEquals("a long line at the end of the string", line);

		s = "this\na test for\nseveral lines\na long line at the end of the string\n";
		line= StringUtil.getLongestLine(s, 10);
		assertEquals("a long line at the end of the string", line);

		s = "this\r\na test for\r\nseveral lines\r\na long line at the end of the string\r\n";
		line = StringUtil.getLongestLine(s, 10);
		assertEquals("a long line at the end of the string", line);

		s = "no line feeds";
		line = StringUtil.getLongestLine(s, 10);
		assertEquals(s, line);
	}

	@Test
	public void testFindWordLeftOfCursor()
	{
		String input = "  ab   test   more    text";
		String word = StringUtil.findWordLeftOfCursor(input, 1);
		assertNull(word);
		word = StringUtil.findWordLeftOfCursor(input, 5);
		assertNotNull(word);
		assertEquals("ab", word);
	}

	@Test
	public void testGetWordLefOfCursor()
	{
		String input = "ab test\nmore text";
		String word = StringUtil.getWordLeftOfCursor(input, 2, " \t");
		assertNotNull(word);
		assertEquals("ab", word);
	}

	@Test
	public void testLineStartsWith()
	{
		String s = "some stuff     -- this is a comment";
		boolean isComment = StringUtil.lineStartsWith(s, 0, "--");
		assertFalse(isComment);

		s = "some stuff     -- this is a comment";
		isComment = StringUtil.lineStartsWith(s, 10, "--");
		assertTrue(isComment);

		s = "-- comment'\nselect 'b' from dual;";
		isComment = StringUtil.lineStartsWith(s, 0, "--");
		assertTrue(isComment);

		isComment = StringUtil.lineStartsWith(s, 12, "--");
		assertFalse(isComment);

		isComment = StringUtil.lineStartsWith("\t# non-standard comment", 0, "#");
		assertTrue(isComment);

		isComment = StringUtil.lineStartsWith("update foo set bar = 24     \t # non-standard comment\n", 25, "#");
		assertTrue(isComment);

		isComment = StringUtil.lineStartsWith("x \n-- comment", 0, "--");
		assertFalse(isComment);

		isComment = StringUtil.lineStartsWith("1 \n-- comment", 2, "--");
		assertFalse(isComment);

	}

	@Test
	public void testFindFirstNonWhitespace()
	{
		String s = "   Hello, world";
		int pos = StringUtil.findFirstNonWhitespace(s);
		assertEquals(3, pos);

		s = "some stuff     -- this is a comment";
		pos = StringUtil.findFirstNonWhitespace(s, 10, true);
		assertEquals(15, pos);

		pos = StringUtil.findFirstNonWhitespace(s, 12, true);
		assertEquals(15, pos);

		String empty = "   ";
		pos = StringUtil.findFirstNonWhitespace(empty);
		assertEquals(-1, pos);
	}

	@Test
	public void testGetFirsNonWhitespace()
	{
		String value = "   this is a test";
		char c = StringUtil.getFirstNonWhitespace(value);
		assertEquals('t', c);

		value = "   ";
		c = StringUtil.getFirstNonWhitespace(value);
		assertEquals(0, c);

		value = "";
		c = StringUtil.getFirstNonWhitespace(value);
		assertEquals(0, c);

		value = null;
		c = StringUtil.getFirstNonWhitespace(value);
		assertEquals(0, c);
	}

	@Test
	public void testGetStartingWhitespace()
	{
		String s = "   Hello, world";
		String p = StringUtil.getStartingWhiteSpace(s);
		assertEquals("   ", p);

		s = "Hello, world";
		p = StringUtil.getStartingWhiteSpace(s);
		assertNull(p);

		s = "\t\nHello, world";
		p = StringUtil.getStartingWhiteSpace(s);
		assertEquals("\t\n", p);
	}

	@Test
	public void testMakeFilename()
	{
		String fname = StringUtil.makeFilename("TABLE_NAME");
		assertEquals("table_name", fname);

		fname = StringUtil.makeFilename("TABLE_\\NAME");
		assertEquals("table_name", fname);

		fname = StringUtil.makeFilename("TABLE_<>NAME");
		assertEquals("table_name", fname);
	}

	@Test
	public void testEndsWith()
	{
		String s = "this is a test";
		assertTrue(StringUtil.endsWith(s, "test"));
		assertFalse(StringUtil.endsWith(s, "testing"));

		assertFalse(StringUtil.endsWith("bla", "blabla"));
		assertTrue(StringUtil.endsWith("bla", "bla"));

		assertFalse(StringUtil.endsWith("est", "test"));
		assertFalse(StringUtil.endsWith("no est", "test"));
	}

	@Test
	public void testIndexOf()
	{
		String s = ".this. is a test";
		int pos = StringUtil.indexOf(s, '.');
		assertEquals(0, pos);

		s = "this. is. a. test.";
		pos = StringUtil.indexOf(s, '.');
		assertEquals(4, pos);

		s = "this. is. a test";
		pos = StringUtil.indexOf(s, '.', 2);
		assertEquals(8, pos);
	}

	@Test
	public void testLastIndexOf()
	{
		String s = "this is a test.";
		int pos = StringUtil.lastIndexOf(s, '.');
		assertEquals(s.length() - 1, pos);

		s = "this. is. a. test.";
		pos = StringUtil.lastIndexOf(s, '.');
		assertEquals(s.length() - 1, pos);

		s = "this is a test";
		pos = StringUtil.lastIndexOf(s, '.');
		assertEquals(-1, pos);

		StringBuilder b = new StringBuilder("this is a test.");
		pos = StringUtil.lastIndexOf(b, '.');
		assertEquals(b.length() - 1, pos);

		b = new StringBuilder("this. is a test");
		pos = StringUtil.lastIndexOf(b, '.');
		assertEquals(4, pos);
	}

	@Test
	public void testDecodeUnicode()
	{
		String value = "Incorrect \\ string";
		String decoded = StringUtil.decodeUnicode(value);
		assertEquals(value, decoded);

		value = "Test \\u00E4\\u00E5";
		decoded = StringUtil.decodeUnicode(value);
		assertEquals("Test \u00E4\u00E5", decoded);

		value = "Wrong \\uxyz encoded";
		decoded = StringUtil.decodeUnicode(value);
		assertEquals(value, decoded);

		value = "Wrong \\u04";
		decoded = StringUtil.decodeUnicode(value);
		assertEquals("Wrong string not decoded", value, decoded);

		value = "test \\u";
		decoded = StringUtil.decodeUnicode(value);
		assertEquals("Wrong string not decoded", value, decoded);

		value = "test \\u wrong";
		decoded = StringUtil.decodeUnicode(value);
		assertEquals("Wrong string not decoded", value, decoded);

		decoded = StringUtil.decodeUnicode("\\r\\ntest");
		assertEquals("Single char not replaced correctly", "\r\ntest", decoded);

		decoded = StringUtil.decodeUnicode("Hello \\t World");
		assertEquals("Single char not replaced correctly", "Hello \t World", decoded);

		decoded = StringUtil.decodeUnicode("test\\t");
		assertEquals("Single char not replaced correctly", "test\t", decoded);

		decoded = StringUtil.decodeUnicode("test\\x");
		assertEquals("Single char not replaced correctly", "test\\x", decoded);

		decoded = StringUtil.decodeUnicode("test\\");
		assertEquals("Single char not replaced correctly", "test\\", decoded);

		decoded = StringUtil.decodeUnicode("test\\\\");
		assertEquals("test\\", decoded);

		value = "abc\\\\def";
		decoded = StringUtil.decodeUnicode(value);
		assertEquals("abc\\def", decoded);

		value = "abc\\\\n";
		decoded = StringUtil.decodeUnicode(value);
		assertEquals("abc\\n", decoded);
	}

	@Test
	public void testReplace()
	{
		String s = StringUtil.replace(null, "gaga", "gogo");
		assertNull(s);

		s = StringUtil.replace("gaga", null, "gogo");
		assertEquals("gaga", s);

		s = StringUtil.replace("gaga", "gogo", null);
		assertEquals("gaga", s);

		String value = "something";
		String result = StringUtil.replace(value, "some", "other");
		assertEquals("otherthing", result);

		value = "ababababaab";
		result = StringUtil.replace(value, "a", "x");
		assertEquals("xbxbxbxbxxb", result);

		value = "foobear";
		result = StringUtil.replace(value, "bear", "bar");
		assertEquals("foobar", result);

		value = "some text";
		result = StringUtil.replace(value, "xyz", "zxy");
		assertEquals(value, result);

		value = "some text";
		result = StringUtil.replace(value, "text", "other text");
		assertEquals("some other text", result);

		value = "some text";
		result = StringUtil.replace(value, "some", "no");
		assertEquals("no text", result);
	}

	@Test
	public void testEncodeUnicode()
	{
		String value = "\u00E4";
		String enc = StringUtil.escapeText(value, CharacterRange.RANGE_7BIT, "");
		assertEquals("Umlaut not replaced", "\\u00E4", enc);

		value = "\n";
		enc = StringUtil.escapeText(value, CharacterRange.RANGE_7BIT, "");
		assertEquals("NL not replaced" , "\\n", enc);

		value = "\u0016";
		enc = StringUtil.escapeText(value, CharacterRange.RANGE_CONTROL, "", CharacterEscapeType.pgHex);
		assertEquals("\\x16", enc);
	}

	@Test
	public void testMakePlainLF()
	{
		String line = "line1\r\nline2";
		String newline = StringUtil.makePlainLinefeed(line);
		assertEquals("line1\nline2", newline);

		line = "line1\nline2";
		newline = StringUtil.makePlainLinefeed(line);
		assertEquals("Wrong replacement", "line1\nline2", newline);

		line = "line1\rline2";
		newline = StringUtil.makePlainLinefeed(line);
		assertEquals("line1\nline2", newline);

		line = "line1\n\rline2";
		newline = StringUtil.makePlainLinefeed(line);
		assertEquals("line1\nline2", newline);
	}

	@Test
	public void testTrimStringBuilder()
	{
		StringBuilder s = new StringBuilder();
		s.append("hello   ");
		StringUtil.trimTrailingWhitespace(s);
		assertEquals("hello", s.toString());

		s = new StringBuilder();
		s.append("hello\n");
		StringUtil.trimTrailingWhitespace(s);
		assertEquals("hello", s.toString());

		s = new StringBuilder();
		s.append("hello\nnewline  ");
		StringUtil.trimTrailingWhitespace(s);
		assertEquals("hello\nnewline", s.toString());

		s = new StringBuilder();
		s.append(" hello");
		StringUtil.trimTrailingWhitespace(s);
		assertEquals(" hello", s.toString());

		s = new StringBuilder();
		StringUtil.trimTrailingWhitespace(s);
		assertEquals("", s.toString());

		s = new StringBuilder(" ");
		StringUtil.trimTrailingWhitespace(s);
		assertEquals("", s.toString());

	}

	@Test
	public void testRtrim()
	{
		String s = "bla";
		assertEquals(s, StringUtil.rtrim(s));

		s = " \tbla";
		assertEquals(s, StringUtil.rtrim(s));

		s = "bla \t\n";
		assertEquals("bla", StringUtil.rtrim(s));

		s = "bla \t\nbla";
		assertEquals(s, StringUtil.rtrim(s));

		s = " \n\r\t";
		assertEquals("", StringUtil.rtrim(s));

		s = "";
		assertEquals(s, StringUtil.rtrim(s));
	}

	@Test
	public void testEqualString()
	{
		String one = "bla";
		String two = null;

		assertEquals(false, StringUtil.equalString(one, two));
		assertEquals(false, StringUtil.equalString(two, one));

		assertEquals(false, StringUtil.equalStringIgnoreCase(one, two));
		assertEquals(false, StringUtil.equalStringIgnoreCase(two, one));

		one = "bla";
		two = "bla";

		assertEquals(true, StringUtil.equalString(one, two));
		assertEquals(true, StringUtil.equalStringIgnoreCase(two, one));

		one = "bla";
		two = "BLA";

		assertEquals(false, StringUtil.equalString(one, two));
		assertEquals(true, StringUtil.equalStringIgnoreCase(two, one));

		one = "bla";
		two = "blub";

		assertEquals(false, StringUtil.equalString(one, two));
		assertEquals(false, StringUtil.equalStringIgnoreCase(two, one));
	}

	@Test
	public void testCaseCheck()
	{
		assertEquals(false, StringUtil.isUpperCase("This is a test"));
		assertEquals(true, StringUtil.isMixedCase("This is a test"));
		assertEquals(false, StringUtil.isLowerCase("This is a test"));

		assertEquals(true, StringUtil.isLowerCase("this is a test 12345 #+*-.,;:!\"$%&/()=?"));
		assertEquals(true, StringUtil.isUpperCase("THIS IS A TEST 12345 #+*-.,;:!\"$%&/()=?"));
		assertEquals(true, StringUtil.isUpperCase("1234567890"));
		assertEquals(true, StringUtil.isLowerCase("1234567890"));
	}

	@Test
	public void testGetRealLineLenght()
	{
		int len = StringUtil.getRealLineLength("bla\r");
		assertEquals(3,len);

		len = StringUtil.getRealLineLength("bla\r\n");
		assertEquals(3,len);

		len = StringUtil.getRealLineLength("bla\r\n\n");
		assertEquals(3,len);

		len = StringUtil.getRealLineLength("bla \r\n\n\r");
		assertEquals(4,len);

		len = StringUtil.getRealLineLength("bla");
		assertEquals(3,len);

		len = StringUtil.getRealLineLength("\r\n");
		assertEquals(0,len);

		len = StringUtil.getRealLineLength("\n");
		assertEquals(0,len);
	}

	@Test
	public void testIsWhitespace()
	{
		String s = "bla";
		assertEquals(false, StringUtil.isWhitespace(s));

		s = "   bla   ";
		assertEquals(false, StringUtil.isWhitespace(s));

		s = " \n \n";
		assertEquals(true, StringUtil.isWhitespace(s));

		s = " \t\r\n   ;";
		assertEquals(false, StringUtil.isWhitespace(s));

		s = "";
		assertEquals(false, StringUtil.isWhitespace(s));
	}

	@Test
	public void testTrimBuffer()
	{
		StringBuilder b = new StringBuilder("bla");
		StringUtil.trimTrailingWhitespace(b);
		assertEquals("Buffer was changed", "bla", b.toString());

		b = new StringBuilder("bla bla ");
		StringUtil.trimTrailingWhitespace(b);
		assertEquals("Whitespace not removed", "bla bla", b.toString());

		b = new StringBuilder("bla bla \t");
		StringUtil.trimTrailingWhitespace(b);
		assertEquals("Whitespace not removed", "bla bla", b.toString());

		b = new StringBuilder("bla bla \t\n\r  \t");
		StringUtil.trimTrailingWhitespace(b);
		assertEquals("Whitespace not removed", "bla bla", b.toString());
	}

	@Test
	public void testToArray()
	{
		List<String> elements = new LinkedList<>();
		elements.add("one");
		elements.add("two");
		elements.add("three");

		String[] result = StringUtil.toArray(elements, false);
		assertEquals(result.length, 3);
		assertEquals(result[1], "two");

		result = StringUtil.toArray(elements, true);
		assertEquals(result.length, 3);
		assertEquals(result[1], "TWO");
	}

	@Test
	public void testGetDoubleValue()
	{
		double value = StringUtil.getDoubleValue("123.45", -1);
		assertEquals(123.45, value, 0.01);

		value = StringUtil.getDoubleValue(" 123.45 ", -1);
		assertEquals(123.45, value, 0.01);

		value = StringUtil.getDoubleValue("bla", -66);
		assertEquals(-66, value, 0.01);
	}

	@Test
	public void testGetIntValue()
	{
		int iValue = StringUtil.getIntValue(" 123 ", -1);
		assertEquals(123, iValue);

		iValue = StringUtil.getIntValue("42", -1);
		assertEquals(42, iValue);

		iValue = StringUtil.getIntValue("bla", -24);
		assertEquals(-24, iValue);
	}

	@Test
	public void testStringToList()
	{
		String list = "1,2,3";
		List l = StringUtil.stringToList(list, ",", true, true, true);
		assertEquals("Wrong number of elements returned", 3, l.size());

		list = "1,2,,3";
		l = StringUtil.stringToList(list, ",", true, true, true);
		assertEquals("Empty element not removed", 3, l.size());

		list = "1,2, ,3";
		l = StringUtil.stringToList(list, ",", false);
		assertEquals("Empty element removed", 4, l.size());

		list = "1,2,,3";
		l = StringUtil.stringToList(list, ",", false);
		assertEquals("Null element not removed", 3, l.size());

		list = " 1 ,2,3";
		l = StringUtil.stringToList(list, ",", true);
		assertEquals("Null element not removed", 3, l.size());
		assertEquals(" 1 ", l.get(0));

		l = StringUtil.stringToList(list, ",", true, true);
		assertEquals("Element not trimmed", "1", l.get(0));

		list = "1,\"2,5\",3";
		l = StringUtil.stringToList(list, ",", true, true, true);
		assertEquals("Quoted string not recognized","2,5", l.get(1));

		list = "library.jar";
		l = StringUtil.stringToList(list, ";", true, true, false);
		assertEquals("Single element list not correct", 1, l.size());
		assertEquals("Single element list not correct", "library.jar", l.get(0));

	}

	@Test
	public void testHasOpenQuotes()
	{
		String value = "this line does not have quotes";
		assertFalse(StringUtil.hasOpenQuotes(value, '\'', QuoteEscapeType.none));

		value = "this line 'does' have quotes";
		assertFalse(StringUtil.hasOpenQuotes(value, '\'', QuoteEscapeType.none));

		value = "this line leaves a 'quote open";
		assertTrue(StringUtil.hasOpenQuotes(value, '\'', QuoteEscapeType.none));

		value = "this 12\\\"monitor ";
		assertFalse(StringUtil.hasOpenQuotes(value, '"', QuoteEscapeType.escape));

		value = "this \"monitor ";
		assertTrue(StringUtil.hasOpenQuotes(value, '"', QuoteEscapeType.escape));

		value = "'foo';'Peter\\'s house';'some data'";
		assertFalse(StringUtil.hasOpenQuotes(value, '\'', QuoteEscapeType.escape));

		value = "'foo';'Peter''s house';'some data'";
		assertFalse(StringUtil.hasOpenQuotes(value, '\'', QuoteEscapeType.duplicate));

		value = "'foo';'Peter's house';'some data'";
		assertTrue(StringUtil.hasOpenQuotes(value, '\'', QuoteEscapeType.duplicate));
	}

	@Test
	public void testIsNumber()
	{
		boolean isNumber = StringUtil.isNumber("1");
		assertEquals(true, isNumber);

		isNumber = StringUtil.isNumber("1.234");
		assertEquals(true, isNumber);

		isNumber = StringUtil.isNumber("1.xxx");
		assertEquals(false, isNumber);

		isNumber = StringUtil.isNumber("bla");
		assertEquals(false, isNumber);

		assertFalse(StringUtil.isNumber(""));

		assertTrue(StringUtil.isNumber("  1 "));
	}

	@Test
	public void testMaxString()
	{
		String s = StringUtil.getMaxSubstring("Dent", 4, null);
		assertEquals("Truncated", "Dent", s);

		s = StringUtil.getMaxSubstring("Dent1", 4, null);
		assertEquals("Truncated", "Dent", s);

		s = StringUtil.getMaxSubstring("Den", 4, null);
		assertEquals("Truncated", "Den", s);

		s = StringUtil.getMaxSubstring("Beeblebrox", 5, null);
		assertEquals("Truncated", "Beebl", s);

		s = StringUtil.getMaxSubstring("Beeblebrox", 5, "...");
		assertEquals("Truncated", "Beebl...", s);

	}

	@Test
	public void testTrimQuotes()
	{
		String s = StringUtil.trimQuotes(" \"bla\" ");
		assertEquals("bla", s);
		s = StringUtil.trimQuotes(" \"bla ");
		assertEquals(" \"bla ", s);
		s = StringUtil.trimQuotes(" 'bla' ");
		assertEquals("bla", s);
	}

	@Test
	public void testPadRight()
	{
		String result = StringUtil.padRight("someStuff", 20);
		assertEquals(20, result.length());
		assertTrue(result.startsWith("someStuff"));
	}

	@Test
	public void testFormatNumber()
	{
		String result = StringUtil.formatNumber(10, 10, true);
		assertEquals(10, result.length());
		assertEquals("10        ", result);

		result = StringUtil.formatNumber(10, 10, false);
		assertEquals(10, result.length());
		assertEquals("        10", result);

		result = StringUtil.formatNumber(100000, 5, false);
		assertEquals(6, result.length());
		assertEquals("100000", result);
	}

	@Test
	public void testContainsWords()
	{
		String input = "So long and thanks for all the fish";
		List<String> values = CollectionUtil.arrayList("thanks", "phish");

		boolean found = StringUtil.containsWords(input, values, false, true);
		assertTrue(found);

		found = StringUtil.containsWords(input, values, true, true);
		assertFalse(found);

		values = CollectionUtil.arrayList("thanks", "fish");
		found = StringUtil.containsWords(input, values, true, false);
		assertTrue(found);

		found = StringUtil.containsWords(input, values, false, true);
		assertTrue(found);

		values = CollectionUtil.arrayList("thanks", "FISH");
		found = StringUtil.containsWords(input, values, true, false);
		assertFalse(found);

		found = StringUtil.containsWords(input, values, true, true);
		assertTrue(found);

		values = CollectionUtil.arrayList("nothere", "also_not_there");
		found = StringUtil.containsWords(input, values, true, false);
		assertFalse(found);

		values = CollectionUtil.arrayList("nothere", "also_not_there");
		found = StringUtil.containsWords(input, values, false, false);
		assertFalse(found);

		values = CollectionUtil.arrayList("a[ndl]{2}");
		found = StringUtil.containsWords(input, values, false, false, true);
		assertTrue(found);

		input = "Special $com";
		values = CollectionUtil.arrayList("$com");
		found = StringUtil.containsWords(input, values, false, false, false);
		assertTrue(found);

		found = StringUtil.containsWords(
			"Life, Universe\nand everything",
			CollectionUtil.arrayList("^and"), false, false, true
		);
		assertTrue(found);
	}

	@Test
	public void testArray()
	{
		int[] data = new int[] {1,2,3,4,5};
		String list = StringUtil.arrayToString(data);
		assertEquals("1,2,3,4,5", list);
		int[] data2 = StringUtil.stringToArray(list);
		assertArrayEquals(data, data2);
	}

}
