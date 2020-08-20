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

	int x, y, width, height;
	boolean isSelected = false;
	Color color = Color.RED;
	GObject(int x, int y) {
		this.x = x; this.y = y;
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
		return String.format("%s at %d,%x size %d,%d\n", 
			getClass().getSimpleName(), x, y, width, height);
	};
}

class GText extends GObject {
	String text;
	Font font = new Font("Sans", Font.PLAIN, 24);
	GText(int x, int y, String text) {
		super(x, y);
		width = 200;	// XXX Use FontMetrics
		height = 50;
		this.text = text;
	}
	void render(Graphics g) {
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
	GLine(int x, int y, int width, int height) {
		super(x, y);
		this.width = width;
		this.height = height;
	}
	void render(Graphics g) {
		((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
		g.setColor(color);
		g.drawLine(x, y, x + width, y + height);
	}
	@Override
	public String toString() {
		return String.format("%s from %d, %d to %d %d", 
			getClass().getSimpleName(), x, y, width, height);
	}
}

class GMarker extends GLine {
	private static final int MARKER_TRANS_ALPHA = 100;
	GMarker(int x, int y, int endx, int endy) {
		super(x, y, endx, endy);
		lineWidth = 20;
		color = new Color(255, 255, 0, MARKER_TRANS_ALPHA); // Yellow w/ reduced alpha
	}
}

/** Multi-straight-line-segment polyline, no bezier or anything */
class GPolyLine extends GObject {
	final int MAX_POINTS = 250;
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
		((Graphics2D)g).setStroke(new BasicStroke(3));
		g.setColor(color);
		// Translate
		int lastX = x, lastY = y;
		for (int i = 0; i < nPoints; i++) {
			g.drawLine(lastX, lastY, lastX + xPoints[i], lastY + yPoints[i]);
			lastX += xPoints[i+1];
			lastY += yPoints[i+1];
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
		((Graphics2D)g).setStroke(new BasicStroke(3));
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
		((Graphics2D)g).setStroke(new BasicStroke(3));
		g.setColor(color);
		g.drawOval(x, y, width, height);
	}
	
	@Override
	public String toString() {
		return String.format("GOval at %d, %s size %d, %d\n", x, y, width, height);
	}
}
