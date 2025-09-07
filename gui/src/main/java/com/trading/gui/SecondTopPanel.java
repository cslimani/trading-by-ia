package com.trading.gui;

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
	
	@PostConstruct
	private void initButtons() {
		mainPanel.setSecondTopPanel(this);

		
	}


}
