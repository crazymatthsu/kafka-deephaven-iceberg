-- Full lifecycle of a given parent OrderId across all four base tables.
-- Replace the OrderId literal below.

WITH oid AS (SELECT 'OID-0000002a-1' AS OrderId)
SELECT 'execution' AS source, e.ClOrdId, e.OrderId, e.ExecId, e.ExecType, e.OrdStatus,
       e.CumQty, e.LeavesQty, e.LastQty, e.LastPx, e.TransactTimeNanos
FROM ice('executions') e, oid WHERE e.OrderId = oid.OrderId
UNION ALL
SELECT 'order', o.ClOrdId, NULL, NULL, NULL, NULL,
       NULL, NULL, NULL, NULL, o.TransactTimeNanos
FROM ice('orders') o, oid
WHERE o.ClOrdId IN (SELECT DISTINCT ClOrdId FROM ice('executions') WHERE OrderId = oid.OrderId)
UNION ALL
SELECT 'replace', r.ClOrdId, NULL, NULL, NULL, NULL,
       NULL, NULL, NULL, NULL, r.TransactTimeNanos
FROM ice('replaces') r, oid
WHERE r.ClOrdId IN (SELECT DISTINCT ClOrdId FROM ice('executions') WHERE OrderId = oid.OrderId)
UNION ALL
SELECT 'cancel', c.ClOrdId, NULL, NULL, NULL, NULL,
       NULL, NULL, NULL, NULL, c.TransactTimeNanos
FROM ice('cancels') c, oid
WHERE c.ClOrdId IN (SELECT DISTINCT ClOrdId FROM ice('executions') WHERE OrderId = oid.OrderId)
ORDER BY TransactTimeNanos;
