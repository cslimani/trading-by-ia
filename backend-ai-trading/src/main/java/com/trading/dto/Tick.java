package com.trading.dto;

import java.time.Instant;

import com.trading.tech.TickToCandles.PriceSource;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Tick {
    public Instant time;
    public double ask;
    public double bid;

    public double price(PriceSource src) {
        switch (src) {
            case ASK: return ask;
            case BID: return bid;
            default:  return (ask + bid) * 0.5;
        }
    }
}
