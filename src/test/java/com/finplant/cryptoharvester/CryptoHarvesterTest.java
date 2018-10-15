package com.finplant.cryptoharvester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.knowm.xchange.currency.CurrencyPair;

import com.finplant.cryptoharvester.Settings.Instrument;

import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;

public class CryptoHarvesterTest {
	CryptoHarvester dummy = new CryptoHarvester();
	
    @SuppressWarnings("static-access")
	@Test
    public void subscribeToExchangeTest() {
    	StreamingExchange exchange = 
    			StreamingExchangeFactory.INSTANCE.createExchange(BinanceStreamingExchange.class.getName());
    	CurrencyPair currencyPair = CurrencyPair.ETH_EUR;
    	String instrument = "ETHEUR";
    	List<Quote> buffer = new ArrayList<Quote>();
    	
    	dummy.subscribeToExchange(exchange, currencyPair, instrument, buffer);
    	assertTrue("Exchange disconnects after subscribing: ", !exchange.isAlive());
    }
    
    @SuppressWarnings("static-access")
	@Test
    public void addOrReplaceQuoteTest() {
    	List<Quote> buffer = new ArrayList<Quote>();
    	Quote quote1 = new Quote(null, null, null, 
				"Binance", "ETHEUR", CurrencyPair.ETH_EUR);
    	Quote quote2 = new Quote(null, null, null, 
    			"Binance", "BCHEUR", CurrencyPair.BCH_EUR);
    	buffer.add(quote1);

    	dummy.addOrReplaceQuote(buffer, quote1);
    	assertEquals("Quote was simply replaced.", buffer.size(), 1);
    	dummy.addOrReplaceQuote(buffer, quote2);
    	assertEquals("New quote added.", buffer.size(), 2);
    }
    
    @SuppressWarnings("static-access")
	@Test
    public void syntheticInsrumentGeneratorTest() {
    	StreamingExchange exchange = 
    			StreamingExchangeFactory.INSTANCE.createExchange(BinanceStreamingExchange.class.getName());
    	
    	Instrument instrument = new Instrument();
    	instrument.setName("ETHBTC");
    	instrument.setInstrument("ETH/BTC");
    	List<String> depends = new ArrayList<String>();
    	depends.add("ETH/EUR");
    	depends.add("BTC/EUR");
    	instrument.setDepends(depends);
    	
    	List<Quote> buffer = new ArrayList<Quote>();
    	
    	Quote quote1 = new Quote(null, new BigDecimal(10), new BigDecimal(10), 
				"Binance", "ETHBTC", CurrencyPair.ETH_EUR);
    	Quote quote2 = new Quote(null, new BigDecimal(5), new BigDecimal(5), 
				"Binance", "ETHBTC", CurrencyPair.BTC_EUR);
    	Quote expectedQuote = new Quote(null, new BigDecimal(2), new BigDecimal(2), 
				"Binance", "ETHBTC", CurrencyPair.ETH_BTC);
    	buffer.add(quote1);
    	buffer.add(quote2);
    	
    	Quote newQuote = dummy.syntheticInsrumentGenerator(exchange, buffer, instrument);

    	assertTrue("Predicted quote equals generated quote.", expectedQuote.equals(newQuote));
    }
    
    @SuppressWarnings("static-access")
	@Test
    public void main50SecondCoverageTest() throws InterruptedException {
    	Thread dummyThread = new Thread(new Runnable() {
    		public void run() {
    			dummy.main(null);
    		}
    	});
    	dummyThread.start();
    	dummyThread.join(50000);
    	assertTrue(dummyThread.isAlive());
    }
}
