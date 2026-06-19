CREATE TABLE payments (
    id                     UUID PRIMARY KEY,
    transaction_reference  VARCHAR(64)    NOT NULL,
    amount                 NUMERIC(19,2)  NOT NULL,
    phone_number           VARCHAR(20)    NOT NULL,
    payment_method         VARCHAR(20)    NOT NULL,
    status                 VARCHAR(20)    NOT NULL,
    created_at             TIMESTAMP      NOT NULL,
    updated_at             TIMESTAMP      NOT NULL,

    CONSTRAINT uq_payments_transaction_reference UNIQUE (transaction_reference)
);

CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at);
