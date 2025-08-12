package com.trading;

import java.awt.EventQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component	
public class TradingGuiRunner implements CommandLineRunner{

	@Autowired
	private GuiFrame guiLauncher;
	
	@Override
	public void run(String... args) throws Exception {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				guiLauncher.setVisible(true);
			}
		});
		
	}

}
