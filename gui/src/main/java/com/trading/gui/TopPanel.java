package com.trading.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.IntStream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextArea;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trading.GuiFrame;
import com.trading.enums.EnumTimeRange;

import jakarta.annotation.PostConstruct;
import lombok.Data;

@Component
@Data
public class TopPanel  extends AbstractPanel{

	private static final long serialVersionUID = -8381828449155374906L;
	private JComboBox<EnumTimeRange> timeRangeComboBox;
	private JComboBox<String> marketComboBox;
	private JTextArea positionTextArea;
	private JTextArea distanceTextArea;
	DecimalFormat df = new DecimalFormat("#.#");
	Double detlaTotal = 0d;
	
	@Autowired
	private MainPanel mainPanel;
	private JTextArea backTestInfoArea;
	private Integer nbBOS = 0;
	private JTextArea nbBosTextArea;
	private JTextArea deltaPriceTextArea;
	private JTextArea tradeInfoTextArea;
	private JTextArea nbCandelsArea;
	private JTextArea colorPanel;
	private boolean disableListener;
	private JTextArea indicatorTextArea;
	
	@PostConstruct
	private void initButtons() {
		mainPanel.setTopPanel(this);

		colorPanel = new JTextArea();
		colorPanel.setSize(100, 100);
		colorPanel.setText(IntStream.range(1, 30).boxed().map(i -> " ").reduce(" ", (s1,s2) -> s1+ s2));
//		add(colorPanel);
		
		nbCandelsArea = new JTextArea();
		nbCandelsArea.setLocation(20, 0);
		add(nbCandelsArea);
		
		nbBosTextArea = new JTextArea();
		nbBosTextArea.setLocation(20, 0);
		add(nbBosTextArea);
		
		addButton("Unzoom max", () -> {
			mainPanel.unzoomMaximum();
		});
		
		JComboBox<String> tradeTypeComboBox = new JComboBox<String>(new String[]
				{"SMALL_FALSE_BREAK","FALSE_BREAK", "DOUBLE_FALSE_BREAK", "SUPPLY_MULTI_TF","BOS_SUPPLY"});
//		add(tradeTypeComboBox);
		tradeTypeComboBox.addActionListener(e -> {
			JComboBox<String> cb = (JComboBox<String>) e.getSource();
			data.setTradeType((String) cb.getSelectedItem());
		});
		
		timeRangeComboBox = new JComboBox<EnumTimeRange>(EnumTimeRange.values());
		timeRangeComboBox.setSelectedItem(GuiFrame.TIME_RANGE);
		timeRangeComboBox.setLocation(20, 20);
		add(timeRangeComboBox);
		timeRangeComboBox.addActionListener(e -> {
			if (!disableListener) {
				JComboBox<EnumTimeRange> cb = (JComboBox<EnumTimeRange>) e.getSource();
				mainPanel.onChangeTimeRange((EnumTimeRange) cb.getSelectedItem());
			}
		});

//		List<String> markets = marketDAO.findByEnabled(true).stream().map(m -> m.getCode()).sorted().toList();
//		marketComboBox = new JComboBox<String>(markets.toArray(new String[0]));
//		marketComboBox.setSelectedItem(GuiFrame.MARKET_CODE);
//		marketComboBox.setLocation(20, 20);
//		add(marketComboBox);
//		marketComboBox.addActionListener(e -> {
//			JComboBox<String> cb = (JComboBox<String>) e.getSource();
//			mainPanel.onChangeMarket((String) cb.getSelectedItem());
//		});
		
		
//		marketComboBox = new JComboBox<String>();
//		marketComboBox.setLocation(20, 20);
//		add(marketComboBox);
//		marketComboBox.addActionListener(e -> {
//			JComboBox<String> cb = (JComboBox<String>) e.getSource();
//			mainPanel.onChangeMarket((String) cb.getSelectedItem());
//		});
		addTimerangeButton(EnumTimeRange.S15);
		addTimerangeButton(EnumTimeRange.M5);
		
		backTestInfoArea = new JTextArea();
		backTestInfoArea.setLocation(0, 0);
		add(backTestInfoArea);

		positionTextArea = new JTextArea();
		positionTextArea.setLocation(100, 0);
		add(positionTextArea);

		indicatorTextArea = new JTextArea();
		indicatorTextArea.setLocation(100, 0);
		add(indicatorTextArea);
		
		tradeInfoTextArea = new JTextArea();
		tradeInfoTextArea.setLocation(30, 0);
		add(tradeInfoTextArea);
		
		deltaPriceTextArea = new JTextArea();
		deltaPriceTextArea.setLocation(30, 0);
		add(deltaPriceTextArea);
		
		
		distanceTextArea = new JTextArea();
		distanceTextArea.setLocation(200, 200);
		distanceTextArea.setText("");
		add(distanceTextArea);
		
		setTradeInfo("0/3", Color.WHITE);
	}

