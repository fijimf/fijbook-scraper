CREATE TABLE scrape_job
(
    id   BIGSERIAL PRIMARY KEY,
    update_or_fill VARCHAR(8) NOT NULL,
    season INT NOT NULL,
    model VARCHAR(24) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL
);

CREATE TABLE scrape_request
(
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES scrape_job(id),
    model_key VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    status_code INT NOT NULL,
    digest VARCHAR(48) NOT NULL,
    updates_proposed INT NOT NULL,
    updates_accepted INT NOT NULL
);

CREATE UNIQUE INDEX ON scrape_request(job_id, model_key);
