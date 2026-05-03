-- All NewOrderSingle rows for a given account, latest TransactTime first.
-- Usage: edit ACCT001 below or pass via -cmd "SET VARIABLE acct = 'ACCT005';"

SELECT
  ClOrdId,
  Symbol,
  Side,
  OrdType,
  OrderQty,
  Price,
  TransactTimeNanos
FROM ice('orders')
WHERE Account = 'ACCT001'
ORDER BY TransactTimeNanos DESC
LIMIT 100;
