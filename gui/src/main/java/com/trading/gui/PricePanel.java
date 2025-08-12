package com.trading.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.springframework.stereotype.Component;

import com.trading.utils.AppUtils;

import jakarta.annotation.PostConstruct;

@Component
public class PricePanel extends AbstractPanel{

	private static final long serialVersionUID = -8381828449155374906L;
	private Graphics2D g2d;


	@PostConstruct
	public void init() {
		setPreferredSize(new Dimension(60, MainPanel.WINDOW_HEIGHT));
	}

	private void drawGraph() {
		if (data.getPriceMax() != null) {
			int y = 4;
			while(y < getHeight()) {
				y += 40;
				g2d.setPaint(Color.white);
				g2d.drawString(String.valueOf(AppUtils.getRoundedNumber(pixelToPrice(y), data.getMarket().getScale())), 5, y);
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		doDrawing(g);
	}

	public void doDrawing(Graphics g) {
		setBackground(new Color(21, 33, 38));
		g2d = (Graphics2D) g;

		RenderingHints rh = new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		rh.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHints(rh);
		prepareData();
		drawGraph();
	}
}
