CREATE VIEW vw_saldo_cliente AS
SELECT c.id AS cliente_id, c.nome,
       COALESCE(SUM(l.valor) FILTER (WHERE l.natureza = 'DEBITO'),  0)
     - COALESCE(SUM(l.valor) FILTER (WHERE l.natureza = 'CREDITO'), 0) AS saldo_devedor
FROM clientes c
LEFT JOIN lancamentos l ON l.cliente_id = c.id
GROUP BY c.id, c.nome;
