package com.trading.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.IntStream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextArea;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trading.enums.EnumTimeRange;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class TopPanel  extends AbstractPanel{

	private static final long serialVersionUID = -8381828449155374906L;
	private JComboBox<EnumTimeRange> timeRangeComboBox;
	private JComboBox<String> marketComboBox;
	private JTextArea positionTextArea;
	private JTextArea distanceTextArea;
	Double detlaTotal = 0d;
	
	@Autowired
	private MainPanel mainPanel;
	private JTextArea backTestInfoArea;
	private Integer nbBOS = 0;
	private JTextArea nbBosTextArea;
	private JTextArea deltaPriceTextArea;
	private JTextArea tradeInfoTextArea;
	private JTextArea nbCandlesArea;
	private JTextArea colorPanel;
	private boolean disableListener;
	private JTextArea indicatorTextArea;
	
	@PostConstruct
	private void initButtons() {
		mainPanel.setTopPanel(this);

//		nbCandlesArea = new JTextArea();
//		nbCandlesArea.setColumns(5);
//		nbCandlesArea.setLocation(20, 0);
//		add(nbCandlesArea);
		
//		nbBosTextArea = new JTextArea();
//		nbBosTextArea.setLocation(20, 0);
//		add(nbBosTextArea);
		
		addButton("Unzoom max", () -> {
			mainPanel.unzoomMaximum();
		});
		
		JComboBox<String> tradeTypeComboBox = new JComboBox<String>(new String[]
				{"SMALL_FALSE_BREAK","FALSE_BREAK", "DOUBLE_FALSE_BREAK", "SUPPLY_MULTI_TF","BOS_SUPPLY"});
		tradeTypeComboBox.addActionListener(_ -> {
			data.setTradeType(tradeTypeComboBox.getItemAt(tradeTypeComboBox.getSelectedIndex()));
		});
		
		timeRangeComboBox = new JComboBox<EnumTimeRange>(EnumTimeRange.values());
		timeRangeComboBox.setSelectedItem(data.getTimeRange());
		timeRangeComboBox.setLocation(20, 20);
		add(timeRangeComboBox);
		timeRangeComboBox.addActionListener(_ -> {
			if (!disableListener) {
				mainPanel.onChangeTimeRange(timeRangeComboBox.getItemAt(timeRangeComboBox.getSelectedIndex()));
			}
		});

		addTimerangeButton(EnumTimeRange.S30);
		addTimerangeButton(EnumTimeRange.M1);
		addTimerangeButton(EnumTimeRange.M5);
		
//		backTestInfoArea = new JTextArea();
//		backTestInfoArea.setLocation(0, 0);
//		add(backTestInfoArea);

		positionTextArea = new JTextArea();
		positionTextArea.setLocation(100, 0);
		positionTextArea.setColumns(20);
		add(positionTextArea);

		indicatorTextArea = new JTextArea();
		indicatorTextArea.setLocation(100, 0);
		indicatorTextArea.setColumns(30);
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
		b.addActionListener(_ -> {
			timeRangeComboBox.setSelectedItem(tr);
		});
		
	}

	public void addButton(String name, Runnable runnable) {
		JButton button = new JButton(name);
		add(button);
		button.setLocation(20, 20);
		button.addActionListener(_ -> {
			runnable.run();
			mainPanel.repaint();
			repaint();
		});
	}
	
	public void changeTimeRange(EnumTimeRange timerange){
		timeRangeComboBox.setSelectedItem(timerange);
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

	public void setNbCandles(int size) {
		nbCandlesArea.setText("Size = " + size);
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

	public void setCandleCounter(String text) {
		nbCandlesArea.setText(text);
	}

	public void updateIndicator(String value) {
		indicatorTextArea.setText(value);
	}

	public void addMarketReady(String market) {
		marketComboBox.addItem(market);
		marketComboBox.setSelectedItem(market);
//		this.repaint();
	}

}
