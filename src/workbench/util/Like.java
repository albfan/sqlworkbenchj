/*
 * Like.java
 *
 * Created on 11. Oktober 2002, 15:02
 */

package workbench.util;

import java.util.*;

/**
 * SQL comparison operator <i>like</i>.
 * An instance of this class represents a pattern of the SQL operator LIKE.
 *
 * @author  <a href="mailto:ey@inweb.de?subject=Like Operator">Christian Ey</a>
 * @version $Revision: 1.5 $
 */
public class Like {

    public static final String WILDCARD_SINGLE = "_";
    public static final String WILDCARD_SEQUENCE = "%";

    private String pattern;
    private String escape;
    private boolean ignoreCase;

    /** Pattern string in form of a sequence of tokens */
    private Like.Token[] a;

		public static void main( String[] args) throws Exception
		{

			Like pattern = new Like("'%request%'", false);
			boolean wowThisMatches = pattern.like(" ");
			System.out.println( "matches: " + wowThisMatches);
		}

    /**
     * Creates a new instance of a Like Pattern.
     *
     * @param pattern the LIKE pattern to represent
     * @param escape the escape character/String used to escape wildcards
     */
    public Like( String pattern, String escape) {
        this.init( pattern, escape, false);
    }

    /**
     * Creates a new instance of a Like Pattern.
     *
     * @param pattern the LIKE pattern to represent
     * @param ignoreCase defines whether pattern matching is case sensitive
     *        or not
     */
    public Like( String pattern, boolean ignoreCase) {
        this.init( pattern, null, ignoreCase);
    }

    /**
     * Creates a new instance of a Like Pattern.
     * If this constructor is used, wildcards can not be escaped.
     *
     * @param pattern the LIKE pattern to represent
     */
    public Like( String pattern) {
        this.init( pattern, null, false);
    }

    private void init( String pattern, String escape, boolean ignoreCase) {
        if (pattern == null) {
            pattern = "";
        }
        this.ignoreCase = ignoreCase;
        this.pattern = pattern;
        pattern = (ignoreCase) ? pattern.toLowerCase() : pattern;
        this.escape = escape;

        // cut the pattern into tokens
        StringTokenizer t = new StringTokenizer( pattern, WILDCARD_SINGLE+WILDCARD_SEQUENCE, true);
        a = new Like.Token[t.countTokens()];
        String u;
        for (int i=0; i<a.length; ++i) {
            u = t.nextToken();
            if (u.equals( WILDCARD_SINGLE)) {
                a[i] = new Like.Token( WILDCARD_SINGLE, true, false);
            } else if(u.equals( WILDCARD_SEQUENCE)) {
                a[i] = new Like.Token( WILDCARD_SEQUENCE, false, true);
            } else {
                a[i] = new Like.Token( u, false, false);
            }
        }

        if (a.length > 0) {
            Like.Token last;

            // puts the tokens back together where an escape char applies
            if (escape != null) {
                int esclen = escape.length();
                ArrayList l = new ArrayList();
                last = a[0];
                for (int i=1; i<a.length; ++i) {
                    if (last.isImage && last.image.endsWith( escape)) {
                        last.image = last.image.substring( 0, last.image.length()-esclen)
                                        .concat( a[i].image);
                        if (((i+1)<a.length) && a[i+1].isImage) {
                            ++i;
                            last.image = last.image.concat( a[i].image);
                        }
                    } else {
                        l.add( last);
                        last = a[i];
                    }
                }
                l.add( last);
                a = (Like.Token[])l.toArray( new Like.Token[l.size()]);

//                System.out.println( "escaped: " + tokensToString( a));
            }

            // optimize: any sequence of wildcards will result in:
            // All WILDCARD_SINGLEs first and one WILDCARD_SEQUENCE
            // afterwards (if any), no matter where and how often
            // WILDCARD_SEQUENCE occurred.
            // A sequence of wildcards ends when an image token is
            // found or end of tokens reached.
            boolean optimize = false;
            last = a[0];
            for (int i=1; (i<a.length) && !optimize; ++i) {
                optimize = !last.isImage && !a[i].isImage;
                last = a[i];
            }

            if (optimize) {
                ArrayList l = new ArrayList();
                // the last WILDCARD_SEQUENCE token
                last = null;
                for (int i=0; i<a.length; ++i) {
                    if (a[i].isImage) {
                        if (last != null) {
                            l.add( last);
                            last = null;
                        }
                        l.add( a[i]);
                    } else if (a[i].single) {
                        l.add( a[i]);
                    } else { //==> if (a[i].sequence) {
                        last = a[i];
                    }
                }
                if (last != null) {
                    l.add( last);
                }
                a = (Like.Token[])l.toArray( new Like.Token[l.size()]);

//                System.out.println( "optimized: " + tokensToString( a));
            }
        }
    }

    /**
     * Checks whether a specified String <code>s</code>
     * matches this LIKE pattern.
     *
     * @param s the string to match
     * @return <code>true</code> if <code>s</code> matches this pattern
     */
    public boolean like( String s) {
        if (s != null) {
            if (ignoreCase) {
                s = s.toLowerCase();
            }
            return next( s, s.length(), 0, 0);
        } else {
            return false;
        }
    }

    private boolean next( String s, int length, int cursor, int element) {
        if (a.length > element) {
            if (a[element].sequence) {
                if (a.length > element+1) {
                    // calculate the sequence wildcard

                    int tempcursor = cursor;
                    do {
                        cursor = s.indexOf( a[element+1].image, tempcursor);
                        if (cursor < 0) {
                            return false;
                        } else {
                            // found an occurence of the next element
//                            System.out.println( "nextfound?"+tempcursor+":"+a[element+1].image+":"+cursor);

                            tempcursor = cursor + a[element+1].image.length();
                        }
                    } while (!next( s, length, cursor, element+1));
                    return true;
                } else {
                    // the sequence wildcard is the last element, so skip til the end

//                    System.out.println( "skipped!"+cursor);

                    // would be nicer if it was 'return next()' but this is faster
                    // and does the same
                    return true;
                }
            } else if (a[element].single) {
//                System.out.println( "onemore?"+cursor);
                if ((cursor+1) < length) {
                    return next( s, length, cursor+1, element+1);
                } else {
                    return false;
                }
            } else {
                int j = cursor + a[element].image.length();
//                System.out.println( "substring?"+s+":"+cursor+","+j+":"+length);
                if (   (j <= length)
                    && a[element].image.equals( s.substring( cursor, j))  ) {

                    return next( s, length, j, element+1);
                } else {
                    return false;
                }
            }
        } else {
            return cursor == length;
        }
    }

    private String tokensToString( Like.Token[] ts) {
        StringBuffer b = new StringBuffer();
        for (int i=0; i<ts.length; ++i) {
            b.append( ts[i].image + "|");
        }
        return b.toString();
    }

    public String toString() {
        return "pattern: " + pattern + ", escape: " + escape
            + ", optimized pattern: " + tokensToString( a);
    }

    /** Getter for attribute pattern */
    public String getPattern() {
        return this.pattern;
    }

    /** Getter for attribute ignoreCase */
    public boolean getIgnoreCase() {
        return this.ignoreCase;
    }

    /**
     * Tokens a pattern consists of.
     */
    static class Token {
        String image = null;
        boolean single = false;
        boolean sequence = false;
        boolean isImage = false;

        Token( String image, boolean single, boolean sequence) {
            this.image = image;
            this.single = single;
            this.sequence = sequence;
            this.isImage = !single && !sequence;
        }
    }
}