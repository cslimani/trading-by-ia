package com.trading.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JToolBar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class SecondTopPanel  extends AbstractPanel{

	private static final long serialVersionUID = -8381828449155374906L;
	
	@Autowired
	private MainPanel mainPanel;

	private JToolBar toolBar;
	
	@PostConstruct
	private void initButtons() {
		mainPanel.setSecondTopPanel(this);
		toolBar = new JToolBar(JToolBar.HORIZONTAL);
		toolBar.setFloatable(false); 
		add(toolBar, BorderLayout.NORTH);
	}

	public void addLabel(JButton button) {
		toolBar.add(button);		
	}
	
	public void addSeparator(int length) {
		toolBar.addSeparator(new Dimension(length, 1));
	}


}
