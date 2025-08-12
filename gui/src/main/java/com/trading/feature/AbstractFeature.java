package com.trading.feature;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonConfig;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.trading.GuiFrame;
import com.trading.component.AlertComponent;
import com.trading.component.BacktestComponent;
import com.trading.component.HorizontalLineComponent;
import com.trading.component.ZoneSelectorComponent;
import com.trading.dto.DataDTO;
import com.trading.gui.LeftPanel;
import com.trading.gui.MainPanel;
import com.trading.gui.TopPanel;
import com.trading.metatrader.MetaBrokerApi;
import com.trading.repository.AccumulationRespository;
import com.trading.repository.AlertRepository;
import com.trading.repository.CandelRepository;
import com.trading.repository.MarketRepository;
import com.trading.repository.MomentOfInterestRespository;
import com.trading.repository.PointOfInterestRespository;
import com.trading.repository.TickPriceRepository;
import com.trading.repository.TradeRepository;
import com.trading.repository.TrainablePatternRepository;
import com.trading.repository.TrainingTradeRepository;
import com.trading.service.CandelManager;
import com.trading.service.LevelService;
import com.trading.service.PriceHolderService;
import com.trading.service.PriceService;
import com.trading.service.PriceStreamingClient;
import com.trading.service.TradeService;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

@Slf4j
public class AbstractFeature {

	@Autowired
	public TrainablePatternRepository trainablePatternRepository;
	@Autowired
	public PointOfInterestRespository poiRespository;
	@Autowired
	public DataDTO data;
	@Autowired
	public MainPanel mainPanel;
	@Autowired
	public TopPanel topPanel;
	@Autowired
	public LeftPanel leftPanel;
	@Autowired
	public GuiFrame guiFrame;
	@Autowired
	public CandelRepository candelRepository;
	@Autowired
	public MomentOfInterestRespository moiRespository;
	@Autowired
	public ZoneSelectorComponent zoneSelectorComponent;
	@Autowired
	public AlertComponent alertComponent;
	@Autowired
	public TrainablePatternComponent trainablePatternComponent;
	@Autowired
	public ObjectMapper objectMapper;
	@Autowired 
	public PlatformTransactionManager transactionManager;
	@Autowired
	public MarketRepository marketRepository;
	@Autowired
	public LevelService levelService;
	@Autowired
	public TrainingTradeRepository trainingTradeRepository;
	@Autowired
	public TickPriceRepository tickPriceRepository;
	@Autowired
	AccumulationRespository accumulationRespository;
	@Autowired
	public PriceService priceService;
	@Autowired
	public PriceHolderService priceHolderService;
	@Autowired
	public PriceStreamingClient priceStreamingClient;
	@Autowired
	public MetaBrokerApi metaBrokerApi;
	@Autowired
	public TradeService tradeService;
	@Autowired
	public TradeRepository tradeRepository;
	@Autowired
	public CandelManager candelManager;
	@Autowired
	public AlertRepository alertRepository;
	@Autowired
	public BacktestComponent backtestComponent;
	@Autowired
	public HorizontalLineComponent horizontalLineComponent;
	
	@Getter
	@Setter
	public boolean enabled;
	
	public OkHttpClient client = new OkHttpClient.Builder()
			.connectTimeout(10, TimeUnit.SECONDS)
			.writeTimeout(10, TimeUnit.SECONDS)
			.readTimeout(300, TimeUnit.SECONDS)
			.build();
	
	public void transactionnal(Runnable runnable) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				runnable.run();
			}
		});
	}
	
	public void warn(String message) {
		topPanel.info(message, Color.red);
	}
	
	public void addButton(String name, Runnable runnable, boolean reload) {
		addButton(name, runnable, reload, null);
	}
	
	public void addButton(String name, Runnable runnable, boolean reload, boolean unzoom) {
		addButton(name, runnable, reload, unzoom, null);
	}
	
	public void addButton(String name, Runnable runnable, boolean reload, Runnable after) {
		addButton(name, runnable, reload, false, after);
	}
	public void addButton(String name, Runnable runnable, boolean reload, boolean unzoom, Runnable after) {
		JButton button = new JButton(name);
		topPanel.add(button);
		button.setLocation(20, 20);
		button.addActionListener(e -> {
			runnable.run();
			mainPanel.repaint();
			topPanel.repaint();
			if (reload) {
				mainPanel.reload();
			}
			if (unzoom) {
				mainPanel.unzoomMaximum();
			}
			if (after !=null) {
				after.run();
			}
		});
	}
	
	public static void plot(List<? extends Number> values) {
		Plot plt = Plot.create(PythonConfig.pythonBinPathConfig("/usr/bin/python3"));
		plt.plot().add(values);
		plt.legend().loc("upper right");
		try {
			plt.show();
		} catch (IOException | PythonExecutionException e) {
			e.printStackTrace();
		}
	}

	public static void plot(List<? extends Number> values, Number min, Number max) {
		Plot plt = Plot.create(PythonConfig.pythonBinPathConfig("/usr/bin/python3"));
		plt.plot().add(values);
		plt.ylim(min, max);
		plt.legend().loc("upper right");
		try {
			plt.show();
		} catch (IOException | PythonExecutionException e) {
			e.printStackTrace();
		}
	}
}
