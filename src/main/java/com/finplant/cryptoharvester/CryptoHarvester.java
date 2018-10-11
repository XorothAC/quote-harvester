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

		while(true) {
			Thread.sleep(flush_period_ms);
			
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
			db.writeToDB(quoteBuffer);
		}
	}
	
	public static void subscribeToExchange(StreamingExchange exchange, CurrencyPair currencyPair, 
			String instrument, List<Quote> buffer) {
		// Specify subscription objects
		ProductSubscription subscription = 
				ProductSubscription.create().addTicker(currencyPair).build();
		
		// Connect to the Exchange WebSocket API. Blocking wait for the connection.
		exchange.connect(subscription).blockingAwait();
		LOG.info("Connected to exchange: " + exchange.toString());
		
		try {
			Disposable sub = exchange.getStreamingMarketDataService().getTicker(currencyPair)
				.subscribe(ticker -> {
						Quote quote = new Quote(ticker.getTimestamp(), ticker.getBid(), 
								ticker.getAsk(), exchange.toString(), instrument, currencyPair);
						addOrReplaceQuote(buffer, quote);
					},
					throwable -> LOG.error("ERROR in getting tickers: ", throwable));
//			sub.dispose();
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
//		ETH/USD  |  ETH/BTC  BTC/USDT  | (ETH/BTC)*(BTC/USDT)= (ETH*BTC)/(BTC*USDT)= ETH/USDT
//		ETH/USD  |  ETH/BCH  USD/BCH   | (ETH/BCH)/(USD/BCH) = (ETH*BCH)/(USD*BCH) = ETH/USD
//					quote1   quote2
//	    X/Z      |  X/Y      Y/Z       | (X/Y)*(Y/Z)		 = XY/ZY			   = X/Z
//		X/Z		 |	X/Y		 Z/Y	   | (X/Y)/(Z/Y)		 = XY/ZY			   = X/Z
//		X/Z		 |  Y/X		 Y/Z       | (Y/Z)/(Y/X)		 = XY/ZY			   = X/Z
//		X/Z		 |  Y/X		 Z/Y       | (Z/Y)/((Z/Y)^2 *Y/X)= (Z/Y)/(Z^2 / XY)	   = (Z*XY)/(Y*Z^2) = X/Z
		
		Quote quote = null;
		
		String instrument1 = instrument.getDepends().get(0);
		String instrument2 = instrument.getDepends().get(1);

		int bufferIndex1 = buffer.indexOf(new Quote(null, null, null, 
				exchange.toString(), instrument.getName(), new CurrencyPair(instrument1)));
		int bufferIndex2 = buffer.indexOf(new Quote(null, null, null, 
				exchange.toString(), instrument.getName(), new CurrencyPair(instrument2)));
		
		if (bufferIndex1 > -1 && bufferIndex2 > -1) {
			quote = new Quote(null, null, null, 
					exchange.toString(), instrument.getName(), new CurrencyPair(instrument.getInstrument()));
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
			
			if (split1[1].equals(split2[0])) {
				quote.setBid(quote1.getBid().multiply(quote2.getBid()));
				quote.setAsk(quote1.getAsk().multiply(quote2.getAsk()));
			} else if (split1[1] == split2[1]) {
				quote.setBid(quote1.getBid().divide(quote2.getBid()));
				quote.setAsk(quote1.getAsk().divide(quote2.getAsk()));
			} else if (split1[0] == split2[0]) {
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
