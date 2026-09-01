#!/bin/bash
set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "Checking API health at $BASE_URL/actuator/health..."
until curl -s "$BASE_URL/actuator/health" | grep UP > /dev/null; do
  echo "Waiting for API to become ready..."
  sleep 1
done

echo "API is ready. Seeding test data..."

P1_ID="01952e42-7a57-7000-8000-000000000001"
curl -s -X POST "$BASE_URL/products" \
  -H "Content-Type: application/json" \
  -d '{"id":"'"$P1_ID"'","name":"Zapatillas Running Pro","description":"Calzado de alto rendimiento amortiguado"}' > /dev/null

seed_monthly_prices() {
  local product_id="$1"
  local currency="$2"
  local start_year="$3"
  local end_year="$4"
  local base_price="$5"
  local price_step="$6"
  local count=0

  for year in $(seq "$start_year" "$end_year"); do
    for month in 01 02 03 04 05 06 07 08 09 10 11 12; do
      case "$month" in
        01|03|05|07|08|10|12) last_day=31 ;;
        04|06|09|11) last_day=30 ;;
        02) last_day=28 ;;
      esac
      init_date="${year}-${month}-01"
      end_date="${year}-${month}-${last_day}"
      price_val=$(awk -v b="$base_price" -v s="$price_step" -v c="$count" 'BEGIN { printf "%.2f", b + (c * s) }')

      curl -s -X POST "$BASE_URL/products/$product_id/prices" \
        -H "Content-Type: application/json" \
        -d '{"value":'"$price_val"',"currency":"'"$currency"'","initDate":"'"$init_date"'","endDate":"'"$end_date"'"}' > /dev/null

      count=$((count + 1))
    done
  done
}

seed_monthly_prices "$P1_ID" "EUR" 2023 2026 89.99 1.50
seed_monthly_prices "$P1_ID" "USD" 2024 2026 99.99 1.50

P2_ID="01952e42-7a57-7000-8000-000000000002"
curl -s -X POST "$BASE_URL/products" \
  -H "Content-Type: application/json" \
  -d '{"id":"'"$P2_ID"'","name":"Camiseta Técnica DryFit","description":"Tejido transpirable para entrenamiento"}' > /dev/null

curl -s -X POST "$BASE_URL/products/$P2_ID/prices" \
  -H "Content-Type: application/json" \
  -d '{"value":39.99,"currency":"EUR","initDate":"2024-01-01","endDate":"2024-12-31"}' > /dev/null

curl -s -X POST "$BASE_URL/products/$P2_ID/prices" \
  -H "Content-Type: application/json" \
  -d '{"value":49.99,"currency":"EUR","initDate":"2025-01-01","endDate":null}' > /dev/null

P3_ID="01952e42-7a57-7000-8000-000000000003"
curl -s -X POST "$BASE_URL/products" \
  -H "Content-Type: application/json" \
  -d '{"id":"'"$P3_ID"'","name":"Mochila Senderismo 30L","description":"Impermeable con compartimentos múltiples"}' > /dev/null

echo "Data seeding completed successfully!"
echo "---------------------------------------------------------"
echo "Seeded Products:"
echo "1. Product 1: $P1_ID (Running Pro - 48 EUR prices, 36 USD prices)"
echo "2. Product 2: $P2_ID (Camiseta DryFit - 1 closed price, 1 open-ended active price)"
echo "3. Product 3: $P3_ID (Mochila Senderismo - 0 prices)"
echo "---------------------------------------------------------"
echo "Open Swagger UI in your browser: http://localhost:8080/swagger-ui.html"
