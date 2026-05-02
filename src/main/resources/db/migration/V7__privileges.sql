CREATE TABLE IF NOT EXISTS privileges (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS role_privileges (
    role         VARCHAR(32) NOT NULL,
    privilege_id BIGINT NOT NULL REFERENCES privileges(id) ON DELETE CASCADE,
    PRIMARY KEY (role, privilege_id),
    CONSTRAINT chk_role_privileges_role CHECK (role IN ('CLIENT','MANAGER','ADMIN'))
);

CREATE INDEX IF NOT EXISTS idx_role_privileges_role ON role_privileges(role);

INSERT INTO privileges(code, description) VALUES
    ('CAMPAIGN_CREATE',        'Создать собственную кампанию'),
    ('CAMPAIGN_UPDATE_OWN',    'Обновить собственную кампанию'),
    ('CAMPAIGN_DELETE_OWN',    'Удалить собственную кампанию'),
    ('CAMPAIGN_VIEW_OWN',      'Просмотреть собственную кампанию'),
    ('CAMPAIGN_SIGN_CLIENT',   'Подписать кампанию как клиент'),
    ('CAMPAIGN_PAUSE_OWN',     'Приостановить собственную кампанию'),
    ('PAYMENT_TOPUP',          'Пополнить баланс'),
    ('PAYMENT_VIEW_OWN',       'Просмотреть свой баланс'),
    ('CAMPAIGN_VIEW_ALL',      'Просмотреть все кампании'),
    ('CAMPAIGN_MODERATE_SIGN', 'Подписать кампанию как модератор'),
    ('CAMPAIGN_REJECT',        'Отклонить кампанию'),
    ('CAMPAIGN_PAUSE_ANY',     'Приостановить любую кампанию'),
    ('CAMPAIGN_DELETE_ANY',    'Удалить любую кампанию'),
    ('COMMENT_CREATE',         'Создать модерационный комментарий'),
    ('COMMENT_DELETE_OWN',     'Удалить собственный комментарий'),
    ('COMMENT_DELETE_ANY',     'Удалить любой комментарий'),
    ('CLIENT_DELETE_ANY',      'Soft-delete клиента'),
    ('USER_MANAGE',            'Создавать учётные записи пользователей'),
    ('USER_DELETE',            'Soft-delete пользователей'),
    ('PRIVILEGE_VIEW',         'Просмотр каталога привилегий'),
    ('PRIVILEGE_ASSIGN',       'Назначение привилегий ролям')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_privileges(role, privilege_id)
SELECT 'CLIENT', id FROM privileges WHERE code IN (
    'CAMPAIGN_CREATE', 'CAMPAIGN_UPDATE_OWN', 'CAMPAIGN_DELETE_OWN',
    'CAMPAIGN_VIEW_OWN', 'CAMPAIGN_SIGN_CLIENT', 'CAMPAIGN_PAUSE_OWN',
    'PAYMENT_TOPUP', 'PAYMENT_VIEW_OWN'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_privileges(role, privilege_id)
SELECT 'MANAGER', id FROM privileges WHERE code IN (
    'CAMPAIGN_VIEW_ALL', 'CAMPAIGN_MODERATE_SIGN', 'CAMPAIGN_REJECT',
    'CAMPAIGN_PAUSE_ANY', 'CAMPAIGN_DELETE_ANY',
    'COMMENT_CREATE', 'COMMENT_DELETE_OWN',
    'CLIENT_DELETE_ANY'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_privileges(role, privilege_id)
SELECT 'ADMIN', id FROM privileges
ON CONFLICT DO NOTHING;
