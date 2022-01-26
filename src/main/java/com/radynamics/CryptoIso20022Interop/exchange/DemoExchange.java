package com.radynamics.CryptoIso20022Interop.exchange;

import java.time.LocalDateTime;

public class DemoExchange implements ExchangeRateProvider {
    private ExchangeRate[] exchangeRates;

    public static final String ID = "demo";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayText() {
        return "Demo Exchange (fixed rates)";
    }

    @Override
    public void load() {
        exchangeRates = new ExchangeRate[3];
        exchangeRates[0] = new ExchangeRate("XRP", "USD", 0.843332, LocalDateTime.now());
        exchangeRates[1] = new ExchangeRate("XRP", "EUR", 0.69946, LocalDateTime.now());
        exchangeRates[2] = new ExchangeRate("XRP", "CHF", 0.78825, LocalDateTime.now());
    }

    @Override
    public ExchangeRate[] rates() {
        return exchangeRates;
    }
}
