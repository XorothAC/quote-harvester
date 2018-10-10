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
