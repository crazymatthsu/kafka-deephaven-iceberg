package com.oms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;

class ProtobufRoundTripTest {

    private static Header header(String clOrdId, String msgType) {
        return Header.newBuilder()
                .setClOrdId(clOrdId)
                .setAccount("ACCT001")
                .setSymbol("AAPL")
                .setMsgType(msgType)
                .setSenderCompId("SIM")
                .setTargetCompId("OMS")
                .setTransactTime(Timestamp.newBuilder().setSeconds(1_700_000_000L).build())
                .build();
    }

    @Test
    void newOrderSingleRoundTrip() throws InvalidProtocolBufferException {
        NewOrderSingle msg = NewOrderSingle.newBuilder()
                .setHeader(header("CL-1", "D"))
                .setSide(Side.SIDE_BUY)
                .setOrdType(OrdType.ORD_TYPE_LIMIT)
                .setTimeInForce(TimeInForce.TIF_DAY)
                .setOrderQty(100)
                .setPrice(150.25)
                .setHasPrice(true)
                .build();

        NewOrderSingle parsed = NewOrderSingle.parseFrom(msg.toByteArray());
        assertThat(parsed).isEqualTo(msg);
        assertThat(parsed.getHeader().getClOrdId()).isEqualTo("CL-1");
    }

    @Test
    void orderCancelReplaceRoundTrip() throws InvalidProtocolBufferException {
        OrderCancelReplaceRequest msg = OrderCancelReplaceRequest.newBuilder()
                .setHeader(header("CL-2", "G"))
                .setOrigClOrdId("CL-1")
                .setSide(Side.SIDE_BUY)
                .setOrdType(OrdType.ORD_TYPE_LIMIT)
                .setTimeInForce(TimeInForce.TIF_DAY)
                .setOrderQty(150)
                .setPrice(151.00)
                .setHasPrice(true)
                .build();
        assertThat(OrderCancelReplaceRequest.parseFrom(msg.toByteArray())).isEqualTo(msg);
    }

    @Test
    void orderCancelRoundTrip() throws InvalidProtocolBufferException {
        OrderCancelRequest msg = OrderCancelRequest.newBuilder()
                .setHeader(header("CL-3", "F"))
                .setOrigClOrdId("CL-2")
                .setSide(Side.SIDE_BUY)
                .setOrderQty(150)
                .build();
        assertThat(OrderCancelRequest.parseFrom(msg.toByteArray())).isEqualTo(msg);
    }

    @Test
    void executionReportRoundTrip() throws InvalidProtocolBufferException {
        ExecutionReport msg = ExecutionReport.newBuilder()
                .setHeader(header("CL-1", "8"))
                .setOrderId("OID-1")
                .setExecId("EX-1")
                .setExecType(ExecType.EXEC_TYPE_PARTIAL_FILL)
                .setOrdStatus(OrdStatus.ORD_STATUS_PARTIALLY_FILLED)
                .setSide(Side.SIDE_BUY)
                .setOrdType(OrdType.ORD_TYPE_LIMIT)
                .setOrderQty(100)
                .setCumQty(40)
                .setLeavesQty(60)
                .setLastQty(40)
                .setLastPx(150.10)
                .setPrice(150.25)
                .setHasPrice(true)
                .setHasLast(true)
                .build();

        ExecutionReport parsed = ExecutionReport.parseFrom(msg.toByteArray());
        assertThat(parsed).isEqualTo(msg);
        assertThat(parsed.getOrderQty()).isEqualTo(parsed.getCumQty() + parsed.getLeavesQty());
    }
}
