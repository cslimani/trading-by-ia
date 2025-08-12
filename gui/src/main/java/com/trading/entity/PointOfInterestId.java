package com.trading.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PointOfInterestId implements Serializable {

    String market;
    LocalDateTime dateMax;
}
