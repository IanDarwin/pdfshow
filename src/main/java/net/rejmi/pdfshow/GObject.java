package net.rejmi.pdfshow;

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
		g.drawLine(x, y, endX, endY);
	}
}
