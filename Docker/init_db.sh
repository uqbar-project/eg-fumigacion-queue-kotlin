#!/bin/bash
set -e

# Crear la base y el usuario
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    CREATE DATABASE facturacion_db;
    CREATE USER docker WITH ENCRYPTED PASSWORD 'docker';
    GRANT ALL PRIVILEGES ON DATABASE facturacion_db TO docker;
EOSQL

# Ahora sí: habilitar PostGIS en *facturacion_db*
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "facturacion_db" <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS postgis;
EOSQL
