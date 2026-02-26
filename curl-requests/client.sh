
# 1. СОЗДАНИЕ РЕКЛАМНОЙ КАМПАНИИ (без дат)
curl -X POST http://localhost:8080/api/client/campaigns \
  -H "Authorization: client-key-1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Новогодняя распродажа",
    "content": "Скидки до 70% на все товары! Только до 31 января!",
    "targetUrl": "https://shop.ru/new-year",
    "dailyBudget": 150.00
  }'

# 2. СОЗДАНИЕ КАМПАНИИ С ДАТАМИ
curl -X POST http://localhost:8080/api/client/campaigns \
  -H "Authorization: client-key-1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Летний фестиваль",
    "content": "Фестиваль на открытом воздухе! Ждем всех!",
    "targetUrl": "https://shop.ru/summer-fest",
    "dailyBudget": 200.00,
    "startDate": "2024-06-01T00:00:00",
    "endDate": "2024-08-31T23:59:59"
  }'

# 3. СОЗДАНИЕ КАМПАНИИ КЛИЕНТОМ 2
curl -X POST http://localhost:8080/api/client/campaigns \
  -H "Authorization: client-key-2" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Осенняя коллекция",
    "content": "Новая коллекция осень-зима уже в магазине!",
    "targetUrl": "https://shop.ru/autumn",
    "dailyBudget": 80.00
  }'

# 4. СОЗДАНИЕ КАМПАНИИ КЛИЕНТОМ 3 (мало денег)
curl -X POST http://localhost:8080/api/client/campaigns \
  -H "Authorization: client-key-3" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Акция выходного дня",
    "content": "Только в субботу и воскресенье!",
    "targetUrl": "https://shop.ru/weekend",
    "dailyBudget": 300.00
  }'

# 5. ПРИОСТАНОВКА КАМПАНИИ (PAUSE)
curl -X POST http://localhost:8080/api/client/campaigns/1/pause \
  -H "Authorization: client-key-1"

# 6. ВОЗОБНОВЛЕНИЕ КАМПАНИИ (RESUME)
curl -X POST http://localhost:8080/api/client/campaigns/1/resume \
  -H "Authorization: client-key-1"

# 7. ПОПОЛНЕНИЕ БАЛАНСА (PAY)
curl -X POST http://localhost:8080/api/payment/pay \
  -H "Authorization: client-key-3" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500.00,
    "description": "Пополнение через банковскую карту"
  }'

# 8. ПРОВЕРКА БАЛАНСА
curl -X GET http://localhost:8080/api/payment/balance \
  -H "Authorization: client-key-1"

# 9. ПРОВЕРКА БАЛАНСА КЛИЕНТА 3
curl -X GET http://localhost:8080/api/payment/balance \
  -H "Authorization: client-key-3"

# 10. ПОВТОРНАЯ ПРИОСТАНОВКА
curl -X POST http://localhost:8080/api/client/campaigns/2/pause \
  -H "Authorization: client-key-1"

# 11. ПОПЫТКА ПРИОСТАНОВИТЬ ЧУЖУЮ КАМПАНИЮ (ошибка)
curl -X POST http://localhost:8080/api/client/campaigns/3/pause \
  -H "Authorization: client-key-1"

# 12. МАЛЕНЬКОЕ ПОПОЛНЕНИЕ
curl -X POST http://localhost:8080/api/payment/pay \
  -H "Authorization: client-key-2" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 25.00,
    "description": "Маленькое пополнение"
  }'