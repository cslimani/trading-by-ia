package com.trading;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.trading.dto.DataDTO;
import com.trading.entity.Market;
import com.trading.enums.EnumMode;
import com.trading.enums.EnumTimeRange;
import com.trading.gui.MainPanel;
import com.trading.gui.PricePanel;
import com.trading.gui.TopPanel;
import com.trading.repository.MarketRepository;
import com.trading.service.PriceService;

import jakarta.annotation.PostConstruct;

@Component("GuiFrame")
public class GuiFrame extends JFrame {

	private static final long serialVersionUID = 3358629944258036149L;

	private static final int CANDEL_WIDTH = 10;
	public static final int SPACE_BETWEEN_CANDELS = 2;
	@Autowired
	private MainPanel mainPanel;
	@Autowired
	private TopPanel topPanel;
	@Autowired
	private PricePanel pricePanel;
//	@Autowired
//	private LeftPanel leftPanel;
	@Autowired
	private Environment env;
	@Autowired
	private PriceService priceService;
	@Autowired
	private DataDTO data;
	@Autowired
	private MarketRepository marketDAO;
	
	
	public static EnumTimeRange TIME_RANGE = EnumTimeRange.M1;
	public static String MARKET_CODE = "US100";
	
	@PostConstruct
	public void init() throws IOException {
		Market market = marketDAO.getByCode(MARKET_CODE);
		
		String activeProfile = env.getActiveProfiles()[0].toUpperCase();
		data.init(market, CANDEL_WIDTH, SPACE_BETWEEN_CANDELS, TIME_RANGE, EnumMode.valueOf(activeProfile));
		priceService.init(this.getWidth());
		
		topPanel.init();
		setTitle("My Trading Board");
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(MainPanel.WINDOW_WIDTH, MainPanel.WINDOW_HEIGHT+150));  
		JScrollPane scrollPanel = new JScrollPane(mainPanel,  JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPanel.getViewport().setViewPosition(new Point(500, 0));
		mainPanel.init(scrollPanel);
		
		JSplitPane centerAndRightPane = new JSplitPane();
		centerAndRightPane.setLayout(new BoxLayout(centerAndRightPane, BoxLayout.X_AXIS));
		centerAndRightPane.setLeftComponent(scrollPanel);                  // at the top we want our "topPanel"
		centerAndRightPane.setRightComponent(pricePanel); 
		
//		JSplitPane centerAllTogetherPane = new JSplitPane();
//		centerAllTogetherPane.setLayout(new BoxLayout(centerAllTogetherPane, BoxLayout.X_AXIS));
//		centerAllTogetherPane.setLeftComponent(leftPanel);                  // at the top we want our "topPanel"
//		centerAllTogetherPane.setRightComponent(centerAndRightPane); 
		
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setLayout(new BoxLayout(splitPane, BoxLayout.Y_AXIS));
		splitPane.setTopComponent(topPanel);                  // at the top we want our "topPanel"
		splitPane.setBottomComponent(centerAndRightPane); 
		getContentPane().add(splitPane);
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainPanel.moveViewToEnd();
		mainPanel.requestFocusInWindow();
	}

	public void updateTile(String title) {
		setTitle(title);
		repaint();
	}

}
