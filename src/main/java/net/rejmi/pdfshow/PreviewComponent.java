package net.rejmi.pdfshow;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/** Class with a miniature of the slide. Only used if two-monitor mode */
class PreviewComponent extends JComponent {
    private final SwingGUI swingGUI;
    int pageNum;
    float scaleX, scaleY;

    PreviewComponent(SwingGUI swingGUI, float x, float y) {
        this.swingGUI = swingGUI;
        this.scaleX = x;
        this.scaleY = y;
    }

    void setPageNum(int pageNum) {
        this.pageNum = pageNum;
        repaint();
    }

    /** A simple draw component for slide miniatures.
     * NB Page numbers in this code are zero based
     * @param g the <code>Graphics</code> object to draw with
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        try {
            if (swingGUI.currentTab == null) {
                return;
            }
            if (pageNum < 0) {
                return;
            }
            if (pageNum >= swingGUI.currentTab.getPageCount()) {
                return;
            }
            swingGUI.currentTab.renderer.renderPageToGraphics(pageNum, (Graphics2D) g, scaleX, scaleY);
        } catch (IOException e) {
            throw new RuntimeException("Preview Rendering failed: " + e);
        }
    }
}
