package net.rejmi.pdfshow;

import java.awt.*;
import javax.swing.*;

public class Gfx {

	/**
	 * Function to add the arrowhead to a line segment.
	 * @author https://stackoverflow.com/questions/3010803/draw-arrow-on-line-algorithm/9295210#9295210
	 * @author Ian Darwin - parameterized more, reversed direction.
	 */
	public static void drawArrowhead(int tipX, int tipY, int tailX, int tailY, Graphics g) {
		int arrowLength = 10;
		int arrowHeadAngle = 30;

		int dx = tipX - tailX;
		int dy = tipY - tailY;

		double theta = Math.atan2(dy, dx);

		double rad = Math.toRadians(arrowHeadAngle);
		double x = tailX + arrowLength * Math.cos(theta + rad);
		double y = tailY + arrowLength * Math.sin(theta + rad);

		double phi2 = Math.toRadians(-arrowHeadAngle);
		double x2 = tailX + arrowLength * Math.cos(theta + phi2);
		double y2 = tailY + arrowLength * Math.sin(theta + phi2);

		int[] arrowYs = new int[3];
		arrowYs[0] = tailY;
		arrowYs[1] = (int) y;
		arrowYs[2] = (int) y2;

		int[] arrowXs = new int[3];
		arrowXs[0] = tailX;
		arrowXs[1] = (int) x;
		arrowXs[2] = (int) x2;

		g.fillPolygon(arrowXs, arrowYs, 3);
	}
}
