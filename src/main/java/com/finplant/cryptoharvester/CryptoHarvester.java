package com.finplant.cryptoharvester;

import info.bitrich.xchangestream.core.*;
import info.bitrich.xchangestream.binance.*;
import info.bitrich.xchangestream.poloniex.*;

import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoHarvester {
	private static final Logger LOG = LoggerFactory.getLogger(CryptoHarvester.class);
	
	public static void main(String[] args) {
		subscribeToExchanges();
		System.out.print("Lorem Ipsum");
	}
	
	public static void subscribeToExchanges() {
		// Poloniex streaming exchange
		StreamingExchange exchange = 
				StreamingExchangeFactory.INSTANCE.createExchange(PoloniexStreamingExchange.class.getName());

		// Connect to the Exchange WebSocket API. Blocking wait for the connection.
		exchange.connect().blockingAwait();
		
		// Disconnect from exchange (non-blocking)
		exchange.disconnect().subscribe(() -> LOG.info("Disconnected from the Exchange"));
	}
}
