package com.trading.feature.old;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.trading.dto.DataDTO;
import com.trading.entity.TrainablePattern;
import com.trading.feature.AbstractFeature;
import com.trading.entity.Candel;
import com.trading.entity.PointOfInterest;
import com.trading.gui.TopPanel;
import com.trading.repository.TrainablePatternRepository;
import com.trading.repository.PointOfInterestRespository;

@Component
public class BOSFeature extends AbstractFeature{

	@Autowired
	DataDTO data;
	@Autowired
	TrainablePatternRepository trainablePatternRepository;
	@Autowired
	PointOfInterestRespository poiRespository;
	@Autowired
	TopPanel topPanel;

	List<Candel> pointsOfBOS = new ArrayList<>(4);
	List<PointOfInterest> poiList;
	PointOfInterest poi;
	private int index = 0;

	private void saveBOS(LocalDateTime date, int sentiment) {
		Candel candel = data.getCandels().stream().filter(c -> c.getDate().isEqual(date)).findFirst().get();
		pointsOfBOS.add(candel);
		candel.setSelected(true);
		if (pointsOfBOS.size() == 4) {
			TrainablePattern bos = data.getBos();
//			bos.setDateP1(pointsOfBOS.get(0).getDate());
//			bos.setDateP2(pointsOfBOS.get(1).getDate());
//			bos.setDateP3(pointsOfBOS.get(2).getDate());
//			bos.setDateP4(pointsOfBOS.get(3).getDate());
			bos.setSentiment(sentiment);
			bos.setCreationDate(LocalDateTime.now());
			trainablePatternRepository.save(bos);
			topPanel.newBOSSaved(sentiment);
			createEmptyBOS();
			pointsOfBOS.clear();
		}
	}

	public long getNbBos() {
		return trainablePatternRepository.count();
	}

	public void createEmptyBOS() {
		TrainablePattern bos = new TrainablePattern();
		bos.setMarket(data.getMarket().getCode());
//		bos.setDateMax(poi.getDateMax());
//		bos.setDateMinBeforeMax(poi.getDateMinBeforeMax());
		data.setBos(bos);
	}

	@Transactional
	public void POIisDone() {
		poi.setDone(true);
		poiRespository.save(poi);
	}

	public void clear() {
		pointsOfBOS.forEach(c -> c.setSelected(false));
		pointsOfBOS.clear();		
	}

	public void onClick(LocalDateTime date, Double price, int button) {
		if (button == 1) {
			saveBOS(date, 1);
		} else if (button == 3){
			saveBOS(date, 0);
		} 
	}

	public void mouseMoved(LocalDateTime date, Double price) {
		// TODO Auto-generated method stub

	}

	public void init() {
		poiList = poiRespository.findByDone(false);
		addNextPOIButton();
		addClearBOSButton();
	}

	private void addClearBOSButton() {
		JButton button = new JButton("Clear BOS");
		topPanel.add(button);
		button.setLocation(20, 20);
		button.addActionListener(e -> {
			clear();
			mainPanel.reload();
		});
	}

	private void addNextPOIButton() {
		JButton button = new JButton("Next POI");
		topPanel.add(button);
		button.setLocation(20, 20);
		button.addActionListener(e -> {
			nextPOI();
		});
	}

	public List<Candel> getCandels() {
		PointOfInterest poi = poiList.get(index);
		List<Candel> candels = null;
//				candelRepository.findByMarketAndDateBetweenOrderByDateAsc(
//						poi.getMarket(),
//						poi.getDateMax(),
//						poi.getDateMax().plusMinutes(100));
		//BOS
		createEmptyBOS();
		return candels;
	}

	public void nextPOI() {
		POIisDone();
		index++;
		data.setAutoZoomDone(false);
		clear();
		mainPanel.reload();
	}

}
