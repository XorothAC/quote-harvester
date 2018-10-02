package com.finplant.cryptoharvester;

import info.bitrich.xchangestream.core.*;
import info.bitrich.xchangestream.binance.*;
import info.bitrich.xchangestream.poloniex.*;

public class CryptoHarvester {

	public static void main(String[] args) {
		StreamingExchange exchange = 
				StreamingExchangeFactory.INSTANCE.createExchange(PoloniexStreamingExchange.class.getName());

		// Connect to the Exchange WebSocket API. Blocking wait for the connection.
		exchange.connect().blockingAwait();

		// Disconnect from exchange (non-blocking)
		exchange.disconnect().subscribe(() -> LOG.info("Disconnected from the Exchange"));
		System.out.print("Lorem Ipsum");
	}
}
