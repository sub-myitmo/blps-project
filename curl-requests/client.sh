curl -X POST http://localhost:8080/api/client/campaigns -H "Authorization: client-key-1" -H "Content-Type: application/json" -d '{"name": "Новогодняя распродажа","content": "Скидки до 70% на все товары!","targetUrl": "https://shop.ru/new-year","dailyBudget": 150.00,"startDate": "'$(date -d "+1 day" +"%Y-%m-%dT00:00:00")'","endDate": "'$(date -d "+1 month" +"%Y-%m-%dT23:59:59")'"}'

curl -X POST http://localhost:8080/api/client/campaigns -H "Authorization: client-key-2" -H "Content-Type: application/json" -d '{"name": "Летний фестиваль","content": "Фестиваль на открытом воздухе!","targetUrl": "https://shop.ru/summer-fest","dailyBudget": 200.00,"startDate": "'$(date -d "+2 days" +"%Y-%m-%dT00:00:00")'","endDate": "'$(date -d "+3 months" +"%Y-%m-%dT23:59:59")'"}'

curl -X POST http://localhost:8080/api/client/campaigns -H "Authorization: client-key-3" -H "Content-Type: application/json" -d '{"name": "Акция выходного дня","content": "Только в субботу и воскресенье!","targetUrl": "https://shop.ru/weekend","dailyBudget": 300.00,"startDate": "'$(date -d "+3 days" +"%Y-%m-%dT00:00:00")'","endDate": "'$(date -d "+1 week" +"%Y-%m-%dT23:59:59")'"}'

curl -X GET http://localhost:8080/api/client/campaigns/1 -H "Authorization: client-key-1"

curl -X POST http://localhost:8080/api/client/campaigns/1 -H "Authorization: client-key-1" -H "Content-Type: application/json" -d '{"action": "SIGN_DOC"}'

curl -X POST http://localhost:8080/api/client/campaigns/2 -H "Authorization: client-key-2" -H "Content-Type: application/json" -d '{"action": "PAUSE"}'

curl -X POST http://localhost:8080/api/client/campaigns/2 -H "Authorization: client-key-2" -H "Content-Type: application/json" -d '{"action": "RESUME","startDate": "'$(date -d "+5 days" +"%Y-%m-%dT00:00:00")'","endDate": "'$(date -d "+4 months" +"%Y-%m-%dT23:59:59")'"}'

curl -X POST http://localhost:8080/api/payment/pay -H "Authorization: client-key-3" -H "Content-Type: application/json" -d '{"amount": 500.00,"description": "Пополнение через банковскую карту"}'

curl -X GET http://localhost:8080/api/payment/balance -H "Authorization: client-key-1"

curl -X GET http://localhost:8080/api/payment/balance -H "Authorization: client-key-3"

curl -X GET http://localhost:8080/api/client/campaigns/3 -H "Authorization: client-key-1"

curl -X POST http://localhost:8080/api/client/campaigns -H "Authorization: client-key-1" -H "Content-Type: application/json" -d '{"name": "Невалидная кампания","content": "Тест","targetUrl": "https://shop.ru/test","dailyBudget": 100.00,"startDate": "2020-01-01T00:00:00"}'