import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const activePriceLatency = new Trend('active_price_latency', true);
const priceHistoryLatency = new Trend('price_history_latency', true);
const createProductLatency = new Trend('create_product_latency', true);
const addPriceLatency = new Trend('add_price_latency', true);

export const options = {
  scenarios: {
    active_price_traffic: {
      executor: 'ramping-arrival-rate',
      startRate: 100,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 200,
      stages: [
        { duration: '5s', target: 500 },
        { duration: '15s', target: 1500 },
        { duration: '10s', target: 2000 },
        { duration: '5s', target: 500 },
      ],
      exec: 'queryActivePrice',
    },
    price_history_traffic: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      preAllocatedVUs: 30,
      maxVUs: 100,
      stages: [
        { duration: '5s', target: 200 },
        { duration: '15s', target: 600 },
        { duration: '10s', target: 800 },
        { duration: '5s', target: 200 },
      ],
      exec: 'queryPriceHistory',
    },
    mutation_traffic: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 10,
      maxVUs: 50,
      stages: [
        { duration: '5s', target: 50 },
        { duration: '15s', target: 150 },
        { duration: '10s', target: 200 },
        { duration: '5s', target: 50 },
      ],
      exec: 'createProductAndPrice',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<30', 'p(99)<60'],
    active_price_latency: ['p(95)<15'],
    price_history_latency: ['p(95)<25'],
  },
};

function fetchMetric(name) {
  try {
    const res = http.get(`${BASE_URL}/actuator/metrics/${name}`);
    if (res.status === 200) {
      const body = JSON.parse(res.body);
      if (body.measurements && body.measurements.length > 0) {
        return body.measurements[0].value;
      }
    }
  } catch (e) {
    // ignore
  }
  return null;
}

