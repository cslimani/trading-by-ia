package com.trading.component;

import org.springframework.beans.factory.annotation.Autowired;

import com.trading.GuiFrame;
import com.trading.dto.DataDTO;
import com.trading.gui.LeftPanel;
import com.trading.gui.MainPanel;
import com.trading.gui.TopPanel;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractFeature {

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
	
	@Getter
	@Setter
	public boolean enabled;
}
