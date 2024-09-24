package net.rejmi.pdfshow;

import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.Serial;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.darwinsys.swingui.FontChooser;
import com.darwinsys.swingui.I18N;

enum SettingType { STRING, INTEGER, BOOLEAN, FONT, COLOR }

record SettingHandler(String buttonName,
					  SettingType type,
					  String dialogLabel,
					  Object value,
					  Consumer<Object> callback){
}

/**
 * The Settings panel 
 */
public class Settings extends JPanel {
	@Serial
	private static final long serialVersionUID = 1L;
	Color curColor;

	/**
	 * Create the GUI for the Settings pane
	 * @param jf The JFrame ready to receive the items
	 * @param handlers Array of SettingHandlers
	 */
	Settings(JFrame jf, SettingHandler... handlers) {

		setBorder(BorderFactory.createTitledBorder("Settings"));
		ResourceBundle rb = ResourceBundle.getBundle("Menus");
		setLayout(new GridLayout(0, 1));

		for (SettingHandler handler : handlers) {
			if (handler instanceof SettingHandler(
					String name, SettingType type, String label,
					Object value, Consumer<Object> mHandler)) {
				switch (type) {
					case STRING:
						throw new UnsupportedOperationException("Not written yet");

					case INTEGER: // USED FOR LINE WIDTH and Slide Interval
						JButton lineWidthButton = I18N.mkButton(rb, name);
						lineWidthButton.addActionListener(e -> showIntSliderDialog(label, mHandler));
						add(lineWidthButton);
						break;
					case BOOLEAN:
						final JCheckBox memoryBox = new JCheckBox(I18N.getString(rb, name, "Boolean"));
						memoryBox.setSelected((Boolean)value);
						memoryBox.addItemListener(e -> mHandler.accept(memoryBox.isSelected()));
						add(memoryBox);
						break;
					case FONT:
						JButton fontButton = I18N.mkButton(rb, name);
						fontButton.addActionListener(e -> {
							FontChooser fontChooser = new FontChooser(jf);
							fontChooser.setSelectedFont((Font) handler.value());
							fontChooser.setVisible(true);
							Font myNewFont = fontChooser.getSelectedFont();
							if (myNewFont != null) {
								 mHandler.accept(myNewFont);
							}
							fontChooser.dispose();
						});
						add(fontButton);
						break;
					case COLOR:
						JButton colorButton = I18N.mkButton(rb, name);
						colorButton.addActionListener(e -> {
							Color ch = JColorChooser.showDialog(
									jf,						// parent
									"Pick a Drawing Color",	// title
									this.curColor);
							if (ch != null) {
								 mHandler.accept(ch);
							}
						});
						add(colorButton);
						break;
					default:
						throw new IllegalStateException(
								"Unknown Handler Type %s".formatted(handler.type()));
				}
			}
		}
	}

	// LINE THICKNESS INTERACTION

	final static int PV_WIDTH = 200, PV_HEIGHT = 35;
	final static int DEFAULT_THICKNESS = 3;

	int lineThickness = DEFAULT_THICKNESS;

	/** Popup dialog for numerical line thickness.
	 * Could be reused for any other int number with
	 * a bit of work to generalize.
	 */
	public void showIntSliderDialog(String label, Consumer<Object> mHandler) {
		JComponent preview = new JComponent() {
			public Dimension getPreferredSize(){
				return new Dimension(PV_WIDTH, PV_HEIGHT);
			}
			public void paintComponent(Graphics g) {
				((Graphics2D)g).setStroke(new BasicStroke(lineThickness));
				g.drawLine(0, PV_HEIGHT/2, PV_WIDTH, PV_HEIGHT/2);
			}
		};

		JSlider slider = new JSlider(JSlider.HORIZONTAL, 1, 10, lineThickness);
		slider.setMajorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				lineThickness = slider.getValue();
				if (Main.debug)
					System.out.println("Int setting now " + lineThickness);
				preview.repaint();
			}
		});

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.NORTH, slider);
		panel.add(BorderLayout.SOUTH, preview);

		int result = 
			JOptionPane.showConfirmDialog(null,
				panel, label, JOptionPane.OK_CANCEL_OPTION);

		if (result == JOptionPane.OK_OPTION) {
			int lineThickness = slider.getValue();
			System.out.println("Line thickness selected: " + lineThickness);
			mHandler.accept(lineThickness);
		}
	}
}
