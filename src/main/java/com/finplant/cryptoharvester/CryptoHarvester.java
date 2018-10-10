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

import com.finplant.cryptoharvester.Settings.Instrument;

public class CryptoHarvester {
	private static final Logger LOG = LoggerFactory.getLogger(CryptoHarvester.class);
	private static final Settings setup = Settings.readYaml();
	
	public static void main(String[] args) throws InterruptedException  {
		// Initialization Code
		DatabaseCRUD db = 
				new DatabaseCRUD(setup.getDb().get("url"), setup.getDb().get("user"), setup.getDb().get("password"));
		
		int flush_period_ms = setup.getFlush_period_s() * 1000;
		
		List<Quote> quoteBuffer = new ArrayList<Quote>();
		List<Quote> syntheticQuoteBuffer = new ArrayList<Quote>();
		
		StreamingExchange binanceExchange = 
				StreamingExchangeFactory.INSTANCE.createExchange(BinanceStreamingExchange.class.getName());

		StreamingExchange poloniexExchange = 
				StreamingExchangeFactory.INSTANCE.createExchange(PoloniexStreamingExchange.class.getName());

		// Subscribing to all instruments specified in settings.yml for Binance and Poloniex exchanges
		for (Instrument instrument : setup.getInstruments()) {
			if (instrument.getDepends() == null) {
				CurrencyPair currencyPair = new CurrencyPair(instrument.getInstrument());
				LOG.info(currencyPair.toString());
				subscribeToExchange(binanceExchange, currencyPair, quoteBuffer);
				subscribeToExchange(poloniexExchange, currencyPair, quoteBuffer);
			} else {
				for (String syntheticPair : instrument.getDepends()) {
					CurrencyPair currencyPair = new CurrencyPair(syntheticPair);
					subscribeToExchange(binanceExchange, currencyPair, syntheticQuoteBuffer);
					subscribeToExchange(poloniexExchange, currencyPair, syntheticQuoteBuffer);
				}
			}
		}

		while(true) {
			for (Instrument instrument : setup.getInstruments()) {
				if (instrument.getDepends() != null) {
					Quote binanceQuote = syntheticInsrumentGenerator(binanceExchange, syntheticQuoteBuffer, instrument);
					Quote poloniexQuote = syntheticInsrumentGenerator(poloniexExchange, syntheticQuoteBuffer, instrument);
					
					addOrReplaceQuote(quoteBuffer, binanceQuote);
					addOrReplaceQuote(quoteBuffer, poloniexQuote);
				}
			}
						
			LOG.info(quoteBuffer.get(0).toString());
			LOG.info(String.valueOf(quoteBuffer.size()));
			LOG.info(syntheticQuoteBuffer.get(0).toString());
			LOG.info(String.valueOf(syntheticQuoteBuffer.size()));
			LOG.info("Lorem Ipsum");
			Thread.sleep(flush_period_ms);
		}
	}
	
	public static void subscribeToExchange(StreamingExchange exchange, CurrencyPair currencyPair, List<Quote> buffer) {
		// Specify subscription objects
		ProductSubscription subscription = 
				ProductSubscription.create().addTicker(currencyPair).build();
		
		// Quote comparison object to avoid duplicates in buffer
		Quote quoteType = new Quote(null, null, null, exchange.toString(), currencyPair);
		
		// Connect to the Exchange WebSocket API. Blocking wait for the connection.
		exchange.connect(subscription).blockingAwait();
		LOG.info("Connected to exchange: " + exchange.toString());
		
		
		try {
			exchange.getStreamingMarketDataService().getTicker(currencyPair)
				.subscribe(ticker -> {
						Quote quote = new Quote(ticker.getTimestamp(), ticker.getBid(), 
								ticker.getAsk(), exchange.toString(), ticker.getCurrencyPair());
						addOrReplaceQuote(buffer, quote);
					},
					throwable -> LOG.error("ERROR in getting tickers: ", throwable));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			exchange.disconnect();
		}
	}
	
	public static void addOrReplaceQuote(List<Quote> buffer, Quote quote) {
		if (buffer.contains(quote)) {
			buffer.remove(quote);
		}
		buffer.add(quote);
	}
	
	public static Quote syntheticInsrumentGenerator(StreamingExchange exchange, List<Quote> buffer, Instrument instrument) {
		Quote quote = new Quote(null, null, null, null, null);
		instrument.getInstrument();
		
		
		return quote;
	}
}
