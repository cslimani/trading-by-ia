package com.trading;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trading.dto.DataDTO;
import com.trading.gui.MainPanel;
import com.trading.gui.PricePanel;
import com.trading.gui.SecondTopPanel;
import com.trading.gui.TopPanel;
import com.trading.service.PriceService;

import jakarta.annotation.PostConstruct;

@Component("GuiFrame")
public class GuiFrame extends JFrame {

	private static final long serialVersionUID = 3358629944258036149L;

	private static final int candle_WIDTH = 10;
	public static final int SPACE_BETWEEN_CANDLES = 2;
	@Autowired
	private MainPanel mainPanel;
	@Autowired
	private TopPanel topPanel;
	@Autowired
	SecondTopPanel secondTopPanel;
	@Autowired
	private PricePanel pricePanel;
	//	@Autowired
	//	private LeftPanel leftPanel;
	@Autowired
	private PriceService priceService;
	@Autowired
	private DataDTO data;


	//	public static EnumTimeRange TIME_RANGE = EnumTimeRange.M1;
	//	public static String MARKET_CODE = "US100";

	@PostConstruct
	public void init() throws IOException {
		data.init(candle_WIDTH, SPACE_BETWEEN_CANDLES);
		priceService.init(this.getWidth());

		topPanel.init();
		setTitle("My Trading Board");
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(MainPanel.WINDOW_WIDTH, MainPanel.WINDOW_HEIGHT));  
		JScrollPane scrollPanel = new JScrollPane(mainPanel,  JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPanel.getViewport().setViewPosition(new Point(500, 0));
		mainPanel.init(scrollPanel);

		JSplitPane centerAndRightPane = new JSplitPane();
		centerAndRightPane.setLayout(new BoxLayout(centerAndRightPane, BoxLayout.X_AXIS));
		centerAndRightPane.setLeftComponent(scrollPanel);                  // at the top we want our "topPanel"
		centerAndRightPane.setRightComponent(pricePanel); 

		// conteneur qui regroupe les 2 lignes de boutons
		JPanel topContainer = new JPanel();
		topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
		topContainer.add(topPanel);
		topContainer.add(secondTopPanel);


		JSplitPane splitPane = new JSplitPane();
		splitPane.setLayout(new BoxLayout(splitPane, BoxLayout.Y_AXIS));
		splitPane.setTopComponent(topContainer);                  // at the top we want our "topPanel"
		splitPane.setBottomComponent(centerAndRightPane); 
		getContentPane().add(splitPane);
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainPanel.moveViewToEnd();
		mainPanel.requestFocusInWindow();

		priceService.postInit(this.getWidth());
	}

	public void updateTile(String title) {
		setTitle(title);
		repaint();
	}

	

}
