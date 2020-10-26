package net.rejmi.pdfshow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;

/** This represents additions that we make to the PDF.
 * In the present version they are not saved with the PDF!
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

	int x, y, width, height;
	boolean isSelected = false;

	// Class Drawing Parameters - constructors should save what they need!
	static Color curColor = Color.RED;
	static Font curFont = new Font("Sans", Font.PLAIN, 24);
	static int curLineThickness = 3;

	// These ones we do for you
	Color color;
	int lineThickness;

	GObject(int x, int y) {
		this(x, y, 0, 0);
	}

	GObject(int x, int y, int width, int height) {
		this.x = x; this.y = y;
		this.width = width; this.height = height;
		color = curColor;
		lineThickness = curLineThickness;
	}

	public static Color getColor() {
		return curColor;
	}
	public static void setColor(Color color) {
		GObject.curColor = color;
	}
	public static Font getFont() {
		return curFont;
	}
	public static void setFont(Font font) {
		GObject.curFont = font;
	}
	public static int getLineThickness() {
		return curLineThickness;
	}
	public static void setLineThickness(int lineWidth) {
		GObject.curLineThickness = lineWidth;
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
	
	public String toString() {
		return String.format("%s at %d,%x size %d,%d", 
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
		g.setColor(color);
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
		g.setColor(color);
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
		color = new Color(255, 255, 0, MARKER_TRANS_ALPHA); // Yellow w/ reduced alpha
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
		g.setColor(color);
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
		g.setColor(color);
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
		g.setColor(color);
		g.drawOval(x, y, width, height);
	}
}
