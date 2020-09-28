package net.rejmi.pdfshow;

public class Containment {
	
	static boolean contains(GObject g, int mx, int my) {
		System.out.printf("Contains.contains(%s, mx %d, my %d)\n", g, mx, my);
		int ulx, uly, lrx, lry;
		ulx = uly = lrx = lry = 0; // REMOVE BEFORE COMMIT
		if (g.width < g.x && g.height < g.y) {
			// UL upper left quadrant: width, height both negative
			ulx = g.x + g.width; // g.width is negative
			uly = g.y + g.height;
			lrx = g.x;
			lry = g.y;
		}
		if (g.width > g.x && g.height < g.y) {
			// UR upper right quadrant: width positive, height negative
			ulx = g.x;
			uly = g.y - g.height;
			lrx = g.x + g.width;
			lry = g.y;
			return mx > g.x && mx <= (g.x + g.width) &&
					my < g.y && my >= (-g.y - -g.height);
		}
		if (g.width < g.x && g.height > g.y) {
			// LL lower left quadrant: width negative, height positive
			ulx = -g.x - -g.width;
			uly = g.y;
			lrx = g.x;
			lry = g.y - -g.height;
		}
		if (g.width >= g.x && g.height > g.y) {
			// LR lower right quadrant: width, height both positive
			ulx = g.x;
			ulx = g.y;
			lrx = g.x + g.width;
			lry = g.y + g.height;
		}
		if (ulx > lrx)
			throw new IllegalArgumentException("ulx > lrx");
		if (uly > lry)
			throw new IllegalArgumentException("uly > lry");
		System.out.printf("BBOX ulx %d uly %d; lrx %d lry %d\n", ulx, uly, lrx, lry);
		return mx > ulx && my > uly && mx < lrx && my < lry;
	}

}
