package com.finplant.cryptoharvester;

import java.math.BigDecimal;
import java.util.Date;

import org.knowm.xchange.currency.CurrencyPair;

public class Quote {
	
	private Date timestamp;
	private BigDecimal bid;
	private BigDecimal ask;
	private String exchange;
	private CurrencyPair name;
	
	public Quote(Date timestamp, BigDecimal bid, BigDecimal ask, String exchange, CurrencyPair name) {
		this.timestamp = timestamp;
		this.bid = bid;
		this.ask = ask;
		this.exchange = exchange;
		this.name = name;
	}
	
	@Override
	public boolean equals (Object object) {
		boolean result = false;
		if (object == null) {
			result = false;
		} else {
			Quote quote = (Quote) object;
			if (this.getExchange().equals(quote.getExchange()) && this.getName().equals(quote.getName())) {
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
				" Name: "+this.name;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}

	public BigDecimal getBid() {
		return bid;
	}

	public BigDecimal getAsk() {
		return ask;
	}

	public String getExchange() {
		return exchange;
	}

	public CurrencyPair getName() {
		return name;
	}
}
