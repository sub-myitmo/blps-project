
# 1. ПРОСМОТР КАМПАНИЙ НА МОДЕРАЦИИ
curl -X GET http://localhost:8080/api/moderator/campaigns/pending \
  -H "Authorization: moderator-key-1"

# 2. ОДОБРЕНИЕ КАМПАНИИ (APPROVE)
curl -X POST http://localhost:8080/api/moderator/campaigns/1/moderate \
  -H "Authorization: moderator-key-1" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "APPROVE",
    "comment": "Кампания соответствует правилам. Разрешено к публикации."
  }'

# 3. ОДОБРЕНИЕ ВТОРОЙ КАМПАНИИ
curl -X POST http://localhost:8080/api/moderator/campaigns/2/moderate \
  -H "Authorization: moderator-key-1" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "APPROVE",
    "comment": "Хороший контент, запускаем."
  }'

# 4. ОДОБРЕНИЕ ТРЕТЬЕЙ КАМПАНИИ
curl -X POST http://localhost:8080/api/moderator/campaigns/3/moderate \
  -H "Authorization: moderator-key-1" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "APPROVE",
    "comment": "Кампания одобрена."
  }'

# 5. ОТКЛОНЕНИЕ КАМПАНИИ (REJECT)
curl -X POST http://localhost:8080/api/moderator/campaigns/4/moderate \
  -H "Authorization: moderator-key-1" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "REJECT",
    "comment": "Контент содержит запрещенные материалы. Отказано."
  }'

# 6. ПРИОСТАНОВКА АКТИВНОЙ КАМПАНИИ МОДЕРАТОРОМ
curl -X POST http://localhost:8080/api/moderator/campaigns/1/pause \
  -H "Authorization: moderator-key-1" \
  -H "Content-Type: application/json" \
  -d '"Нарушение правил после запуска. Приостановлено модератором."'

# 7. ВОЗОБНОВЛЕНИЕ КАМПАНИИ МОДЕРАТОРОМ
curl -X POST http://localhost:8080/api/moderator/campaigns/1/resume \
  -H "Authorization: moderator-key-1"

# 8. ПОВТОРНЫЙ ПРОСМОТР КАМПАНИЙ
curl -X GET http://localhost:8080/api/moderator/campaigns/pending \
  -H "Authorization: moderator-key-1"

# 9. ПРИОСТАНОВКА КАМПАНИИ КЛИЕНТА 2 МОДЕРАТОРОМ
curl -X POST http://localhost:8080/api/moderator/campaigns/2/pause \
  -H "Authorization: moderator-key-1" \
  -H "Content-Type: application/json" \
  -d '"Подозрительная активность. Проверка."'

# 10. ПОПЫТКА МОДЕРИРОВАТЬ УЖЕ ОДОБРЕННУЮ КАМПАНИЮ (ошибка)
curl -X POST http://localhost:8080/api/moderator/campaigns/1/moderate \
  -H "Authorization: moderator-key-1" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "APPROVE",
    "comment": "Повторная модерация"
  }'

