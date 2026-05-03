package com.oms.sim;

import com.google.protobuf.Message;
import com.oms.ExecType;
import com.oms.OrdStatus;
import com.oms.OrdType;
import com.oms.Side;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates the full lifecycle of one parent order. Determines whether the order is
 * replaced/cancelled/filled based on the supplied {@link Random}. Output is an ordered
 * list of (msgType, payload) pairs that the publisher can iterate over.
 */
public final class Scenario {

    public record Event(String msgType, Message payload, String clOrdId) {}

    private final IdGenerator ids;
    private final Random rnd;

    public Scenario(IdGenerator ids, Random rnd) {
        this.ids = ids;
        this.rnd = rnd;
    }

    public List<Event> generate(String account, String symbol) {
        List<Event> out = new ArrayList<>();

        Side side = rnd.nextBoolean() ? Side.SIDE_BUY : Side.SIDE_SELL;
        boolean isLimit = rnd.nextDouble() < 0.7;
        OrdType ordType = isLimit ? OrdType.ORD_TYPE_LIMIT : OrdType.ORD_TYPE_MARKET;
        double orderQty = (1 + rnd.nextInt(20)) * 100.0; // 100..2000
        Double limitPrice = isLimit ? round2(50 + rnd.nextDouble() * 450) : null;

        // Step 1: NewOrderSingle
        String clNew = ids.nextClOrdId();
        var nos = FixMessageFactory.newOrder(clNew, account, symbol, side, ordType, orderQty, limitPrice);
        out.add(new Event("D", nos, clNew));

        // Stable per-parent OrderID and Side/OrdType used for all subsequent ERs
        String orderId = ids.nextOrderId();
        String latestCl = clNew;
        double currentOrderQty = orderQty;
        Double currentPrice = limitPrice;

        // Step 2: New Order Ack
        out.add(new Event("8", FixMessageFactory.execAck(
                latestCl, account, symbol, side, ordType,
                orderId, ids.nextExecId(),
                ExecType.EXEC_TYPE_NEW, OrdStatus.ORD_STATUS_NEW,
                currentOrderQty, 0.0, currentOrderQty,
                null, null, currentPrice, 0.0), latestCl));

        // Step 3: 0..2 replaces
        int replaces = rnd.nextInt(3);
        for (int i = 0; i < replaces; i++) {
            String clRepl = ids.nextClOrdId();
            double newQty = currentOrderQty + (rnd.nextBoolean() ? 100.0 : -100.0);
            if (newQty < 100) newQty = currentOrderQty + 100;
            Double newPrice = isLimit ? round2(50 + rnd.nextDouble() * 450) : null;

            var rep = FixMessageFactory.replace(
                    clRepl, latestCl, account, symbol, side, ordType, newQty, newPrice);
            out.add(new Event("G", rep, clRepl));

            currentOrderQty = newQty;
            currentPrice = newPrice;
            latestCl = clRepl;

            out.add(new Event("8", FixMessageFactory.execAck(
                    latestCl, account, symbol, side, ordType,
                    orderId, ids.nextExecId(),
                    ExecType.EXEC_TYPE_REPLACED, OrdStatus.ORD_STATUS_REPLACED,
                    currentOrderQty, 0.0, currentOrderQty,
                    null, null, currentPrice, 0.0), latestCl));
        }

        // Step 4: 0..N partial fills, then either FILL or CANCEL
        double cumQty = 0.0;
        double leavesQty = currentOrderQty;
        double avgPx = 0.0;
        boolean cancelled = rnd.nextDouble() < 0.2;

        int maxFills = 1 + rnd.nextInt(4);
        int fillsDone = 0;
        while (leavesQty > 0 && fillsDone < maxFills && !cancelled) {
            // Last fill flushes remaining qty (otherwise we'd never quite hit zero).
            boolean isLast = (fillsDone == maxFills - 1);
            double lastQty = isLast ? leavesQty : Math.max(100, Math.floor(leavesQty * rnd.nextDouble()));
            if (lastQty > leavesQty) lastQty = leavesQty;
            double lastPx = currentPrice != null ? currentPrice : round2(50 + rnd.nextDouble() * 450);

            cumQty += lastQty;
            leavesQty -= lastQty;
            avgPx = round4(((avgPx * (cumQty - lastQty)) + (lastPx * lastQty)) / cumQty);

            ExecType execType = leavesQty == 0
                    ? ExecType.EXEC_TYPE_FILL
                    : ExecType.EXEC_TYPE_PARTIAL_FILL;
            OrdStatus ordStatus = leavesQty == 0
                    ? OrdStatus.ORD_STATUS_FILLED
                    : OrdStatus.ORD_STATUS_PARTIALLY_FILLED;

            out.add(new Event("8", FixMessageFactory.execAck(
                    latestCl, account, symbol, side, ordType,
                    orderId, ids.nextExecId(),
                    execType, ordStatus,
                    currentOrderQty, cumQty, leavesQty,
                    lastQty, lastPx, currentPrice, avgPx), latestCl));
            fillsDone++;
        }

        // Step 5: optional cancel if still open
        if (cancelled && leavesQty > 0) {
            String clCancel = ids.nextClOrdId();
            var canc = FixMessageFactory.cancel(
                    clCancel, latestCl, account, symbol, side, leavesQty);
            out.add(new Event("F", canc, clCancel));
            latestCl = clCancel;

            out.add(new Event("8", FixMessageFactory.execAck(
                    latestCl, account, symbol, side, ordType,
                    orderId, ids.nextExecId(),
                    ExecType.EXEC_TYPE_CANCELED, OrdStatus.ORD_STATUS_CANCELED,
                    currentOrderQty, cumQty, leavesQty,
                    null, null, currentPrice, avgPx), latestCl));
        }

        return out;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
