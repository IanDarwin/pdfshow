package net.rejmi.pdfshow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.util.logging.Logger;

/** 
 * This class hierarchy represents additions that we make to the PDF.
 * They are not saved with the PDF (yet).
 */
abstract class GObject {
	/** pdfbox leaves the Graphics object in upside down mode */
	// Special care needed so we can run on high-res displays like Retina
	static final AffineTransform UPRIGHT_TRANSLATE_INSTANCE;
	static {
		AffineTransform defaultTransform= 
			GraphicsEnvironment
				.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice()
				.getDefaultConfiguration()
				.getDefaultTransform();
		UPRIGHT_TRANSLATE_INSTANCE = 
			new AffineTransform(defaultTransform);
	}
	
	private static Logger logger;
	static {
		// Configure logging
		LoggerSetup.init();
		logger = Logger.getLogger("pdfshow.gobject");
	}

	int x, y, width, height;
	boolean isSelected = false;

	// Class Drawing Parameters - constructors should save what they need!
	// Color starts with neutral blue - save Red for important stuff
	static Color curLineColor = new Color(0x3399FF),
		curFillColor = new Color(0x00000000);	// Transparent(?)
	static Font curFont = new Font("Sans", Font.PLAIN, 24);
	static int curLineThickness = 3;

	// These are acquired at construction time
	Color lineColor, fillColor;
	int lineThickness;

	GObject(int x, int y) {
		this(x, y, 0, 0);
	}

	GObject(int x, int y, int width, int height) {
		this.x = x; this.y = y;
		this.width = width; this.height = height;
		lineColor = curLineColor;
		fillColor = curFillColor;
		lineThickness = curLineThickness;
	}

	public static Color getLineColor() {
		return curLineColor;
	}
	public static void setLineColor(Object color) {
		GObject.curLineColor = (Color)color;
	}

	public static Color getFillColor() {
		return curFillColor;
	}
	public static void setFillColor(Object color) {
		GObject.curFillColor = (Color)color;
	}

	public static Font getFont() {
		return curFont;
	}
	public static void setFont(Object font) {
		GObject.curFont = (Font)font;
	}
	public static int getLineThickness() {
		return curLineThickness;
	}
	public static void setLineThickness(Object lineWidth) {
		GObject.curLineThickness = (int) lineWidth;
	}
	abstract void render(Graphics g);
	
	void draw(Graphics g) {
		((Graphics2D)g).setTransform(UPRIGHT_TRANSLATE_INSTANCE);
		render(g);
		if (isSelected) {
			((Graphics2D)g).setStroke(new BasicStroke(2));
			g.setColor(Color.BLACK);
			g.drawRect(x-2, y-2, width + 2, height + 2);
		}
	}
	
	/**
	 * Determine if the given object contains the given point
	 * (which is usually a mouse click location).
	 * @param mx The point's X coordinate
	 * @param my The point's Y coordinate
	 * @return True if the point is contained within the given GObject's bbox
	 */
	public boolean contains(final int mx, final int my) {
		GObject g = this;
		int ulx = 0, uly = 0, lrx = 0, lry = 0;
		if (g.width == 0 && g.height == 0) {
			return false; // Too small to contain anything
		}
		logger.info(String.format("contains(%s, mx %d, my %d)", g, mx, my));
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
		logger.info(String.format("BBOX ulx %d uly %d; lrx %d lry %d; Quadrant %d", ulx, uly, lrx, lry, quad));
		if (ulx > lrx)
			throw new IllegalArgumentException("ulx > lrx for " + g);
		if (uly > lry)
			throw new IllegalArgumentException("uly > lry for " + g);
		return mx > ulx && my > uly && mx < lrx && my < lry;
	}
	
	public String toString() {
		return String.format("%s(loc = %d,%d size = %d,%d)",
			getClass().getSimpleName(), x, y, width, height);
	};
}

class GText extends GObject {
	String text;
	Font font;
	GText(int x, int y, String text) {
		super(x, y);
		width = 200;	// XXX Use FontMetrics
		height = -50;	// Should negate, as x,y is baseline loc(?)
		this.text = text;
		font = curFont;
	}
	void render(Graphics g) {
		g.setColor(curLineColor);
		g.setFont(font);
		g.drawString(text, x, y);
	}
	@Override
	public String toString() {
		return String.format("GText: x %d, y %d, w %d, h %d %s", x, y, width, height, text);
	}
}

class GLine extends GObject {
	GLine(int x, int y, int width, int height) {
		super(x, y);
		this.width = width;
		this.height = height;
	}
	void render(Graphics g) {
		((Graphics2D)g).setStroke(new BasicStroke(lineThickness));
		g.setColor(lineColor);
		g.drawLine(x, y, x + width, y + height);
	}
	@Override
	public String toString() {
		return String.format("%s from %d, %d size %d %d", 
			getClass().getSimpleName(), x, y, width, height);
	}
}

class GMarker extends GLine {
	private static final int MARKER_TRANS_ALPHA = 100;
	GMarker(int x, int y, int endx, int endy) {
		super(x, y, endx, endy);
		lineThickness = 20;
		lineColor = new Color(255, 255, 0, MARKER_TRANS_ALPHA); // Yellow w/ reduced alpha
	}
}

/** Multi-straight-line-segment polyline, no bezier or anything */
class GPolyLine extends GObject {
	final int MAX_POINTS = 375;
	private int[] xPoints = new int[MAX_POINTS], yPoints = new int[MAX_POINTS];
	private int nPoints = 0;
	GPolyLine(int x, int y) {
		super(x, y);
	}
	
	void addPoint(int x, int y) {
		if (nPoints == xPoints.length) {
			throw new IllegalStateException("I never thought you'd draw a line with that many points");
		}
		this.xPoints[nPoints] = x;
		width = Math.max(x + width, width);
		this.yPoints[nPoints] = y;
		height = Math.max(y + height, height);
		++nPoints;
	}
	
	void render(Graphics g) {
		((Graphics2D)g).setStroke(new BasicStroke(lineThickness));
		g.setColor(curLineColor);
		// Translate
		int lastX = x, lastY = y;
		for (int i = 0; i < nPoints; i++) {
			g.drawLine(lastX, lastY, lastX + xPoints[i], lastY + yPoints[i]);
			lastX += xPoints[i];
			lastY += yPoints[i];
		}
	}

	public int length() {
		return nPoints;
	}
	
	@Override
	public String toString() {
		return String.format(
			"GPolyLine %d points from %d, %d size %d %d", nPoints, x, y, width, height);
	}

	// Only for testing
	int getX(int i) {
		return xPoints[i];
	}
	int getY(int i) {
		return yPoints[i];
	}
}

class GRectangle extends GObject {
	GRectangle(int ulX, int ulY, int width, int height) {
		super(ulX, ulY);
		this.width = width;
		this.height = height;
	}
	void render(Graphics g) {
		((Graphics2D)g).setStroke(new BasicStroke(lineThickness));
		g.setColor(curLineColor);
		g.drawRect(x, y, width, height);
	}
}

class GOval extends GObject {
	GOval(int ulX, int ulY, int width, int height) {
		super(ulX, ulY);
		this.width = width;
		this.height = height;
	}
	void render(Graphics g) {
		((Graphics2D)g).setStroke(new BasicStroke(lineThickness));
		g.setColor(curLineColor);
		g.drawOval(x, y, width, height);
	}
}
