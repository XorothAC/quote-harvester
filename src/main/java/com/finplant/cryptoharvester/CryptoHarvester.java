package com.finplant.cryptoharvester;

import info.bitrich.xchangestream.core.*;
import info.bitrich.xchangestream.binance.*;
import info.bitrich.xchangestream.poloniex2.*;

import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoHarvester {
	private static final Logger LOG = LoggerFactory.getLogger(CryptoHarvester.class);
	private static final Settings setup = Settings.readYaml();
	
	public static void main(String[] args) {
		// Initialization Code
		DatabaseCRUD db = 
				new DatabaseCRUD(setup.getDb().get("url"), setup.getDb().get("user"), setup.getDb().get("password"));
		
		db.connectDB();
		
		System.exit(0);
		// Application Code
		StreamingExchange binanceExchange = 
				StreamingExchangeFactory.INSTANCE.createExchange(BinanceStreamingExchange.class.getName());

		StreamingExchange poloniexExchange = 
				StreamingExchangeFactory.INSTANCE.createExchange(PoloniexStreamingExchange.class.getName());

		subscribeToExchange(binanceExchange);
		subscribeToExchange(poloniexExchange);
		LOG.info("Lorem Ipsum");
	}
	
	public static void subscribeToExchange(StreamingExchange exchange) {
		// Specify subscription objects
		ProductSubscription subscription = 
				ProductSubscription.create().addTicker(CurrencyPair.BTC_USDT).build();
		
		// Connect to the Exchange WebSocket API. Blocking wait for the connection.
		exchange.connect(subscription).blockingAwait();
		LOG.info("Connected to exchange: "+exchange.toString());
		
		// Log ticker info for now
		exchange.getStreamingMarketDataService().getTicker(CurrencyPair.BTC_USDT)
			.subscribe(ticker -> {LOG.info(exchange.toString() + ": {}", ticker);},
					throwable -> LOG.error("ERROR in getting trades: ", throwable));
		
		// Disconnect from exchange (non-blocking)
		//exchange.disconnect().subscribe(() -> LOG.info("Disconnected from the Exchange"));
	}
}
