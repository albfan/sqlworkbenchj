/*
 * PrintUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.print;

import java.awt.print.PageFormat;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;

/*
 * PrintUtil.java
 *
 * Created on May 17, 2003, 9:06 PM
 */

/**
 *
 * @author  thomas
 */
public class PrintUtil
{

	/** Creates a new instance of PrintUtil */
	public PrintUtil()
	{
	}

	public static PrintRequestAttributeSet getPrintAttributes(PageFormat format)
	{
		PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
		PrintRequestAttribute att = null;
		int value = format.getOrientation();
		switch (value)
		{
			case PageFormat.LANDSCAPE:
				att = OrientationRequested.LANDSCAPE;
				break;
			case PageFormat.PORTRAIT:
				att = OrientationRequested.PORTRAIT;
				break;
			case PageFormat.REVERSE_LANDSCAPE:
				att = OrientationRequested.REVERSE_LANDSCAPE;
				break;
		}
		attr.add(att);
		att = new MediaPrintableArea((float)format.getImageableX(), 
		                             (float)format.getImageableY(), 
																 (float)format.getImageableWidth(), 
																 (float)format.getImageableHeight(), MediaPrintableArea.INCH);
		attr.add(att);
		
		return attr;
	}

	public static boolean pageFormatEquals(PageFormat first, PageFormat second)
	{
		if (first != null || second != null) return false;
		if (first.getOrientation() != second.getOrientation()) return false;
		if (first.getHeight() != second.getHeight()) return false;
		if (first.getWidth() != second.getWidth()) return false;
		if (first.getImageableX() != second.getImageableX()) return false;
		if (first.getImageableY() != second.getImageableY()) return false;
		if (first.getImageableWidth() != second.getImageableWidth()) return false;
		if (first.getImageableHeight() != second.getImageableHeight()) return false;
		return true;
	}

	public static double millimeterToPoints(double mm)
	{
		double inch = millimeterToInch(mm);
		return (inch * 72);
	}
	public static double pointsToMillimeter(double points)
	{
		// convert mm to inch
		double inch = points / 72;
		return inchToMillimeter(inch);
	}

	public static double millimeterToInch(double mm)
	{
		return (mm / 25.4);
	}

	public static double inchToMillimeter(double inch)
	{
		return (inch * 25.4);
	}

	public static void printPageFormat(String aName, PageFormat aFormat)
	{
		double width = aFormat.getPaper().getWidth();
		double height = aFormat.getPaper().getHeight();

		double leftmargin = aFormat.getImageableX();
		double rightmargin = width - leftmargin - aFormat.getImageableWidth();

		double topmargin = (int)aFormat.getImageableY();
		double bottommargin = height - topmargin - aFormat.getImageableHeight();

		System.out.println(aName + ": paper size=[" + width + "," + height + "]");
		System.out.println(aName + ": imageable size=[" + aFormat.getImageableWidth() + "," + aFormat.getImageableHeight() + "]");
		System.out.println(aName + ": margins (l,r,t,b)=" + leftmargin + "," + rightmargin + "," + topmargin + "," + bottommargin);
	}

	public static void main(String[] args)
	{
		/*
		System.out.println("72 point = " + pointsToMillimeter(72) + "mm");
		System.out.println("1 inch = " + inchToMillimeter(1) + "mm");
		System.out.println("1 mm = " + millimeterToInch(1) + "inch");
		System.out.println("25 mm = " + millimeterToPoints(25.4) + "points");
		System.out.println("20 mm = " + millimeterToPoints(20) + "points");
		*/
		System.out.println(pointsToMillimeter(612.0));
	}
}
