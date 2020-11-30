package net.rejmi.pdfshow;

import java.util.logging.Logger;

/** Class to implement the contains() method.
 * In descriptions, ULX, ULY, LRX, LRY refer to an object whose bbox
 * is in one of the four directions--relative to its x,y, NOT to 0,0
 * The coordinate frame throughout this app has (0,0) in the upper left,
 * as per Java, X Windows, and PostScript.  See the following map:
 *
 ***************************************************
 * (0,0)                                  (maxx,0) *
 *                                                 *
 *     ulxy                              urxy      *
 *      |                                 |        *
 *      +----> *********************      |        *
 *      |      *    1    *    2    *      |        *
 *      +----> ********************* <----+        *
 *             *    3    *    4    *      |        *
 *             ********************* <----+        *
 *                                                 *
 * (0,maxy)                            (maxX,maxY) *
 ***************************************************
 */
public class Containment {
	
	static Logger logger = Logger.getLogger("pdfshow.gobject");
	
	/**
	 * Determine if the given object contains the given point
	 * (which is usually a mouse click location).
	 * XXX Make this an instance method in GObject.
	 * @param g The GObject that may contain the point
	 * @param mx The point's X coordinate
	 * @param my The point's Y coordinate
	 * @return True if the point is contained within the given GObject's bbox
	 */
	static boolean contains(GObject g, final int mx, final int my) {
		int ulx = 0, uly = 0, lrx = 0, lry = 0;
		if (g.width == 0 && g.height == 0) {
			return false; // Too small to contain anything
		}
		logger.info(String.format("contains(%s, mx %d, my %d)\n", g, mx, my));
		int quad = 0;
		if (g.width < 0 && g.height < 0) {	// 1
			// UL upper left quadrant: width negative, height negative
			quad = 1;
			ulx = g.x + g.width;
			uly = g.y + g.height;
			lrx = g.x;
			lry = g.y;
		} else if (g.width > 0 && g.height < 0) { // 2
			// UR upper right quadrant: width positive, height negative
			quad = 2;
			ulx = g.x;
			uly = g.y + g.height;
			lrx = g.x + g.width;
			lry = g.y;
		} else if (g.width < 0 && g.height > 0) { // 3
			// LL lower left quadrant: width negative, height positive
			quad = 3;
			ulx = g.x - -g.width;
			uly = g.y;
			lrx = g.x;
			lry = g.y + g.height;
		} else if (g.width >= 0 && g.height > 0) { // 3
			// LR lower right quadrant: width positive, height positive
			quad = 4;
			ulx = g.x;
			uly = g.y;
			lrx = g.x + g.width;
			lry = g.y + g.height;
		}
		logger.info(String.format("BBOX ulx %d uly %d; lrx %d lry %d; Quadrant %d\n", ulx, uly, lrx, lry, quad));
		if (ulx > lrx)
			throw new IllegalArgumentException("ulx > lrx for " + g);
		if (uly > lry)
			throw new IllegalArgumentException("uly > lry for " + g);
		return mx > ulx && my > uly && mx < lrx && my < lry;
	}

}
