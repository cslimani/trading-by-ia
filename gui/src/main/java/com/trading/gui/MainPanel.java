package com.trading.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.trading.dto.GraphDateDTO;
import com.trading.dto.HorizontalLine;
import com.trading.dto.PricePointDTO;
import com.trading.entity.Candle;
import com.trading.enums.EnumTimeRange;

@Component("MainPanel")
public class MainPanel extends AbstractPanel implements Scrollable, ActionListener, MouseListener, 
MouseMotionListener, MouseWheelListener, KeyListener, ChangeListener  {

	public static final int WINDOW_WIDTH = 1900;
	public static final int WINDOW_HEIGHT = 900;
	public static final int EXTRA_GHOST_candleS = 10;
	static final String LOCK = "LOCK";

	static final long serialVersionUID = -2520656747519200912L;
	@Autowired
	PricePanel pricePanel;

	Graphics2D g2d;
	JScrollPane scrollPane;

	boolean isBusy;
	TopPanel topPanel;
	SecondTopPanel secondTopPanel;

	public void init(JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
		scrollPane.getViewport().addChangeListener(this);
		repaint();
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addMouseWheelListener(this);
		this.addKeyListener(this);
		this.setFocusable(true);
	}

	private synchronized void doDrawing(Graphics g) throws JsonMappingException, JsonProcessingException {
		setBackground(new Color(21, 33, 38));
		g2d = (Graphics2D) g;

		RenderingHints rh = new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		rh.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHints(rh);

		prepareData();
		drawGrid();
		drawCandles();
		drawHorizontalLines();
		drawVerticalLines();
		horizontalLineComponent.draw(g2d);
		if (!data.isAutoZoomDone()) {
			data.setAutoZoomDone(true);
			int size = data.getViewTotalWidth();
			while (size < scrollPane.getWidth()) {
				size = zoom(true);
			}
		}
	}

	private void drawVerticalLines() {
		verticalLineComponent.draw(g2d);		
	}

	private void drawHorizontalLines() {
		if (data.getPricesToDraw() == null) {
			return;
		}
		for (HorizontalLine line : data.getPricesToDraw()) {
			g2d.setColor(Color.decode(line.getColor()));
			int y = priceToYPixel(line.getPrice());
			int x = 0;
			if (line.getDateStart() != null) {
				Integer xStart = dateToPixel(line.getDateStart());
				if (xStart != null) {
					x = xStart;
				}
			}
			g2d.drawLine(x, y, data.getViewTotalWidth(), y);
		}
	}

	public void drawArrowDown(Integer x, int y) {
		g2d.setPaint(Color.WHITE);
		Polygon polygon = new Polygon();
		int s = 5;
		polygon.addPoint(x, y);
		polygon.addPoint(x + 3*s, y - 2*s);
		polygon.addPoint(x + s, y - 2*s);
		polygon.addPoint(x + s, y - 4*s);
		polygon.addPoint(x - s, y - 4*s);
		polygon.addPoint(x - s, y - 2*s);
		polygon.addPoint(x - 3*s, y - 2*s);
		g2d.fillPolygon(polygon);
	}

	public void drawArrowUP(Integer x, int y) {
		g2d.setPaint(Color.WHITE);
		Polygon polygon = new Polygon();
		int s = 5;
		polygon.addPoint(x, y);
		polygon.addPoint(x + 3*s, y + 2*s);
		polygon.addPoint(x + s, y + 2*s);
		polygon.addPoint(x + s, y + 4*s);
		polygon.addPoint(x - s, y + 4*s);
		polygon.addPoint(x - s, y + 2*s);
		polygon.addPoint(x - 3*s, y + 2*s);
		g2d.fillPolygon(polygon);
	}

	public void drawLine(PricePointDTO pp1, PricePointDTO pp2) {
		Integer pixelDate1 = dateToPixel(pp1.getDate());
		Integer pixelDate2 = dateToPixel(pp2.getDate());
		if (pixelDate1 != null && pixelDate2 != null) {
			g2d.setPaint(Color.RED);
			g2d.drawLine(pixelDate1, priceToYPixel(pp1.getPrice()),	pixelDate2, priceToYPixel(pp2.getPrice()));
		} else {
			System.out.println(pp1.getDate() + " ou " + pp2.getDate() +" can not be associated  associated with any pixel on graph");
		}
	}

	public Integer dateToPixel(LocalDateTime date) {
		GraphDateDTO graphDateDTO = getGraphDateFromDate(date);
		if (graphDateDTO == null) {
			return null;
		}
		return (graphDateDTO.getStartPosition() + graphDateDTO.getEndPosition())/2;
	}

	public int priceToYPixel(Double price) {
		return getHeight() - priceToPixel(price - data.getPriceMin());
	}

	private void drawGrid() {
		synchronized (MainPanel.LOCK) {
			int y = 0;
			while(y < getHeight()) {
				g2d.setPaint(new Color(69, 90, 100));
				g2d.drawLine(0, y, getWidth()+30, y);
				y += 40;
				g2d.setPaint(Color.white);
			}

			int width = 150;
			int x = width/2 + 1 ;

			while(x < getWidth()) {
				g2d.setPaint(new Color(69, 90, 100));
				g2d.drawLine(x, 0, x, getHeight());
				g2d.setPaint(Color.white);
				LocalDateTime date = getDateFromPixel(x);
				if (date == null) {
					return;
				}
				g2d.drawString(padTime(date.getHour()) + ":" + padTime(date.getMinute()), x-15, getHeight());
				x += width;
			}
		}
	}

	private String padTime(int value) {
		return StringUtils.leftPad(String.valueOf(value), 2, "0");
	}

	private void drawCandles() {
		synchronized (MainPanel.LOCK) {
			List<Candle> candles = data.getCandles();
			int candleWidth = data.getCandleWidth();
			for (Candle candle : candles) {
				GraphDateDTO graphDateDTO = getGraphDateFromDate(candle.getDate());
				if (graphDateDTO == null) {
					continue;
				}
				if (!candle.isDraw()) {
					continue;
				}
				int y = getHeight() - priceToPixel(candle.getMax()-data.getPriceMin());
				int height = priceToPixel(candle.getMax()-candle.getMin());
				if (candle.getColor() != null) {
					g2d.setPaint(candle.getColor());
				} else if (candle.isSelected()) {
					g2d.setPaint(Color.decode("#4682B4"));
				}else if (candle.isEndTrade()) {
					g2d.setPaint(Color.ORANGE);
				} else if (candle.isGoingUp()) {
					g2d.setPaint(new Color(36, 160, 107));
				} else {
					g2d.setPaint(new Color(204, 46, 60));
				}
				g2d.fillRect(graphDateDTO.getStartPosition(), y, candleWidth, height);
				int xLine = graphDateDTO.getStartPosition() + candleWidth/2;
				int yLineStart = y ;
				int yLineEnd = y - priceToPixel(candle.getHigh()-candle.getMax());
				g2d.drawLine(xLine, yLineStart, xLine, yLineEnd);

				yLineStart = y  + height;
				yLineEnd = y + height + priceToPixel(candle.getMin() - candle.getLow());
				g2d.drawLine(xLine, yLineStart, xLine, yLineEnd);
			}
		}
	}

	public GraphDateDTO getGraphDateFromDate(LocalDateTime date) {
		for (GraphDateDTO graphDateDTO : data.getGraphDates()) {
			try {
				if (graphDateDTO.getEndDate().isEqual(date) || graphDateDTO.getDate().isEqual(date) || 
						(date.isAfter(graphDateDTO.getDate()) && date.isBefore(graphDateDTO.getEndDate()))) {
					return graphDateDTO;
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private int priceToPixel(Double price) {
		return Double.valueOf(Math.ceil(price*getHeight()/data.getGapPrice())).intValue();
	}

	public long getSecondsFromEpoch(LocalDateTime date) {
		ZoneId zoneId = ZoneId.systemDefault();
		return date.atZone(zoneId).toEpochSecond();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		try {
			doDrawing(g);
		} catch (JsonProcessingException e) {
			System.out.println("Error " + e);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		repaint();
	}

	@Override
	public synchronized void mouseWheelMoved(MouseWheelEvent e) {
		this.isBusy = true;
		zoom(e.getWheelRotation() < 0);
	}

	public int zoom(boolean zoomIn) {
		boolean perfectMatch = false;
		if (zoomIn) {
			service.zoomIn();
		} else {
			service.zoomOut();
			int width = service.getViewTotalWidth();
			if (width < scrollPane.getWidth()) {
				repaint();
				perfectMatch = true;
				service.zoomIn();
			}
		}

		int graphSize = service.rebuildGraphDates(scrollPane.getWidth(), perfectMatch);
		repaint();
		moveViewToEnd();
		repaint();
		moveViewToEnd();
		return graphSize;
	}

	public void moveViewToEnd() {
		LocalDateTime initialPositionDate = data.getLastGraphDate().getDate();
		moveViewToDate(initialPositionDate);
	}

	private void moveViewToDate(LocalDateTime date) {
		GraphDateDTO graphDateDTO = getGraphDateFromDate(date);
		if (graphDateDTO != null) {
			scrollPane.getViewport().setViewPosition(new Point(graphDateDTO.getStartPosition() +100 - data.getViewWidth(), 0));
		}
	}

	@Override
	public synchronized void mouseMoved(MouseEvent event) {
		LocalDateTime date = getDateFromPixel(event.getPoint().x);
		Double price = pixelToPrice(event.getPoint().y);
		topPanel.setDateAndPrice(date, price);
		indicatorFeature.mouseMoved(date, price);
		riskRatioFeature.mouseMoved(date, price);
		distanceFeature.mouseMoved(date, price);
	}

	@Override
	public synchronized void mouseReleased(MouseEvent event) {
	}

	@Override
	public synchronized void mouseDragged(MouseEvent event) {
		this.requestFocusInWindow();
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		this.requestFocusInWindow();
		LocalDateTime date = getDateFromPixel(event.getPoint().x);
		Double price = pixelToPrice(event.getPoint().y);
		if (horizontalLineComponent.isEnabled()) {
			horizontalLineComponent.onClick(date, price);
		}
		if (verticalLineComponent.isEnabled()) {
			verticalLineComponent.onClick(date, price);
		}
		riskRatioFeature.mouseClicked(date, price);
		distanceFeature.mouseClicked(date, price);
		runner.mouseClicked(date, price);
		repaint();
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		//		System.out.println(e.getKeyCode());
		//		tradingZoneFeature.onPressKey(e.getKeyCode());
		// PG DOWN
		//		if (e.getKeyCode() == 34) {
		//			bosFeature.nextPOI();
		//		}
		//		zoneSelectorComponent.keyPressed(e.getKeyCode());
		//		bosTrainingComponent.keyPressed(e.getKeyCode());
		//		trainablePatternComponent.keyPressed(e.getKeyCode());
		horizontalLineComponent.keyPressed(e.getKeyCode());
		verticalLineComponent.keyPressed(e.getKeyCode());
		runner.keyPressed(e.getKeyCode());
		riskRatioFeature.keyPressed(e.getKeyCode());
		distanceFeature.keyPressed(e.getKeyCode());
		//touche - du pav num
		if (e.getKeyCode() == 109) {
			unzoomMaximum();
		}
		repaint();
	}

	@Override
	public void keyReleased(KeyEvent e) {
		horizontalLineComponent.keyReleased(e.getKeyCode());
		verticalLineComponent.keyReleased(e.getKeyCode());
		runner.keyReleased(e.getKeyCode());
		riskRatioFeature.keyReleased(e.getKeyCode());
		distanceFeature.keyReleased(e.getKeyCode());
		repaint();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(data.getViewTotalWidth(), WINDOW_HEIGHT);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(data.getViewTotalWidth(), 128);
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT);
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 128;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 128;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return getPreferredSize().width
				<= getParent().getSize().width;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return getPreferredSize().height
				<= getParent().getSize().height;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		data.setViewHeight(Double.valueOf(scrollPane.getViewport().getVisibleRect().getHeight()).intValue());
		data.setViewWidth(Double.valueOf(scrollPane.getViewport().getVisibleRect().getWidth()).intValue());
		data.setViewPosition(Double.valueOf(scrollPane.getViewport().getViewPosition().getX()).intValue());
		repaint();
		pricePanel.repaint();
	}

	public void onChangeTimeRange(EnumTimeRange timeRange) {
		data.setTimeRange(timeRange);
		if (scrollPane != null) {
			service.loadData(scrollPane.getWidth());
			data.setAutoZoomDone(false);
			//		zoom(true);
			moveViewToEnd();
			unzoomMaximum();
		}
	}

	public void onChangeMarket(String marketCode) {
		if (scrollPane != null) {
			data.setMarket(marketDAO.getByCode(marketCode));
			service.loadData(scrollPane.getWidth());
			moveViewToEnd();
		}
	}

	public void onAddCandles(int nbCandles) {
		data.setNbCandlesAdded(nbCandles);
		service.loadData(scrollPane.getWidth());
		moveViewToEnd();
		repaint();
	}

	public boolean isDrawing() {
		return isBusy;
	}

	public void setTopPanel(TopPanel topPanel) {
		this.topPanel = topPanel;
	}
	public void setSecondTopPanel(SecondTopPanel secondTopPanel) {
		this.secondTopPanel = secondTopPanel;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		
	}

	public void reload() {
		reload(true);
	}

	public void reload(boolean moveToEnd) {
		if (scrollPane != null) {
			service.loadData(scrollPane.getWidth());
			if (moveToEnd) {
				moveViewToEnd();
			}
			repaint();
			topPanel.repaint();
		}
	}

	public void unzoomMaximum() {
		for (int i = 0; i < 1000; i++) {
			zoom(false);
		}
	}

}

