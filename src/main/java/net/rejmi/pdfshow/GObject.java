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

	int x, y;
	Color color;
	GObject(int x, int y) {
		this.x = x; this.y = y;
	}
	abstract void render(Graphics g);
}

class GText extends GObject {
	String text;
	Font font = new Font("Sans", Font.PLAIN, 18);
	GText(int x, int y, String text) {
		super(x, y);
		this.text = text;
	}
	void render(Graphics g) {
		((Graphics2D)g).setTransform(UPRIGHT_TRANSLATE_INSTANCE);
		g.setColor(Color.red);
		g.setFont(font);
		g.drawString(text, x, y);
	}
}

class GLine extends GObject {
	int lineWidth;
	int endX, endY;
	GLine(int x, int y, int endX, int endY) {
		super(x, y);
		this.endX = endX;
		this.endY = endY;
	}
	void render(Graphics g) {
		((Graphics2D)g).setTransform(UPRIGHT_TRANSLATE_INSTANCE);
		((Graphics2D)g).setStroke(new BasicStroke(3));
		g.setColor(Color.RED);
		g.drawLine(x, y, endX, endY);
	}
}

class GPolyLine extends GObject {
	private int[] xPoints, yPoints;
	GPolyLine(int[] xPoints, int[] yPoints) {
		super(xPoints[0], yPoints[0]);
		if (xPoints.length != yPoints.length)
			throw new IllegalArgumentException("GPolyLine(): xlen != ylen");
		this.xPoints = xPoints;
		this.yPoints = yPoints;
	}
	void render(Graphics g) {
		((Graphics2D)g).setTransform(UPRIGHT_TRANSLATE_INSTANCE);
		((Graphics2D)g).setStroke(new BasicStroke(3));
		g.setColor(Color.RED);
		g.drawPolyline(xPoints, yPoints, xPoints.length);
	}
}

class GRectangle extends GObject {
	private int llX, llY;
	GRectangle(int ulX, int ulY, int llX, int llY) {
		super(ulX, ulY);
		this.llX = llX;
		this.llY = llY;
	}
	void render(Graphics g) {
		((Graphics2D)g).setTransform(UPRIGHT_TRANSLATE_INSTANCE);
		((Graphics2D)g).setStroke(new BasicStroke(3));
		g.setColor(Color.RED);
		g.drawRect(x, y, Math.abs(llX - x), Math.abs(llY - y));
	}
}
