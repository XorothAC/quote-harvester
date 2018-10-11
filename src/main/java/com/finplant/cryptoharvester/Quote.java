package com.finplant.cryptoharvester;

import java.math.BigDecimal;
import java.util.Date;

import org.knowm.xchange.currency.CurrencyPair;

public class Quote {
	
	private Date timestamp;
	private BigDecimal bid;
	private BigDecimal ask;
	private String exchange;
	private String name;
	private CurrencyPair currencyPair;
	
	public Quote(Date timestamp, BigDecimal bid, BigDecimal ask, String exchange, String name, CurrencyPair currencyPair) {
		this.timestamp = timestamp;
		this.bid = bid;
		this.ask = ask;
		this.exchange = exchange;
		this.name = name;
		this.currencyPair = currencyPair;
	}
	
	@Override
	public boolean equals (Object object) {
		boolean result = false;
		if (object == null) {
			result = false;
		} else {
			Quote quote = (Quote) object;
			if (this.getExchange().equals(quote.getExchange()) && 
					this.getName().equals(quote.getName()) &&
					this.getCurrencyPair().equals(quote.getCurrencyPair())) {
				result = true;
			}
		}
		
		return result;
	}
	
	@Override
	public String toString() {
		return "Timestamp: " + this.timestamp + 
				" Bid: " + this.bid + 
				" Ask: " + this.ask + 
				" Exchange:" + this.exchange + 
				" Name: " + this.name +
				" Currency Pair: " + this.currencyPair;
	}

	public BigDecimal getBid() {
		return bid;
	}

	public void setBid(BigDecimal bid) {
		this.bid = bid;
	}

	public BigDecimal getAsk() {
		return ask;
	}

	public void setAsk(BigDecimal ask) {
		this.ask = ask;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public String getExchange() {
		return exchange;
	}

	public String getName() {
		return name;
	}

	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}

	public void setCurrencyPair(CurrencyPair currencyPair) {
		this.currencyPair = currencyPair;
	}
}
