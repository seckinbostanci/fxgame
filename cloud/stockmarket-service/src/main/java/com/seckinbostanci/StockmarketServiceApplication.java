package com.seckinbostanci;

import com.pretty_tools.dde.ClipboardFormat;
import com.pretty_tools.dde.DDEException;
import com.pretty_tools.dde.DDEMLException;
import com.pretty_tools.dde.client.DDEClientConversation;
import com.pretty_tools.dde.client.DDEClientEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class StockmarketServiceApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(StockmarketServiceApplication.class);

    private final String SERVICE = "MT4";
    private final String ITEM = "USDTRY";
    private final String TOPIC = "QUOTE";
    private final CountDownLatch eventDisconnect = new CountDownLatch(1);

    @Autowired
    private KafkaUtils kafkaUtils;

    public static void main(String[] args) {
        SpringApplication.run(StockmarketServiceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            final DDEClientConversation conversation = new DDEClientConversation();
            conversation.setEventListener(new DDEClientEventListener() {
                public void onDisconnect() {
                    eventDisconnect.countDown();
                }

                public void onItemChanged(String topic, String item, String data) {
                    StockmarketData stockmarketData = new StockmarketData();
                    stockmarketData.setSymbol(item);
                    stockmarketData.setStockPrices(data.split(" ")[2] + " " + data.split(" ")[3]);
                    stockmarketData.setTime(data.split(" ")[1] + " " + data.split(" ")[1]);
                    LOGGER.info("Stockmarket Data : " + item + " [ " + data + " ]");
                    kafkaUtils.produceEvent(stockmarketData);
                    try {
                        if ("stop".equalsIgnoreCase(data))
                            conversation.stopAdvice(item);
                    } catch (DDEException e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            });

            LOGGER.info("Connecting to Metatrader DDE Server.");
            conversation.connect(SERVICE, TOPIC);
            conversation.startAdvice(ITEM);
            eventDisconnect.await();
            LOGGER.info("Disconnecting from Metatrader DDE Server.");
            conversation.disconnect();
        } catch (DDEMLException e) {
            LOGGER.error("0x" + Integer.toHexString(e.getErrorCode()) + " " + e.getMessage());
        } catch (DDEException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
