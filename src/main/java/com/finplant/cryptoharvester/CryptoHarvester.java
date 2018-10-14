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
		db.createQuotesTable();
		int flush_period_ms = setup.getFlushPeriodS() * 1000;
		
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
				subscribeToExchange(binanceExchange, currencyPair, instrument.getName(), quoteBuffer);
				subscribeToExchange(poloniexExchange, currencyPair, instrument.getName(), quoteBuffer);
			} else {
				for (String syntheticPair : instrument.getDepends()) {
					CurrencyPair currencyPair = new CurrencyPair(syntheticPair);
					LOG.info(currencyPair.toString());
					subscribeToExchange(binanceExchange, currencyPair, instrument.getName(), syntheticQuoteBuffer);
					subscribeToExchange(poloniexExchange, currencyPair, instrument.getName(), syntheticQuoteBuffer);
				}
			}
		}

		// Buffer operations
		while (true) {
			Thread.sleep(flush_period_ms);
			
			// Synthetic instrument generator and buffer update
			for (Instrument instrument : setup.getInstruments()) {
				if (instrument.getDepends() != null && !syntheticQuoteBuffer.isEmpty()) {
					LOG.info(String.valueOf(syntheticQuoteBuffer.size()));
					Quote binanceQuote = 
							syntheticInsrumentGenerator(binanceExchange, syntheticQuoteBuffer, instrument);
					Quote poloniexQuote = 
							syntheticInsrumentGenerator(poloniexExchange, syntheticQuoteBuffer, instrument);
					
					if (binanceQuote != null) {
						addOrReplaceQuote(quoteBuffer, binanceQuote);
					}
					
					if (poloniexQuote != null) {
						addOrReplaceQuote(quoteBuffer, poloniexQuote);
					}
				}
			}
			
			LOG.info("Size of quote buffer: " + String.valueOf(quoteBuffer.size()));
			for (Quote quote : quoteBuffer) {
				LOG.info(quote.toString());
			}
			LOG.info("Size of synthetic quote buffer: " + String.valueOf(syntheticQuoteBuffer.size()));
			for (Quote quote : syntheticQuoteBuffer) {
				LOG.info(quote.toString());
			}
			// Database write and buffer flush
			db.writeToDB(quoteBuffer);
		}
	}
	
	public static void subscribeToExchange(StreamingExchange exchange, CurrencyPair currencyPair, 
			String instrument, List<Quote> buffer) {
		// Specify subscription objects
		ProductSubscription subscription = 
				ProductSubscription.create().addTicker(currencyPair).build();
		String exchangeName = exchange.toString().split("#")[0];
		
		// Connect to the Exchange WebSocket API. Blocking wait for the connection.
		exchange.connect(subscription).blockingAwait();
		LOG.info("Connected to exchange: " + exchangeName);

		// Subscribe to ticker
		try {
			Disposable sub = exchange.getStreamingMarketDataService().getTicker(currencyPair)
				.subscribe(ticker -> {
						Quote quote = new Quote(ticker.getTimestamp(), ticker.getBid(), 
								ticker.getAsk(), exchangeName, instrument, currencyPair);
						addOrReplaceQuote(buffer, quote);
					},
					throwable -> LOG.error("ERROR in getting tickers: ", throwable));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			exchange.disconnect();
		}
	}
	
	// Add or replace current quote in buffer
	public static void addOrReplaceQuote(List<Quote> buffer, Quote quote) {
		if (buffer.contains(quote)) {
			buffer.remove(quote);
		}
		buffer.add(quote);
	}
	
	// Synthetic instrument generator
	public static Quote syntheticInsrumentGenerator(StreamingExchange exchange, List<Quote> buffer, Instrument instrument) {
		Quote quote = null;
		String exchangeName = exchange.toString().split("#")[0];
		
		String instrument1 = instrument.getDepends().get(0);
		String instrument2 = instrument.getDepends().get(1);

		// Try to grab the two needed quotes
		int bufferIndex1 = buffer.indexOf(new Quote(null, null, null, 
				exchangeName, instrument.getName(), new CurrencyPair(instrument1)));
		int bufferIndex2 = buffer.indexOf(new Quote(null, null, null, 
				exchangeName, instrument.getName(), new CurrencyPair(instrument2)));
		
		// If both quotes exist in synthetic instrument buffer, continue
		if (bufferIndex1 > -1 && bufferIndex2 > -1) {
			quote = new Quote(null, null, null, 
					exchangeName, instrument.getName(), new CurrencyPair(instrument.getInstrument()));
			Quote quote1 = buffer.get(bufferIndex1);
			Quote quote2 = buffer.get(bufferIndex2);
			
			String[] split = instrument.getInstrument().split("/");
			String[] split1 = instrument1.split("/");
			String[] split2 = instrument2.split("/");
			
			// Setting correct order for splits
			if (split[1].equals(split1[0]) || split[1].equals(split1[1])) {
				split = split1;
				split1 = split2;
				split2 = split;
			}
			
			// Synthetic quote bid and ask calculations
			if (split1[1].equals(split2[0])) {
				quote.setBid(quote1.getBid().multiply(quote2.getBid()));
				quote.setAsk(quote1.getAsk().multiply(quote2.getAsk()));
			} else if (split1[1].equals(split2[1])) {
				quote.setBid(quote1.getBid().divide(quote2.getBid()));
				quote.setAsk(quote1.getAsk().divide(quote2.getAsk()));
			} else if (split1[0].equals(split2[0])) {
				quote.setBid(quote2.getBid().divide(quote1.getBid()));
				quote.setAsk(quote2.getAsk().divide(quote1.getAsk()));
			} else {
				quote.setBid(quote2.getBid().divide(quote2.getBid().pow(2).multiply(quote1.getBid())));
				quote.setAsk(quote2.getAsk().divide(quote2.getAsk().pow(2).multiply(quote1.getAsk())));
			}
		}
		
		return quote;
	}
}
