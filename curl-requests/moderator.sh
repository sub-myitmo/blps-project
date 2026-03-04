curl -X GET http://localhost:8080/api/moderator/campaigns/PENDING -H "Authorization: moderator-key-1"

curl -X POST http://localhost:8080/api/moderator/campaigns/1 -H "Authorization: moderator-key-1" -H "Content-Type: application/json" -d '{"action": "SIGN_DOC","comment": "Кампания проверена, отправлена на подписание клиенту"}'

curl -X POST http://localhost:8080/api/moderator/campaigns/2 -H "Authorization: moderator-key-1" -H "Content-Type: application/json" -d '{"action": "REJECT","comment": "Контент содержит запрещенные материалы"}'

curl -X POST http://localhost:8080/api/moderator/campaigns/3 -H "Authorization: moderator-key-1" -H "Content-Type: application/json" -d '{"action": "PAUSE","comment": "Нарушение правил после запуска. Приостановлено модератором"}'

curl -X GET http://localhost:8080/api/moderator/campaigns/1 -H "Authorization: moderator-key-1"

curl -X GET http://localhost:8080/api/moderator/campaigns/REJECTED -H "Authorization: moderator-key-1"

curl -X GET http://localhost:8080/api/moderator/campaigns/AT_SIGNING -H "Authorization: moderator-key-1"

curl -X GET http://localhost:8080/api/moderator/campaigns/PAUSED_BY_MODERATOR -H "Authorization: moderator-key-1"

curl -X GET http://localhost:8080/api/moderator/campaigns/ACTIVE -H "Authorization: moderator-key-1"

curl -X POST http://localhost:8080/api/moderator/campaigns/1 -H "Authorization: moderator-key-1" -H "Content-Type: application/json" -d '{"action": "INVALID_ACTION","comment": "Тест"}'