	private void addTimerangeButton(EnumTimeRange tr) {
		JButton b = new JButton(tr.name());
		b.setLocation(20, 20);
		add(b);
		b.addActionListener(e -> {
			timeRangeComboBox.setSelectedItem(tr);
//			mainPanel.onChangeTimeRange(tr);
		});
		
	}

	public void addButton(String name, Runnable runnable) {
		JButton button = new JButton(name);
		add(button);
		button.setLocation(20, 20);
		button.addActionListener(e -> {
			runnable.run();
			mainPanel.repaint();
			repaint();
		});
	}
	
	public void setTimeRange(EnumTimeRange timerange){
		disableListener = true;
		timeRangeComboBox.setSelectedItem(timerange);
		disableListener = false;
	}

	public void setDateAndPrice(LocalDateTime date, Double price) {
		if (date != null && price != null) {
			positionTextArea.setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "	Price = " + String.format("%.1f", price));	
//			System.out.println(price);
		}
	}

	public void updateDistance(Double absolutePixelToPrice) {
		distanceTextArea.setText(String.format("	GAP = %.1f", absolutePixelToPrice) + " pips");		
	}

	public void init() {
		
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
//		distanceTextArea.setText("	Reward : " + data.getTrade().getReward() + "	");
	}

	public void newBOSSaved(int sentiment) {
		nbBosTextArea.setText("BOS : " + bosFeature.getNbBos());
		Color color = Color.GREEN;
		if (sentiment == 0) {
			color = Color.RED;
		}
		info("=> " + String.valueOf(nbBOS++), color);
		repaint();
	}

	public void showDiff(Double rr, Double bestRR) {
		deltaPriceTextArea.setText("RR : " + String.format("%.1f", rr) + " - Best : " + String.format("%.1f", bestRR));
	}

	public void onNewTrade() {
//		tradeInfoTextArea.setText("Prediction : "  + df.format(data.getTrade().getPrediction()));
	}


	public void info(String message) {
		info(message, Color.GREEN);
	}
	
	public void info(String message, Color color) {
		distanceTextArea.setBackground(color);
		distanceTextArea.setText(message);
		new Thread(() -> {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			distanceTextArea.setText("");
		}).start();
	}

	public void setNbCandels(int size) {
		nbCandelsArea.setText("Size = " + size);
	}

	public void setTradeInfo(String info, Color color) {
		tradeInfoTextArea.setBackground(color);
		tradeInfoTextArea.setText(info);
	}

	public void setColoredPanel(Integer sentiment) {
		if (sentiment != null) {
			if (sentiment == -1) {
				colorPanel.setBackground(Color.RED);
			} else if (sentiment == 0) {
				colorPanel.setBackground(Color.ORANGE);
			} else {
				colorPanel.setBackground(Color.GREEN);
			}
		} else {
			colorPanel.setBackground(Color.GRAY);
		}
	}

	public void setCandelCounter(String text) {
		nbCandelsArea.setText(text);
	}

	public void updateIndicator(Double rsi) {
		indicatorTextArea.setText("RSI = " +rsi.intValue());
	}

	public void addMarketReady(String market) {
		marketComboBox.addItem(market);
		marketComboBox.setSelectedItem(market);
//		this.repaint();
	}

}
