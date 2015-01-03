/*
 * PrintUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.print;

import java.awt.print.PageFormat;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;

/**
 *
 * @author Thomas Kellerer
 */
public class PrintUtil
{

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
		if (first == null || second == null) return false;
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
}
