"""
Build orders_wexecs — one row per parent OrderId carrying the latest aggregated state:

  account, symbol, side, ord_type,
  qty fields  : OrderQty, CumQty, LeavesQty
  price fields: Price (limit), LastPx, AvgPx (if available)
  link fields : ClOrdId (latest), OrigClOrdId chain via replaces, OrderId
  TransactTime

Pipeline:
  1. orders ⋈ executions on ClOrdId → seed the (ClOrdId -> OrderId) mapping.
  2. update_view to copy through original NewOrderSingle qty/price + latest exec state.
  3. as-of join replaces/cancels on (OrderId, TransactTime) so the latest amend wins.
  4. last_by(OrderId) — one current row per parent.
"""

# Inputs declared in 10_route_by_msg_type.py: orders, replaces, cancels, executions, latest_exec_by_order

orders_with_oid = orders.natural_join(
    table=latest_exec_by_order.view(["ClOrdId", "OrderId"]),
    on=["ClOrdId"],
)

# Latest replace per OrderId, joined back to executions which carry the OrderId.
latest_replace = (
    executions.view(["ClOrdId", "OrderId", "TransactTimeNanos"])
    .natural_join(replaces, on=["ClOrdId"], joins=["ReplaceOrderQty=OrderQty", "ReplacePrice=Price", "ReplaceTime=TransactTimeNanos"])
    .last_by("OrderId")
)

# Same idea for cancels.
latest_cancel = (
    executions.view(["ClOrdId", "OrderId"])
    .natural_join(cancels, on=["ClOrdId"], joins=["CancelClOrdId=ClOrdId", "CancelTime=TransactTimeNanos"])
    .last_by("OrderId")
)

orders_wexecs = (
    orders_with_oid
    .natural_join(latest_exec_by_order.drop_columns(["MsgType"]), on=["OrderId"],
                  joins=["LatestExecId=ExecId", "LatestExecType=ExecType",
                         "LatestOrdStatus=OrdStatus", "CumQty", "LeavesQty",
                         "LastPx", "LastQty", "ExecTransactTime=TransactTimeNanos"])
    .natural_join(latest_replace, on=["OrderId"],
                  joins=["ReplaceOrderQty", "ReplacePrice", "ReplaceTime"])
    .natural_join(latest_cancel, on=["OrderId"],
                  joins=["CancelClOrdId", "CancelTime"])
    .last_by("OrderId")
)
