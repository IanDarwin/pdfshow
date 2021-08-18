package net.rejmi.pdfshow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.darwinsys.swingui.FontChooser;

/**
 * The Settings panel 
 */
public class Settings extends JPanel {
	private static final long serialVersionUID = 1L;

    public Settings(JFrame jf,
                    Font curFont, Consumer<Font> setFont,
                    Color curColor, Consumer<Color> setColor,
                    int curThickness, Consumer<Integer> setLineThickness,
                    int curSlideTime, Consumer<Integer> setSlideTime,
					boolean curSavePageNumbers, Consumer<Boolean> setSavePageNumbers) {

		// GUI & Actions
		setLayout(new GridLayout(0, 1));
		JButton fontButton = new JButton("Font");
		fontButton.addActionListener(e -> {
			FontChooser fontChooser = new FontChooser(jf);
			fontChooser.setSelectedFont(curFont);
			fontChooser.setVisible(true);
			Font myNewFont = fontChooser.getSelectedFont();
			if (myNewFont != null) {
				setFont.accept(myNewFont);
			};
			fontChooser.dispose();
		});
		add(fontButton);
		JButton colorButton = new JButton("Color");
		colorButton.addActionListener(e -> {
			Color ch = JColorChooser.showDialog(
				jf,             // parent
				"Pick a Drawing Color",   // title
				curColor);
			if (ch != null) {
				setColor.accept(ch);
			}
		});
		add(colorButton);

		JButton linewidthButton = new JButton("Line Thickness");
		// XXX This could be much better - a slider with a live line preview
		linewidthButton.addActionListener(e -> {
			String ret = JOptionPane.showInputDialog("Line Thickness");
			if (ret == null)
				return;
			try {
				setLineThickness.accept(Integer.parseInt(ret.trim()));
			} catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(this,
					String.format(
						"Could not interpret '%s' as a number, alas.", ret),
					"Oops",
					JOptionPane.ERROR_MESSAGE);
			}
		});
		add(linewidthButton);

        JButton slideDelayButton = new JButton("Slide Show Interval");
        // XXX This could be much better - a slider
        slideDelayButton.addActionListener(e -> {
            String ret = JOptionPane.showInputDialog("Slide Show Interval (seconds)");
            if (ret == null)
                return;
            try {
                setSlideTime.accept(Integer.parseInt(ret.trim()));
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this,
                        String.format(
                                "Could not interpret '%s' as a number, alas.", ret),
                        "Oops",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        add(slideDelayButton);

		final JCheckBox memoryBox = new JCheckBox("Restart where left off");
		memoryBox.setSelected(curSavePageNumbers);
		memoryBox.addItemListener(e ->  {
			setSavePageNumbers.accept(memoryBox.isSelected());
		});
		add(memoryBox);
	}
}
