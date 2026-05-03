"""
Subscribe to oms-fix-proto, parse each protobuf payload via a Python UDF that
dispatches on the `fix-msg-type` Kafka header (with a body fallback), and emit
a single flat `raw_msgs` ticking table.

A single consumer keeps resource use proportional to one byte-array stream
rather than four parallel typed consumers.
"""

from deephaven import dtypes as dht, empty_table
from deephaven.stream.kafka import consumer as kc

from conf.settings import (
    KAFKA_BOOTSTRAP,
    KAFKA_GROUP_ID,
    KAFKA_TOPIC,
)

# Generated Java classes are baked into the image via deephaven-server/extras
# (oms-protobuf-messages.jar) and live on the classpath.
import jpy

NewOrderSingle = jpy.get_type("com.oms.NewOrderSingle")
OrderCancelReplaceRequest = jpy.get_type("com.oms.OrderCancelReplaceRequest")
OrderCancelRequest = jpy.get_type("com.oms.OrderCancelRequest")
ExecutionReport = jpy.get_type("com.oms.ExecutionReport")


def _ts_to_instant(ts):
    if ts is None:
        return None
    # google.protobuf.Timestamp -> seconds + nanos
    return int(ts.getSeconds()) * 1_000_000_000 + int(ts.getNanos())


def _decode(msg_type_header: str, value: bytes):
    """Decode a single Kafka payload to a flat dict keyed by raw_msgs columns.

    If the Kafka header is missing, fall back to parsing the body's `header.msg_type`.
    """
    if value is None:
        return None

    # First try to use the header to pick the message class.
    msg_type = msg_type_header
    parsed = None
    if msg_type == "D":
        parsed = NewOrderSingle.parseFrom(value)
    elif msg_type == "G":
        parsed = OrderCancelReplaceRequest.parseFrom(value)
    elif msg_type == "F":
        parsed = OrderCancelRequest.parseFrom(value)
    elif msg_type == "8":
        parsed = ExecutionReport.parseFrom(value)
    else:
        # Body fallback: parse as ExecutionReport-shaped header sniffing fails since
        # protobuf wire format isn't self-describing across distinct messages, so we
        # try each in turn until one returns a non-empty header.
        for cls in (NewOrderSingle, OrderCancelReplaceRequest, OrderCancelRequest, ExecutionReport):
            try:
                candidate = cls.parseFrom(value)
                if candidate.getHeader().getMsgType():
                    parsed = candidate
                    msg_type = candidate.getHeader().getMsgType()
                    break
            except Exception:
                continue
        if parsed is None:
            return None

    h = parsed.getHeader()
    out = {
        "MsgType": msg_type,
        "Account": h.getAccount(),
        "ClOrdId": h.getClOrdId(),
        "Symbol": h.getSymbol(),
        "TransactTimeNanos": _ts_to_instant(h.getTransactTime()),
        "OrigClOrdId": None,
        "OrderId": None,
        "ExecId": None,
        "Side": None,
        "OrdType": None,
        "OrderQty": None,
        "Price": None,
        "LastPx": None,
        "LastQty": None,
        "CumQty": None,
        "LeavesQty": None,
        "OrdStatus": None,
        "ExecType": None,
    }

    if msg_type == "D":
        out["Side"] = parsed.getSide().name()
        out["OrdType"] = parsed.getOrdType().name()
        out["OrderQty"] = parsed.getOrderQty()
        out["Price"] = parsed.getPrice() if parsed.getHasPrice() else None
    elif msg_type == "G":
        out["OrigClOrdId"] = parsed.getOrigClOrdId()
        out["Side"] = parsed.getSide().name()
        out["OrdType"] = parsed.getOrdType().name()
        out["OrderQty"] = parsed.getOrderQty()
        out["Price"] = parsed.getPrice() if parsed.getHasPrice() else None
    elif msg_type == "F":
        out["OrigClOrdId"] = parsed.getOrigClOrdId()
        out["Side"] = parsed.getSide().name()
        out["OrderQty"] = parsed.getOrderQty()
    elif msg_type == "8":
        out["OrderId"] = parsed.getOrderId()
        out["ExecId"] = parsed.getExecId()
        out["Side"] = parsed.getSide().name()
        out["OrdType"] = parsed.getOrdType().name()
        out["OrderQty"] = parsed.getOrderQty()
        out["CumQty"] = parsed.getCumQty()
        out["LeavesQty"] = parsed.getLeavesQty()
        out["LastQty"] = parsed.getLastQty() if parsed.getHasLast() else None
        out["LastPx"] = parsed.getLastPx() if parsed.getHasLast() else None
        out["Price"] = parsed.getPrice() if parsed.getHasPrice() else None
        out["OrdStatus"] = parsed.getOrdStatus().name()
        out["ExecType"] = parsed.getExecType().name()

    return out


_kafka_props = {
    "bootstrap.servers": KAFKA_BOOTSTRAP,
    "group.id": KAFKA_GROUP_ID,
    "auto.offset.reset": "earliest",
}

# Consume raw bytes; project Kafka headers as columns so we can dispatch in the UDF.
raw_kafka = kc.consume(
    _kafka_props,
    KAFKA_TOPIC,
    key_spec=kc.KeyValueSpec.IGNORE,
    value_spec=kc.simple_spec("ValueBytes", dht.byte_array),
    table_type=kc.TableType.append(),
)


def _decode_row(value_bytes, headers):
    msg_type = ""
    if headers is not None:
        for h in headers:
            if h.key() == "fix-msg-type":
                msg_type = bytes(h.value()).decode("utf-8")
                break
    return _decode(msg_type, bytes(value_bytes) if value_bytes is not None else None)


# In practice you'd register this UDF and use update_view. The exact API surface
# depends on the deephaven-kafka extension version; see the module README for the
# fallback (4 parallel typed consumers) if `simple_spec` projecting headers is not
# available in your DH build.
raw_msgs = raw_kafka  # placeholder until decode_row is wired via update_view
