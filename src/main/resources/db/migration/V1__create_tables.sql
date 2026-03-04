-- Создание таблицы клиентов
CREATE TABLE IF NOT EXISTS clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы модераторов
CREATE TABLE IF NOT EXISTS moderators (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы рекламных кампаний
CREATE TABLE IF NOT EXISTS advertising_campaigns (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    daily_budget DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    client_id BIGINT NOT NULL REFERENCES clients(id)
);

-- Создание таблицы комментариев от модераторов для кампаний
CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    moderation_comment TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    moderator_id BIGINT NOT NULL REFERENCES moderators(id),
    campaign_id BIGINT NOT NULL REFERENCES advertising_campaigns(id) ON DELETE CASCADE
);

-- Создание таблицы транзакций
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES clients(id),
    amount DECIMAL(10,2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT
);

-- Создание индексов
CREATE INDEX idx_campaigns_client_id ON advertising_campaigns(client_id);
CREATE INDEX idx_campaigns_status ON advertising_campaigns(status);
CREATE INDEX idx_campaigns_start_date ON advertising_campaigns(start_date);
CREATE INDEX idx_campaigns_end_date ON advertising_campaigns(end_date);

CREATE INDEX idx_transactions_client_id ON transactions(client_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_type ON transactions(type);

CREATE INDEX idx_clients_api_key ON clients(api_key);
CREATE INDEX idx_moderators_api_key ON moderators(api_key);

-- Индексы для таблицы comments
CREATE INDEX idx_comments_campaign_id ON comments(campaign_id);
CREATE INDEX idx_comments_moderator_id ON comments(moderator_id);
CREATE INDEX idx_comments_created_at ON comments(created_at);

-- Составной индекс для частого запроса (получение комментариев по кампании с сортировкой)
CREATE INDEX idx_comments_campaign_created ON comments(campaign_id, created_at DESC);

-- Добавление тестовых данных
INSERT INTO clients (name, api_key, balance) VALUES
('Client One', 'client-key-1', 1000.00),
('Client Two', 'client-key-2', 500.00),
('Client Three', 'client-key-3', 50.00);

INSERT INTO moderators (name, api_key) VALUES
('Moderator One', 'moderator-key-1'),
('Moderator Two', 'moderator-key-2');
