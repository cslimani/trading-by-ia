 SELECT market, count(*)
  FROM hot_spot 
  GROUP BY market;
  
WITH distinct_match AS (
  SELECT DISTINCT a.market AS market_a,
                  b.market AS market_b,
                  a.date_start
  FROM hot_spot a
  JOIN hot_spot b
    ON a.market <> b.market
   AND b.date_start BETWEEN a.date_start - INTERVAL '1 hours'
                        AND a.date_start + INTERVAL '1 hours'
),
counts AS (
  SELECT market, COUNT(*)::bigint AS n FROM hot_spot GROUP BY market
)
SELECT dm.market_a,
       dm.market_b,
       COUNT(*)::double precision / ca.n AS match_rate_from_A_to_B,
       COUNT(*) AS n_matched_rows_in_A,
       ca.n AS n_rows_in_A
FROM distinct_match dm
JOIN counts ca ON ca.market = dm.market_a
GROUP BY dm.market_a, dm.market_b, ca.n
ORDER BY match_rate_from_A_to_B DESC, dm.market_a, dm.market_b;


WITH markets AS (
  SELECT DISTINCT market m FROM hot_spot
),
 counts AS (
  SELECT COUNT(*)::numeric AS total
  FROM hot_spot join markets on markets.m=hot_spot.market
)
SELECT
  COUNT(*)::numeric / c.total AS isolated_rate,
  COUNT(*)                   AS n_isolated,
  c.total::bigint            AS n_total
FROM hot_spot a
join markets on markets.m=a.market
CROSS JOIN counts c
WHERE NOT EXISTS (
  SELECT 1
  FROM hot_spot b
  join markets on markets.m=b.market
  WHERE b.market <> a.market
    and market in ('US100', 'US500', 'US30')
    AND b.date_start BETWEEN a.date_start - INTERVAL '6 hours'
                         AND a.date_start + INTERVAL '6 hours'
)
GROUP BY c.total;
