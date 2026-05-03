package com.oms.sim;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "oms-fix-simulator", mixinStandardHelpOptions = true,
        description = "Generates FIX 4.2 protobuf messages and publishes them to Kafka.")
public final class App implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    private static final List<String> DEFAULT_SYMBOLS = List.of(
            "AAPL", "MSFT", "GOOG", "AMZN", "NVDA", "TSLA", "META", "SPY", "QQQ", "IWM");

    @Option(names = "--bootstrap", defaultValue = "localhost:29092",
            description = "Kafka bootstrap servers")
    String bootstrap;

    @Option(names = "--topic", defaultValue = "oms-fix-proto", description = "Kafka topic")
    String topic;

    @Option(names = "--orders", defaultValue = "100", description = "Number of parent orders")
    int orders;

    @Option(names = "--rate-per-sec", defaultValue = "50",
            description = "Approx rate of parent-order lifecycles per second")
    int ratePerSec;

    @Option(names = "--seed", defaultValue = "42", description = "RNG seed (deterministic)")
    long seed;

    @Option(names = "--accounts", defaultValue = "20", description = "Number of synthetic accounts")
    int accounts;

    @Option(names = "--symbols", split = ",", description = "Symbol pool (comma-separated)")
    List<String> symbols;

    public static void main(String[] args) {
        int code = new CommandLine(new App()).execute(args);
        System.exit(code);
    }

    @Override
    public Integer call() throws Exception {
        if (symbols == null || symbols.isEmpty()) symbols = DEFAULT_SYMBOLS;

        Random rnd = new Random(seed);
        IdGenerator ids = new IdGenerator(seed);
        Scenario scenario = new Scenario(ids, rnd);

        long delayPerOrderNs = ratePerSec > 0 ? TimeUnit.SECONDS.toNanos(1) / ratePerSec : 0;

        LOG.info("Starting simulator: bootstrap={} topic={} orders={} rate={}/s seed={}",
                bootstrap, topic, orders, ratePerSec, seed);

        long published = 0;
        try (KafkaPublisher publisher = new KafkaPublisher(bootstrap, topic)) {
            for (int i = 0; i < orders; i++) {
                long start = System.nanoTime();
                String account = String.format("ACCT%03d", 1 + rnd.nextInt(accounts));
                String symbol = symbols.get(rnd.nextInt(symbols.size()));

                List<Scenario.Event> events = scenario.generate(account, symbol);
                for (Scenario.Event e : events) {
                    publisher.publish(e);
                    published++;
                }

                if (delayPerOrderNs > 0) {
                    long elapsed = System.nanoTime() - start;
                    long remain = delayPerOrderNs - elapsed;
                    if (remain > 0) {
                        TimeUnit.NANOSECONDS.sleep(remain);
                    }
                }
                if ((i + 1) % 100 == 0) {
                    LOG.info("Published {} parent orders, {} total messages", i + 1, published);
                }
            }
            publisher.flush();
        }

        LOG.info("Done. {} parent orders -> {} messages", orders, published);
        return 0;
    }
}
