package net.rejmi.pdfshow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PdfShow {
	private static int pageNumber = 0;
	private static PDDocument doc;
	private static JPanel mainPanel;
	private static JButton upButton, downButton;
	private static JTextField pageNumTF;

	public static void main(String[] args) throws IOException {
		File file = new File("/Users/ian/lt2771", "lt2771add.pdf");

		doc = PDDocument.load(file);
		final PDFRenderer renderer = new PDFRenderer(doc);
		JFrame jf = new JFrame("PDFShow");
		jf.setSize(1000,800);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		mainPanel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				try {
					renderer.renderPageToGraphics(pageNumber, (Graphics2D) g);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(jf, "Failure: " + e);
				}
			}
		};
		mainPanel.setSize(800, 800);
		jf.add(BorderLayout.CENTER, mainPanel);

		JPanel toolbox = new JPanel();
		toolbox.setBackground(Color.cyan);
		toolbox.setPreferredSize(new Dimension(200, 800));
		upButton = new JButton("Up");
		upButton.addActionListener(e -> moveToPage(pageNumber - 1));
		toolbox.add(upButton);
		downButton = new JButton("Down");
		downButton.addActionListener(e -> moveToPage(pageNumber + 1));
		toolbox.add(downButton);
		pageNumTF = new JTextField("0  ");
		pageNumTF.addActionListener(e -> moveToPage(Integer.parseInt(pageNumTF.getText())));
		toolbox.add(pageNumTF);

		moveToPage(0);

		jf.add(BorderLayout.WEST, toolbox);
		jf.setVisible(true);
	}

	private static void moveToPage(int newPage) {
		if (newPage < 0) {
			newPage = 0;
		}
		if (newPage >= doc.getNumberOfPages()) {
			newPage = doc.getNumberOfPages() - 1;
		}
		if (newPage == pageNumber) {
			return;
		}
		pageNumTF.setText(Integer.toString(newPage));
		pageNumber = newPage;
		upButton.setEnabled(pageNumber > 0);
		downButton.setEnabled(pageNumber < doc.getNumberOfPages());
		mainPanel.repaint();
	}
}
