package workbench.util;

import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.io.*;

/**
 * An augmented string tokenizer.
 * <P>
 * The delimiter can be quoted. It is supposed to be a white space character.
 * The character used to quote the delimiter is supposed to be a double quote.
 * This tokenizer allows tokens to comprise white spaces provided the token
 * is inside double quotes. Some examples of what is expected :
 * <Br>
 * input : aaa "bbb ccc" " dd " "e    f"
 * <Br>
 * output : <aaa> <bbb ccc> < dd > <e    f>
 * <P>
 * It is more sophisticated than the StringTokenizer and less complex than
 * StreamTokenizer.
 *
 * @version $Id: LineTokenizer.java,v 1.3 2002-12-14 15:54:36 thomas Exp $
 * @author Jean-Paul Le Fèvre
 * @see java.io.StreamTokenizer
 */
//______________________________________________________________________________

public class LineTokenizer extends StringTokenizer
{
	/**
	 * @serial The quote character.
	 */
	private int qt = '"';
	/**
	 * @serial The default delimiter.
	 */
	static final private String DELIM = " \t\n\r\f";
	/**
	 * @serial The delimiter.
	 */
	private String delim = DELIM;
	
	
	/**
	 * Creates a tokenizer.
	 * @param str the line to tokenize.
	 */
	public LineTokenizer(String str)
	{
		super(str, DELIM, true);
	}
	
	/**
	 * Creates a tokenizer.
	 * @param str the line to tokenize.
	 * @param delim the delimiters.
	 */
	public LineTokenizer(String str, String delim)
	{
		super(str, delim, true);
		this.delim = delim;
	}
	
	/**
	 * Sets the quote character.
	 * @param ch the character used to quote the separator.
	 */
	final public void quoteChar(int ch)
	{
		this.qt = ch;
	}
	
	/**
	 * Returns the next token from this line tokenizer.
	 * <P>
	 * The delimiters are not provided by these method.
	 * @return the next token
	 */
	public String nextToken()
	{
		/**
		 * Get the next actual token. Get rid of the delimiters.
		 */
		String token = nextTrueToken();
		if(token == null) throw new NoSuchElementException();
		/**
		 * It is not a quoted token. Simply return it.
		 */
		if(token.charAt(0) != qt)	return token;

		// check if the token contains a delimiter
		// if not, then remove the quote char and 
		// return the token
		boolean containsToken = false;
		for (int i=0; i<this.delim.length(); i++)
		{
			if (token.indexOf(this.delim.charAt(i)) > -1)
			{
				containsToken = true;
			}
		}
		if (!containsToken)
		{
			// remove the quote char
			int end = 0;
			if (token.charAt(token.length()-1) == qt)
			{
				end = token.length() -1;
			}
			token = token.substring(1, end);
			return token;
		}
		
		/**
		 * There is just one quoted token :  "abcd"
		 * Discard the quote and return the token.
		 */
		if(! hasMoreTokens())
		{
			int i = token.length() - 1;
			if(i < 2)
			{
				return token;
			}
			else if(token.charAt(i) == qt)
			{
				return token.substring(1, i);
			}
			else
			{
				return token;
			}
		}
		
		/**
		 * A quoted token with embedded delimiters : "a b  c"
		 */
		StringBuffer buf = new StringBuffer(token.substring(1));

		while(hasMoreTokens())
		{
			token = super.nextToken();
			int i = token.length() - 1;
			
			if(token.charAt(i) != qt)
			{
				buf.append(token);
			}
			else if(i < 1)
			{
				break;
			}
			else
			{
				buf.append(token.substring(0, i));
				break;
			}
		}
		
		return buf.toString();
	}

	/**
	 * Returns the next token from this line tokenizer.
	 * <Br>
	 * It swallows the delimiters before returning the token.
	 * @return the next token
	 * @see StringTokenizer#StringTokenizer(String, String, boolean)
	 */
	final private String nextTrueToken()
	{
		while(hasMoreTokens())
		{
			String token = super.nextToken();
			if(delim.indexOf(token.charAt(0)) < 0)
			{
				return token;
			}
		}
		
		return null;
	}
	
	public static void main(String args[])
	{
		String line = "spool text \"dasdfasd\" tablename";
		LineTokenizer ltk = new LineTokenizer(line, " ");
		System.out.println("count=" + ltk.countTokens());
		while(ltk.hasMoreTokens())
		{
			System.out.println(ltk.nextToken());
		}
	}
	
}
