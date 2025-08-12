package com.trading.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrainingTradeId implements Serializable {

    private static final long serialVersionUID = 1L;
    
	String market;
    LocalDateTime dateOpen;
    String type;
}
