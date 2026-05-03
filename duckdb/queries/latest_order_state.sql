-- Recompute the orders_wexecs view server-side from the four base Iceberg tables.
-- Should match the orders_wexecs Iceberg table row-for-row when joined on OrderId.

WITH
exec_per_order AS (
  SELECT *
  FROM (
    SELECT
      *,
      ROW_NUMBER() OVER (PARTITION BY OrderId ORDER BY TransactTimeNanos DESC) AS rn
    FROM ice('executions')
  )
  WHERE rn = 1
),
replace_per_order AS (
  SELECT e.OrderId,
         r.OrderQty AS ReplaceOrderQty,
         r.Price    AS ReplacePrice,
         r.TransactTimeNanos AS ReplaceTime
  FROM ice('replaces') r
  JOIN ice('executions') e USING (ClOrdId)
  QUALIFY ROW_NUMBER() OVER (PARTITION BY e.OrderId ORDER BY r.TransactTimeNanos DESC) = 1
),
cancel_per_order AS (
  SELECT e.OrderId,
         c.ClOrdId AS CancelClOrdId,
         c.TransactTimeNanos AS CancelTime
  FROM ice('cancels') c
  JOIN ice('executions') e USING (ClOrdId)
  QUALIFY ROW_NUMBER() OVER (PARTITION BY e.OrderId ORDER BY c.TransactTimeNanos DESC) = 1
)
SELECT
  o.Account,
  o.Symbol,
  o.Side,
  o.OrdType,
  e.OrderId,
  o.ClOrdId AS OriginalClOrdId,
  e.ClOrdId AS LatestClOrdId,
  o.OrderQty AS InitialOrderQty,
  e.OrderQty AS CurrentOrderQty,
  e.CumQty,
  e.LeavesQty,
  e.LastPx,
  e.LastQty,
  o.Price AS InitialPrice,
  rep.ReplacePrice,
  e.OrdStatus,
  e.ExecType,
  o.TransactTimeNanos AS OriginalTime,
  e.TransactTimeNanos AS LatestExecTime,
  rep.ReplaceTime,
  cnc.CancelTime
FROM ice('orders') o
JOIN exec_per_order e
  ON o.ClOrdId = e.ClOrdId
   OR o.ClOrdId = (SELECT first(ClOrdId) FROM ice('executions') WHERE OrderId = e.OrderId)
LEFT JOIN replace_per_order rep ON rep.OrderId = e.OrderId
LEFT JOIN cancel_per_order cnc  ON cnc.OrderId = e.OrderId
ORDER BY e.OrderId
LIMIT 1000;
