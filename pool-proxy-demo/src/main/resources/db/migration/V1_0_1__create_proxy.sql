create table pool_baseline
(
    name               string not null,
    liveness_interval  int    not null default 30,
    connection_timeout int    not null default 5 * 60,
    min_idle           int    not null default 0 CHECK (min_idle >= 0 and min_idle <= max_size),
    max_size           int    not null default 512 CHECK (max_size >= 0 and min_idle <= max_size),
    strategy           string not null default 'DYNAMIC_SIZE' CHECK (strategy in ('DYNAMIC_SIZE', 'FIXED_SIZE')),

    primary key (name)
    );

create table pool_metrics
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

INSERT INTO pool_baseline (name) VALUES ('default') ON CONFLICT DO NOTHING ;
