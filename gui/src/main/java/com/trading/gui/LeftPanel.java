package com.trading.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Box;
import javax.swing.JButton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trading.GuiFrame;
import com.trading.entity.Market;

import jakarta.annotation.PostConstruct;

@Component
public class LeftPanel extends AbstractPanel{

	private static final long serialVersionUID = -8381828449155374906L;
	Graphics2D g2d;
	@Autowired
	MainPanel mainPanel;
	@Autowired
	GuiFrame guiFrame;
	Map<String, JButton> mapMarketButtons = new ConcurrentHashMap<String, JButton>();

	@PostConstruct
	public void init() {
		setPreferredSize(new Dimension(100, MainPanel.WINDOW_HEIGHT));
		//		setLayout(new FlowLayout(FlowLayout.LEFT));
	}

	public void loadAllMarketsFromDatabase() {
		marketDAO.findByEnabled(true).forEach(m -> data.getMapMarkets().put(m.getCode(), m));
		reloadMarkets();
	}

	public synchronized void reloadMarkets() {
		removeAll();
		data.getMapMarkets().values()
		.stream()
		.sorted(Comparator.comparing(Market::getCode))
		.forEach(market -> {
			JButton button = new JButton(market.getCode()
					.replace(".cash", "")
					);
			add(button);
			add(Box.createVerticalStrut(60));
			button.setFont(new Font("Arial", Font.BOLD, 12));
			button.addActionListener(_ -> {
				mainPanel.onChangeMarket(market.getCode());
				updateButtons();
				guiFrame.updateTile(market.getCode());
			});
			mapMarketButtons.put(market.getCode(), button);
		});
		updateButtons();
		repaint();
	}

	public void updateButtons() {
		data.getMapMarkets().values()
		.stream()
		.forEach(market -> {
			JButton button = mapMarketButtons.get(market.getCode());
			if (button != null) {
				String buttonName = market.getCode().replace(".cash", "");
				if (market.isOpenTrade() && market.getRiskRatio() != null) {
					buttonName += " " + market.getRiskRatio();
				}
				button.setText(buttonName);
				if (data.getMarketCode().equals(market.getCode())) {
					button.setBackground(Color.BLUE);
					button.setForeground(Color.WHITE);
				} else if (market.isOpenTrade()){
					button.setBackground(Color.WHITE);
					button.setForeground(Color.RED);
				} else if (market.isAlert()){
					button.setBackground(Color.ORANGE);
					button.setForeground(Color.BLACK);
				}  else {
					button.setBackground(Color.WHITE);
					button.setForeground(Color.BLACK);
				}
			}
		});
		repaint();
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
	}

}
