package com.trading.feature;

import java.awt.Color;

import javax.swing.JButton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.GuiFrame;
import com.trading.component.HorizontalLineComponent;
import com.trading.component.ZoneSelectorComponent;
import com.trading.dto.DataDTO;
import com.trading.gui.LeftPanel;
import com.trading.gui.MainPanel;
import com.trading.gui.TopPanel;
import com.trading.repository.CandelRepository;
import com.trading.repository.MarketRepository;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AbstractFeature {

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
	public ZoneSelectorComponent zoneSelectorComponent;
	@Autowired
	public ObjectMapper objectMapper;
	@Autowired 
	public PlatformTransactionManager transactionManager;
	@Autowired
	public MarketRepository marketRepository;
	@Autowired
	public HorizontalLineComponent horizontalLineComponent;
	
	@Getter
	@Setter
	public boolean enabled;
	
	
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
		button.addActionListener(_ -> {
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
	
}
