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
import javax.swing.JPanel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PdfShow {
	private static int pageNumber = 0;

	public static void main(String[] args) throws IOException {
		File file = new File("/Users/ian/lt2771", "lt2771add.pdf");

		PDDocument doc = PDDocument.load(file);
		final PDFRenderer renderer = new PDFRenderer(doc);
		JFrame jf = new JFrame("PDFShow");
		jf.setSize(1000,800);
		
		JPanel mainPanel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				try {
					renderer.renderPageToGraphics(pageNumber, (Graphics2D) g);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		};
		mainPanel.setSize(800, 800);
		jf.add(BorderLayout.CENTER, mainPanel);
		JPanel toolbox = new JPanel();
		toolbox.setBackground(Color.cyan);
		toolbox.setPreferredSize(new Dimension(200, 800));
		JButton upButton = new JButton("Up");
		upButton.addActionListener(e -> { pageNumber = Math.max(0, pageNumber - 1);
			mainPanel.repaint();
			});
		toolbox.add(upButton);
		JButton downButton = new JButton("Down");
		downButton.addActionListener(e -> { pageNumber = Math.min(doc.getNumberOfPages(), pageNumber + 1);
			mainPanel.repaint();
			});
		toolbox.add(downButton);
		jf.add(BorderLayout.WEST, toolbox);
		jf.setVisible(true);
	}
}
