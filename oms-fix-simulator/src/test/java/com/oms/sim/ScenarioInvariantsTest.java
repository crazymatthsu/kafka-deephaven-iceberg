package com.oms.sim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import com.oms.ExecutionReport;
import com.oms.NewOrderSingle;
import com.oms.OrderCancelReplaceRequest;
import com.oms.OrderCancelRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScenarioInvariantsTest {

    @Test
    void qtyInvariantHoldsForEveryExecutionReport() {
        runMany(2_000, (msgType, payload) -> {
            if (payload instanceof ExecutionReport er) {
                assertThat(er.getOrderQty())
                        .as("OrderQty == CumQty + LeavesQty for execId=%s", er.getExecId())
                        .isCloseTo(er.getCumQty() + er.getLeavesQty(), offset(1e-9));
            }
        });
    }

    @Test
    void execIdsAreUniqueAcrossLargeRun() {
        Set<String> seen = new HashSet<>();
        runMany(10_000, (msgType, payload) -> {
            if (payload instanceof ExecutionReport er) {
                assertThat(seen.add(er.getExecId()))
                        .as("duplicate execId %s", er.getExecId())
                        .isTrue();
            }
        });
    }

    @Test
    void clOrdIdChainReachesOriginalNewOrderSingle() {
        // For each parent order: NewOrder -> ack -> [replaces -> ack]* -> [fills]* -> [cancel -> ack]?
        // Walk every G/F's OrigClOrdID and confirm we can chain back to a known D.
        Random rnd = new Random(123);
        IdGenerator ids = new IdGenerator(123);
        Scenario scen = new Scenario(ids, rnd);

        for (int i = 0; i < 200; i++) {
            var events = scen.generate("ACCT001", "AAPL");
            Set<String> dClOrdIds = new HashSet<>();
            // forward chain: child clOrdId -> parent (orig) clOrdId
            Map<String, String> parents = new HashMap<>();
            for (var ev : events) {
                switch (ev.msgType()) {
                    case "D" -> dClOrdIds.add(((NewOrderSingle) ev.payload()).getHeader().getClOrdId());
                    case "G" -> {
                        var g = (OrderCancelReplaceRequest) ev.payload();
                        parents.put(g.getHeader().getClOrdId(), g.getOrigClOrdId());
                    }
                    case "F" -> {
                        var f = (OrderCancelRequest) ev.payload();
                        parents.put(f.getHeader().getClOrdId(), f.getOrigClOrdId());
                    }
                    default -> { /* ER mirrors something already chained */ }
                }
            }
            for (String child : parents.keySet()) {
                String cur = child;
                int hops = 0;
                while (parents.containsKey(cur) && hops < 100) {
                    cur = parents.get(cur);
                    hops++;
                }
                assertThat(dClOrdIds)
                        .as("Chain root %s for child %s should be a NewOrderSingle", cur, child)
                        .contains(cur);
            }
        }
    }

    private static void runMany(int orders, java.util.function.BiConsumer<String, Object> visitor) {
        Random rnd = new Random(7);
        IdGenerator ids = new IdGenerator(7);
        Scenario scen = new Scenario(ids, rnd);
        for (int i = 0; i < orders; i++) {
            for (var ev : scen.generate("ACCT001", "AAPL")) {
                visitor.accept(ev.msgType(), ev.payload());
            }
        }
    }
}
