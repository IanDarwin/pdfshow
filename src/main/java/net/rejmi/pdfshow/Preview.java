package net.rejmi.pdfshow;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class Preview extends JPanel {
    private final SwingGUI swingGUI;
    PreviewComponent current, prev, next;
    private int pageNumber;

    Preview(SwingGUI swingGUI) {
        this.swingGUI = swingGUI;
        setLayout(new TripartiteLayoutManager());

        current = new PreviewComponent(swingGUI, 0.6f, 0.6f);
        add("main", current);

        prev  = new PreviewComponent(swingGUI, 0.35f, 0.35f);
        prev.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (pageNumber > 1)
                    swingGUI.currentTab.gotoPage(pageNumber - 1);
            }
        });
        add("left", prev);

        next  = new PreviewComponent(swingGUI, 0.35f, 0.35f);
        next.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (pageNumber < swingGUI.currentTab.getPageCount() - 1)
                    swingGUI.currentTab.gotoPage(pageNumber + 1);
            }
        });
        add("right", next);
    }

    void setPageNumber(int pageNum) {
        this.pageNumber = pageNum;
        pageNum--; // 1-based to 0-based
        current.setPageNum(pageNum);
        prev.setPageNum(pageNum - 1);
        next.setPageNum(pageNum + 1);
        repaint();
    }
}
