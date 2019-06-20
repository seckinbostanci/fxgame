package com.seckinbostanci;

public class StockmarketData {
    private String symbol;
    private String stockPrices;
    private String time;

    public StockmarketData() {
    }

    public StockmarketData(String symbol, String stockPrices, String time) {
        this.symbol = symbol;
        this.stockPrices = stockPrices;
        this.time = time;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getStockPrices() {
        return stockPrices;
    }

    public void setStockPrices(String stockPrices) {
        this.stockPrices = stockPrices;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

}
