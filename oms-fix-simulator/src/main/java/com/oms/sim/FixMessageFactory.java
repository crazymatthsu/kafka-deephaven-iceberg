package com.oms.sim;

import com.google.protobuf.Timestamp;
import com.oms.ExecType;
import com.oms.ExecutionReport;
import com.oms.Header;
import com.oms.NewOrderSingle;
import com.oms.OrdStatus;
import com.oms.OrdType;
import com.oms.OrderCancelReplaceRequest;
import com.oms.OrderCancelRequest;
import com.oms.Side;
import com.oms.TimeInForce;
import java.time.Instant;

/** Pure builders — no Kafka, no IO. All side effects happen elsewhere. */
public final class FixMessageFactory {

    private FixMessageFactory() {}

    public static Header header(String clOrdId, String account, String symbol, String msgType) {
        Instant now = Instant.now();
        return Header.newBuilder()
                .setClOrdId(clOrdId)
                .setAccount(account)
                .setSymbol(symbol)
                .setMsgType(msgType)
                .setSenderCompId("SIM")
                .setTargetCompId("OMS")
                .setTransactTime(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .build();
    }

    public static NewOrderSingle newOrder(
            String clOrdId, String account, String symbol, Side side, OrdType ordType,
            double qty, Double price) {
        NewOrderSingle.Builder b = NewOrderSingle.newBuilder()
                .setHeader(header(clOrdId, account, symbol, "D"))
                .setSide(side)
                .setOrdType(ordType)
                .setTimeInForce(TimeInForce.TIF_DAY)
                .setOrderQty(qty);
        if (price != null) {
            b.setPrice(price).setHasPrice(true);
        }
        return b.build();
    }

    public static OrderCancelReplaceRequest replace(
            String clOrdId, String origClOrdId, String account, String symbol,
            Side side, OrdType ordType, double newQty, Double newPrice) {
        OrderCancelReplaceRequest.Builder b = OrderCancelReplaceRequest.newBuilder()
                .setHeader(header(clOrdId, account, symbol, "G"))
                .setOrigClOrdId(origClOrdId)
                .setSide(side)
                .setOrdType(ordType)
                .setTimeInForce(TimeInForce.TIF_DAY)
                .setOrderQty(newQty);
        if (newPrice != null) {
            b.setPrice(newPrice).setHasPrice(true);
        }
        return b.build();
    }

    public static OrderCancelRequest cancel(
            String clOrdId, String origClOrdId, String account, String symbol,
            Side side, double qty) {
        return OrderCancelRequest.newBuilder()
                .setHeader(header(clOrdId, account, symbol, "F"))
                .setOrigClOrdId(origClOrdId)
                .setSide(side)
                .setOrderQty(qty)
                .build();
    }

    public static ExecutionReport execAck(
            String ackingClOrdId, String account, String symbol, Side side, OrdType ordType,
            String orderId, String execId, ExecType execType, OrdStatus ordStatus,
            double orderQty, double cumQty, double leavesQty,
            Double lastQty, Double lastPx, Double price, double avgPx) {
        ExecutionReport.Builder b = ExecutionReport.newBuilder()
                .setHeader(header(ackingClOrdId, account, symbol, "8"))
                .setOrderId(orderId)
                .setExecId(execId)
                .setExecType(execType)
                .setOrdStatus(ordStatus)
                .setSide(side)
                .setOrdType(ordType)
                .setOrderQty(orderQty)
                .setCumQty(cumQty)
                .setLeavesQty(leavesQty)
                .setAvgPx(avgPx);
        if (lastQty != null && lastPx != null) {
            b.setLastQty(lastQty).setLastPx(lastPx).setHasLast(true);
        }
        if (price != null) {
            b.setPrice(price).setHasPrice(true);
        }
        return b.build();
    }
}
