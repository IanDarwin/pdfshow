package net.rejmi.pdfshow;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

public class TripartiteLayoutManager implements LayoutManager {
    Component current, prev, next;

    @Override
    public void addLayoutComponent(String name, Component comp) {
        System.out.println("TripartiteLayoutManager.addLayoutComponent: " + name + " " + comp);
        switch(name) {
            case "current": current = comp; break;
            case "prev": prev = comp; break;
            case "next": next = comp; break;
            default: throw new IllegalArgumentException("Constraint must be in {current,prev,next}");
        }
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(800, 700);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(300, 200);
    }

    @Override
    public void layoutContainer(Container parent) {
        if (current == null || prev == null || next == null) {
            throw new IllegalArgumentException("Must provide three miniatures");
        }
        Dimension totalSize = parent.getSize();
        int dx = totalSize.width / 2;
        int dy = totalSize.height / 2;

        current.setBounds(0, 0, totalSize.width, dy);

        prev.setBounds(0, dy, dx, dy);

        next.setBounds(dx, dy, dx, dy);
        System.out.printf("Parent size %s dx,dy=%d,%d, Current: %s\n\tPrev: %s\n\tNext: %s\n",
                totalSize, dx, dy, current, prev, next);
    }
}
