-- Filter orders by all three: account, symbol, and ClOrdID prefix.

SELECT
  o.ClOrdId,
  o.Account,
  o.Symbol,
  o.Side,
  o.OrdType,
  o.OrderQty,
  o.Price,
  o.TransactTimeNanos
FROM ice('orders') o
WHERE o.Account = 'ACCT001'
  AND o.Symbol = 'AAPL'
  AND o.ClOrdId LIKE 'CL-%'
ORDER BY o.TransactTimeNanos DESC
LIMIT 100;