export function setup() {
  const startWait = Date.now();
  let healthy = false;
  let attempts = 0;

  while (!healthy && attempts < 60) {
    try {
      const res = http.get(`${BASE_URL}/actuator/health`);
      if (res.status === 200 && res.body.includes('UP')) {
        healthy = true;
        break;
      }
    } catch (e) {
      // connecting
    }
    attempts++;
    sleep(1);
  }

  const startupDurationMs = Date.now() - startWait;

  const initialMemoryBytes = fetchMetric('jvm.memory.used') || 0;
  const initialCpuUsage = fetchMetric('process.cpu.usage') || 0;
  const initialThreads = fetchMetric('jvm.threads.live') || 0;

  const products = [];
  for (let i = 1; i <= 10; i++) {
    const prodRes = http.post(
      `${BASE_URL}/products`,
      JSON.stringify({
        name: `Zapatillas Seed ${i}`,
        description: `Modelo Seed de Benchmark ${i}`,
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    if (prodRes.status === 201) {
      const prodBody = JSON.parse(prodRes.body);
      const productId = prodBody.id;
      products.push(productId);

      const eurIntervals = [
        { initDate: '2024-01-01', endDate: '2024-06-30', value: 99.99 },
        { initDate: '2024-07-01', endDate: '2024-12-31', value: 129.99 },
        { initDate: '2025-01-01', endDate: '2025-06-30', value: 149.99 },
        { initDate: '2025-07-01', endDate: '2025-12-31', value: 179.99 },
        { initDate: '2026-01-01', endDate: '2026-12-31', value: 199.99 },
      ];

      for (const interval of eurIntervals) {
        http.post(
          `${BASE_URL}/products/${productId}/prices`,
          JSON.stringify({
            value: interval.value,
            currency: 'EUR',
            initDate: interval.initDate,
            endDate: interval.endDate,
          }),
          { headers: { 'Content-Type': 'application/json' } }
        );
      }

      const usdIntervals = [
        { initDate: '2024-01-01', endDate: '2024-06-30', value: 109.99 },
        { initDate: '2024-07-01', endDate: '2024-12-31', value: 139.99 },
      ];

      for (const interval of usdIntervals) {
        http.post(
          `${BASE_URL}/products/${productId}/prices`,
          JSON.stringify({
            value: interval.value,
            currency: 'USD',
            initDate: interval.initDate,
            endDate: interval.endDate,
          }),
          { headers: { 'Content-Type': 'application/json' } }
        );
      }
    }
  }

  return {
    products,
    startupDurationMs,
    initialMetrics: {
      memoryMb: (initialMemoryBytes / (1024 * 1024)).toFixed(2),
      cpuUsage: (initialCpuUsage * 100).toFixed(2),
      threads: initialThreads,
    },
  };
}

export function queryActivePrice(data) {
  if (!data.products || data.products.length === 0) return;
  const productId = data.products[Math.floor(Math.random() * data.products.length)];
  const dates = ['2024-04-15', '2024-09-01', '2025-03-20', '2025-11-10', '2026-05-01'];
  const date = dates[Math.floor(Math.random() * dates.length)];
  const currency = Math.random() > 0.3 ? 'EUR' : 'USD';

  const res = http.get(`${BASE_URL}/products/${productId}/prices?date=${date}&currency=${currency}`);
  activePriceLatency.add(res.timings.duration);

  check(res, {
    'active price status is 200': (r) => r.status === 200,
    'active price has value': (r) => r.json('value') !== undefined,
  });
}

export function queryPriceHistory(data) {
  if (!data.products || data.products.length === 0) return;
  const productId = data.products[Math.floor(Math.random() * data.products.length)];
  const sortOrder = Math.random() > 0.5 ? 'DESC' : 'ASC';

  const res = http.get(`${BASE_URL}/products/${productId}/prices?currency=EUR&pageSize=20&sortOrder=${sortOrder}`);
  priceHistoryLatency.add(res.timings.duration);

  check(res, {
    'price history status is 200': (r) => r.status === 200,
    'price history has prices array': (r) => Array.isArray(r.json('prices')),
  });
}

export function createProductAndPrice() {
  const prodRes = http.post(
    `${BASE_URL}/products`,
    JSON.stringify({
      name: `Producto Dyn ${__VU}_${__ITER}`,
      description: `Descripcion dinámica de carga ${__VU}_${__ITER}`,
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  createProductLatency.add(prodRes.timings.duration);

  const passed = check(prodRes, {
    'create product status is 201': (r) => r.status === 201,
  });

  if (passed && prodRes.json('id')) {
    const newProductId = prodRes.json('id');
    const priceRes = http.post(
      `${BASE_URL}/products/${newProductId}/prices`,
      JSON.stringify({
        value: 129.99,
        currency: 'EUR',
        initDate: '2024-01-01',
        endDate: '2024-06-30',
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    addPriceLatency.add(priceRes.timings.duration);

    check(priceRes, {
      'add price status is 201': (r) => r.status === 201,
    });
  }
}

export function teardown(data) {
  const finalMemoryBytes = fetchMetric('jvm.memory.used') || 0;
  const finalCpuUsage = fetchMetric('process.cpu.usage') || 0;
  const systemCpuUsage = fetchMetric('system.cpu.usage') || 0;
  const peakThreads = fetchMetric('jvm.threads.live') || 0;
  const maxMemoryBytes = fetchMetric('jvm.memory.max') || 0;

  return {
    ...data,
    finalMetrics: {
      memoryMb: (finalMemoryBytes / (1024 * 1024)).toFixed(2),
      maxMemoryMb: (maxMemoryBytes / (1024 * 1024)).toFixed(2),
      processCpuPercent: (finalCpuUsage * 100).toFixed(2),
      systemCpuPercent: (systemCpuUsage * 100).toFixed(2),
      peakThreads: peakThreads,
    },
  };
}

export function handleSummary(data) {
  const httpReqs = data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0;
  const httpRate = data.metrics.http_reqs ? data.metrics.http_reqs.values.rate.toFixed(1) : 0;
  const failedRate = data.metrics.http_req_failed ? (data.metrics.http_req_failed.values.rate * 100).toFixed(2) : 0;
  
  const durP50 = data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(50)'].toFixed(2) : 'N/A';
  const durP90 = data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(90)'].toFixed(2) : 'N/A';
  const durP95 = data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'].toFixed(2) : 'N/A';
  const durP99 = data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(99)'].toFixed(2) : 'N/A';

  const startup = data.setup_data ? `${data.setup_data.startupDurationMs} ms` : 'N/A';
  const initMem = data.setup_data && data.setup_data.initialMetrics ? `${data.setup_data.initialMetrics.memoryMb} MB` : 'N/A';
  const finalMem = data.teardown_data && data.teardown_data.finalMetrics ? `${data.teardown_data.finalMetrics.memoryMb} MB` : 'N/A';
  const processCpu = data.teardown_data && data.teardown_data.finalMetrics ? `${data.teardown_data.finalMetrics.processCpuPercent}%` : 'N/A';
  const peakThreads = data.teardown_data && data.teardown_data.finalMetrics ? `${data.teardown_data.finalMetrics.peakThreads}` : 'N/A';

  const report = `
================================================================================
                    PRODUCT API - BENCHMARK & RESOURCE REPORT                    
================================================================================
  [Startup]
    • Cold Startup Duration   : ${startup}

  [Resource Consumption Under Load]
    • Initial Memory (Idle)   : ${initMem}
    • Final Memory (Under Load): ${finalMem} / 1024 MB container limit
    • Process CPU Utilization : ${processCpu} / 100% (1.0 CPU limit)
    • Live / Active Threads   : ${peakThreads} threads

  [Traffic & Latency Performance]
    • Total HTTP Requests     : ${httpReqs} requests
    • Sustained Throughput    : ${httpRate} req/sec
    • Error Rate (Failed Req) : ${failedRate}%
    • Request Latency (p50)   : ${durP50} ms
    • Request Latency (p90)   : ${durP90} ms
    • Request Latency (p95)   : ${durP95} ms
    • Request Latency (p99)   : ${durP99} ms

  [Threshold SLA Status]
    ${data.metrics.http_req_failed && data.metrics.http_req_failed.values.rate < 0.01 ? '✓' : '✗'} Error Rate < 1%         : ${failedRate}%
    ${data.metrics.http_req_duration && data.metrics.http_req_duration.values['p(95)'] < 30 ? '✓' : '✗'} Latency p(95) < 30ms     : ${durP95} ms
    ${data.metrics.http_req_duration && data.metrics.http_req_duration.values['p(99)'] < 60 ? '✓' : '✗'} Latency p(99) < 60ms     : ${durP99} ms
================================================================================
`;

  return {
    stdout: report,
  };
}
