package com.finplant.cryptoharvester;

import info.bitrich.xchangestream.core.*;
import info.bitrich.xchangestream.binance.*;
import info.bitrich.xchangestream.poloniex2.*;
import io.reactivex.disposables.Disposable;

import java.util.ArrayList;
import java.util.List;

import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoHarvester {
	private static final Logger LOG = LoggerFactory.getLogger(CryptoHarvester.class);
	private static final Settings setup = Settings.readYaml();
	private static List<Quote> quoteBuffer = new ArrayList<Quote>();
	
	public static void main(String[] args) {
		// Initialization Code
		DatabaseCRUD db = 
				new DatabaseCRUD(setup.getDb().get("url"), setup.getDb().get("user"), setup.getDb().get("password"));
	
		// Application Code
		StreamingExchange binanceExchange = 
				StreamingExchangeFactory.INSTANCE.createExchange(BinanceStreamingExchange.class.getName());

		StreamingExchange poloniexExchange = 
				StreamingExchangeFactory.INSTANCE.createExchange(PoloniexStreamingExchange.class.getName());

		subscribeToExchange(binanceExchange);
		//subscribeToExchange(poloniexExchange);
		
		LOG.info(quoteBuffer.get(1).getExchange());
		LOG.info(quoteBuffer.get(1).getName().toString());
		LOG.info("Lorem Ipsum");
		quoteBuffer.clear();
	}
	
	public static void subscribeToExchange(StreamingExchange exchange) {
		// Specify subscription objects
		ProductSubscription subscription = 
				ProductSubscription.create().addTicker(CurrencyPair.BTC_USDT).build();
		
		// Connect to the Exchange WebSocket API. Blocking wait for the connection.
		exchange.connect(subscription).blockingAwait();
		LOG.info("Connected to exchange: "+exchange.toString());
		quoteBuffer.add(new Quote(null,null,null,null,null));
		try {
			// Log ticker info for now
			exchange.getStreamingMarketDataService().getTicker(CurrencyPair.BTC_USDT)
				.subscribe(ticker -> {quoteBuffer.add(new Quote(ticker.getTimestamp(), ticker.getBid(), ticker.getAsk(), exchange.toString(), ticker.getCurrencyPair()));},
						throwable -> LOG.error("ERROR in getting tickers: ", throwable));
			
			Thread.sleep(5000);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			exchange.disconnect();
			LOG.info("Disconnected from the Exchange");
		}
	}
}
