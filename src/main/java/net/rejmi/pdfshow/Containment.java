package net.rejmi.pdfshow;

/** Class to implement the contains() method.
 * In descriptions, UL, UR, LL, LR refer to an object whose bbox
 * is in one of the four directions relative to its x,y, NOT to 0,0
 * The coordinate frame throughout this app has (0,0) in the upper left,
 * as per Java, X Windows, and PostScript.  See the following map:
 *
 ***************************************************
 * (0,0)                                  (maxx,0) *
 *                                                 *
 *     llxy                              urxy      *
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
	
	/**
	 * Determine if the given object contains the given point
	 * (which is usually a mouse click location).
	 * @param g The GObject that may contain the point
	 * @param mx The point's X coordinate
	 * @param my The point's Y coordinate
	 * @return True if the point is contained within the given GObject's bbox
	 */
	static boolean contains(GObject g, int mx, int my) {
		int urx = 0, ury = 0, llx = 0, lly = 0;
		if (g.width == 0 && g.height == 0) {
			return false; // Too small to contain anything
		}
		int quad = 0;
		if (g.width < 0 && g.height < 0) {	// 1
			// UL upper left quadrant: width negative, height negative
			quad = 1;
			urx = g.x + g.width;
			ury = g.y + g.height;
			llx = g.x;
			lly = g.y;
		} else if (g.width > 0 && g.height < 0) { // 2
			// UR upper right quadrant: width positive, height negative
			quad = 2;
			llx = g.x;
			lly = g.y + g.height;
			urx = g.x + g.width;
			ury = g.y;
		} else if (g.width < 0 && g.height > 0) { // 3
			// LL lower left quadrant: width negative, height positive
			quad = 3;
			urx = -g.x - -g.width;
			ury = g.y;
			llx = g.x;
			lly = g.y + g.height;
		} else if (g.width >= 0 && g.height > 0) { // 3
			// LR lower right quadrant: width positive, height positive
			quad = 4;
			llx = g.x;
			lly = g.y;
			urx = g.x + g.width;
			ury = g.y + g.height;
		}
		System.out.printf("contains(%s, mx %d, my %d) quad %d\n", g, mx, my, quad);
		System.out.printf("BBOX llx %d lly %d; urx %d ury %d;\n", llx, lly, urx, ury);
		if (urx < llx)
			throw new IllegalArgumentException("urx < llx for " + g);
		if (ury < lly)
			throw new IllegalArgumentException("ury < lly for " + g);
		return mx > llx && my > lly && mx < urx && my < ury;
	}

}
