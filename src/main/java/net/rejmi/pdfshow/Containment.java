package net.rejmi.pdfshow;

/** Class to contain the contains() method.
 * In descriptions, UL, UR, LL, LR refer to an object whose bbox
 * is in one of the four directions relative to its x,y, NOT to 0,0
 * The coordinate frame throughout this app has (0,0) in the upper left
 * (as per Java, X Windows, and PostScript). 
 */
public class Containment {
	
	static boolean contains(GObject g, int mx, int my) {
		int ulx = 0, uly = 0, lrx = 0, lry = 0;
		if (g.width == 0 && g.height == 0) {
			return false; // Too small to contain anything
		}
		if (g.width < g.x && g.height < g.y) {
			// UL upper left quadrant: width negative, height negative
			ulx = g.x + g.width;
			uly = g.y + g.height;
			lrx = g.x;
			lry = g.y;
		}
		if (g.width > g.x && g.height < g.y) {
			// UR upper right quadrant: width positive, height negative
			ulx = g.x;
			uly = g.y + g.height;
			lrx = g.x + g.width;
			lry = g.y;
		}
		if (g.width < g.x && g.height > g.y) {
			// LL lower left quadrant: width negative, height positive
			ulx = g.x + g.width;
			uly = g.y;
			lrx = g.x;
			lry = g.y + g.height;
		}
		if (g.width >= g.x && g.height > g.y) {
			// LR lower right quadrant: width, height both positive
			ulx = g.x;
			ulx = g.y;
			lrx = g.x + g.width;
			lry = g.y + g.height;
		}
		// System.out.printf("Contains.contains(%s, mx %d, my %d) n", g, mx, my);
		// System.out.printf("BBOX ulx %d uly %d; lrx %d lry %d\n", ulx, uly, lrx, lry);
		if (ulx > lrx)
			throw new IllegalArgumentException("ulx > lrx for " + g);
		if (uly > lry)
			throw new IllegalArgumentException("uly > lry for " + g);
		return mx > ulx && my > uly && mx < lrx && my < lry;
	}

}
