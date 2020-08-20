package net.rejmi.pdfshow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/** This represents additions that we make to the PDF.
 * In the present version they are not saved with the PDF!
 */
abstract class GObject {
	/** pdfbox leaves the Graphics object in upside down mode */
	static final AffineTransform UPRIGHT_TRANSLATE_INSTANCE = AffineTransform.getTranslateInstance(1, -1);

	int x, y, maxX, maxY;	// ULX, ULY, LRX, LRY!
	Color color = Color.RED;
	GObject(int x, int y) {
		this.x = x; this.y = y;
	}
	abstract void render(Graphics g);
}

class GText extends GObject {
	String text;
	Font font = new Font("Sans", Font.PLAIN, 24);
	GText(int x, int y, String text) {
		super(x, y);
		maxX = x + 200;	// XXX Use FontMetrics
		maxY = y + 50;
		this.text = text;
	}
	void render(Graphics g) {
		((Graphics2D)g).setTransform(UPRIGHT_TRANSLATE_INSTANCE);
		g.setColor(color);
		g.setFont(font);
		g.drawString(text, x, y);
	}
	@Override
	public String toString() {
		return String.format("GText: %d, %d, %s", x, y, text);
	}
}

class GLine extends GObject {
	int lineWidth = 3;
	GLine(int x, int y, int endX, int endY) {
		super(x, y);
		this.maxX = endX;
		this.maxY = endY;
	}
	void render(Graphics g) {
		((Graphics2D)g).setTransform(UPRIGHT_TRANSLATE_INSTANCE);
		((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
		g.setColor(color);
		g.drawLine(x, y, maxX, maxY);
	}
	@Override
	public String toString() {
		return String.format("%s from %d, %d to %d %d", 
			getClass().getSimpleName(), x, y, maxX, maxY);
	}
}

class GMarker extends GLine {
	private static final int MARKER_TRANS_ALPHA = 100;
	GMarker(int x, int y, int endx, int endy) {
		super(x, y, endx, endy);
		lineWidth = 15;
		color = new Color(255, 255, 0, MARKER_TRANS_ALPHA); // Yellow w/ reduced alpha
	}
}

class GPolyLine extends GObject {
	final int MAX_POINTS = 250;
	private int[] xPoints = new int[MAX_POINTS], yPoints = new int[MAX_POINTS];
	private int nPoints;
	GPolyLine(int x, int y) {
		super(x, y);
		if (xPoints.length != yPoints.length)
			throw new IllegalArgumentException("GPolyLine(): xlen != ylen");
	}
	
	void addPoint(int x, int y) {
		if (nPoints == xPoints.length) {
			throw new IllegalStateException("I never thought you'd draw a line with that many points");
		}
		this.xPoints[nPoints] = x;
		maxX = Math.max(x, maxX);
		this.yPoints[nPoints] = y;
		maxY = Math.max(y, maxY);
		++nPoints;
	}
	
	void render(Graphics g) {
		((Graphics2D)g).setTransform(UPRIGHT_TRANSLATE_INSTANCE);
		((Graphics2D)g).setStroke(new BasicStroke(3));
		g.setColor(color);
		g.drawPolyline(xPoints, yPoints, nPoints);
	}

	public int length() {
		return nPoints;
	}
	
	@Override
	public String toString() {
		return String.format("GPolyLine %d points from %d, %d to %d %d", nPoints, x, y, xPoints[nPoints - 1], yPoints[nPoints - 1]);
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
	GRectangle(int ulX, int ulY, int maxX, int maxY) {
		super(ulX, ulY);
		this.maxX = maxX;
		this.maxY = maxY;
	}
	void render(Graphics g) {
		((Graphics2D)g).setTransform(UPRIGHT_TRANSLATE_INSTANCE);
		((Graphics2D)g).setStroke(new BasicStroke(3));
		g.setColor(color);
		g.drawRect(x, y, Math.abs(maxX - x), Math.abs(maxY - y));
	}
}

class GOval extends GObject {
	GOval(int ulX, int ulY, int maxX, int maxY) {
		super(ulX, ulY);
		this.maxX = maxX;
		this.maxY = maxY;
	}
	void render(Graphics g) {
		((Graphics2D)g).setTransform(UPRIGHT_TRANSLATE_INSTANCE);
		((Graphics2D)g).setStroke(new BasicStroke(3));
		g.setColor(color);
		g.drawOval(x, y, Math.abs(maxX - x), Math.abs(maxY - y));
	}
}
