create table if not exists pool_baseline
(
    name               string not null,
    liveness_interval  int    not null default 30,
    connection_timeout int    not null default 5 * 60,
    min_idle           int    not null default 0 CHECK (min_idle >= 0 and min_idle <= max_size),
    max_size           int    not null default 512 CHECK (max_size >= 0 and min_idle <= max_size),
    strategy           string not null default 'DYNAMIC_SIZE' CHECK (strategy in ('DYNAMIC_SIZE', 'FIXED_SIZE')),

    primary key (name)
);

comment on column pool_baseline.name is 'Descriptive name for pooling baseline.';
comment on column pool_baseline.liveness_interval is 'Update interval to signal pool instance liveness in seconds.';
comment on column pool_baseline.connection_timeout is 'Load balancer or server connection timeout in seconds.';
comment on column pool_baseline.min_idle is 'Minimum idle pool size overriding sizing strategy.';
comment on column pool_baseline.max_size is 'Maximum pool size overriding sizing strategy.';
comment on column pool_baseline.strategy is 'Pool sizing strategy.';

create table if not exists pool_metrics
(
    name                        string      not null,
    expired_at                  timestamptz not null default now() + '5 minutes',
    app_name                    string      null,
    app_version                 string      null,

    min_idle                    int         not null,
    max_size                    int         not null,
    idle_count                  int         not null,
    active_count                int         not null,
    total_connections           int         not null,
    threads_awaiting_connection int         not null,

    primary key (name)
) with (ttl_expiration_expression = 'expired_at', ttl_job_cron = '*/2 * * * *');

comment on column pool_metrics.name is 'Name uniquely identifying pool instances.';
comment on column pool_metrics.expired_at is 'Timestamp of instance expiry (liveness updates).';
comment on column pool_metrics.app_name is 'Application name for visibility.';
comment on column pool_metrics.app_version is 'Application version for visibility.';
comment on column pool_metrics.min_idle is 'Minimum number of idle connections in the pool.';
comment on column pool_metrics.max_size is 'Maximum number of active connections that can be allocated at the same time.';
comment on column pool_metrics.idle_count is 'Number of established but idle connections.';
comment on column pool_metrics.active_count is 'Current number of active connections that have been allocated from the data source.';
comment on column pool_metrics.total_connections is 'The total number of connections currently in the pool.';
comment on column pool_metrics.threads_awaiting_connection is 'The number of threads awaiting connections from the pool.';

INSERT INTO pool_baseline (name) VALUES ('default') ON CONFLICT DO NOTHING ;
