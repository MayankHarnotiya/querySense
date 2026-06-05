-- Read-only user for the analytics database (second safety wall)
CREATE USER analytics_readonly WITH PASSWORD 'readonly';
GRANT CONNECT ON DATABASE analytics TO analytics_readonly;

\connect analytics

GRANT USAGE ON SCHEMA public TO analytics_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO analytics_readonly;