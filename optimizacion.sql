ALTER TABLE clientes ADD COLUMN parcela_geometria geometry(Polygon, 4326);

CREATE INDEX idx_clientes_parcela_espacial ON clientes USING gist(parcela_geometria);

// y después

@Query(nativeQuery = true, value = """
  SELECT * FROM clientes cliente
  WHERE ST_Contains(cliente.parcela_geometria, ST_MakePoint(:x, :y))
  LIMIT 1
""")
fun findByCoordenadasContienePunto(@Param("x") x: Double, @Param("y") y: Double): Cliente?
