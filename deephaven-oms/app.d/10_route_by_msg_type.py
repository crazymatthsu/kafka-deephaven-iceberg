"""
Route raw_msgs into 4 base tables keyed appropriately:
  orders     -> last_by ClOrdId  (35=D)
  replaces   -> last_by ClOrdId  (35=G)
  cancels    -> last_by ClOrdId  (35=F)
  executions -> keyed by ExecId  (35=8) plus latest_exec_by_order = last_by(OrderId)

These names match TODO.md lines 60-73 and the orders_wexecs build in
20_build_orders_wexecs.py.
"""

# raw_msgs is defined in 00_kafka_source.py and shared across the app.d session.
orders = raw_msgs.where("MsgType == `D`").last_by("ClOrdId")
replaces = raw_msgs.where("MsgType == `G`").last_by("ClOrdId")
cancels = raw_msgs.where("MsgType == `F`").last_by("ClOrdId")
executions = raw_msgs.where("MsgType == `8`")  # one row per ExecId; ExecIds are unique
latest_exec_by_order = executions.last_by("OrderId")
