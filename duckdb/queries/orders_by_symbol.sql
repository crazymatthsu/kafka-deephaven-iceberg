-- All NewOrderSingle rows for a given symbol.

SELECT
  ClOrdId,
  Account,
  Side,
  OrdType,
  OrderQty,
  Price,
  TransactTimeNanos
FROM ice('orders')
WHERE Symbol = 'AAPL'
ORDER BY TransactTimeNanos DESC
LIMIT 100;
