package net.rejmi.pdfshow;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/**
 * Create a layout like this:
 * +-------------------------+
 * +                         +
 * +          main           +
 * +                         +
 * +-------------------------+
 * +           +             +
 * +  left     +  right      +
 * +           +             +
 * +-------------------------+
 * The constraint names are currently "current", "prev" and "next",
 * reflecting this class' origins in a slide show program.
 */
public class TripartiteLayoutManager implements LayoutManager {
    Component current, prev, next;

    @Override
    public void addLayoutComponent(String name, Component comp) {
        switch(name) {
            case "main": current = comp; break;
            case "left": prev = comp; break;
            case "right": next = comp; break;
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
    }
}
