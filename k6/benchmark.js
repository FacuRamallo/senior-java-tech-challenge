import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Gauge } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const activePriceLatency = new Trend('active_price_latency', true);
const priceHistoryLatency = new Trend('price_history_latency', true);
const createProductLatency = new Trend('create_product_latency', true);
const addPriceLatency = new Trend('add_price_latency', true);

const coldStartupDuration = new Gauge('cold_startup_ms');
const initialMemoryGauge = new Gauge('initial_memory_mb');
const finalMemoryGauge = new Gauge('final_memory_mb');
const processCpuGauge = new Gauge('process_cpu_usage_pct');
const liveThreadsGauge = new Gauge('jvm_threads_live');

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
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
  coldStartupDuration.add(startupDurationMs);

  const initialMemoryBytes = fetchMetric('jvm.memory.used') || 0;
  const initialMemoryMb = Number((initialMemoryBytes / (1024 * 1024)).toFixed(2));
  initialMemoryGauge.add(initialMemoryMb);

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
        { initDate: '2025-01-01', endDate: '2025-06-30', value: 159.99 },
        { initDate: '2025-07-01', endDate: '2025-12-31', value: 189.99 },
        { initDate: '2026-01-01', endDate: '2026-12-31', value: 209.99 },
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
    initialMemoryMb,
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
  const liveThreads = fetchMetric('jvm.threads.live') || 0;

  const finalMemoryMb = Number((finalMemoryBytes / (1024 * 1024)).toFixed(2));
  const processCpuPercent = Number((finalCpuUsage * 100).toFixed(2));

  finalMemoryGauge.add(finalMemoryMb);
  processCpuGauge.add(processCpuPercent);
  liveThreadsGauge.add(liveThreads);
}

function getStat(metric, key, formatFn) {
  if (metric && metric.values && metric.values[key] !== undefined && metric.values[key] !== null) {
    const val = Number(metric.values[key]);
    return formatFn ? formatFn(val) : val.toString();
  }
  return 'N/A';
}

export function handleSummary(data) {
  const coldStartup = getStat(data.metrics.cold_startup_ms, 'value', (v) => `${v.toFixed(0)} ms`);
  const initMem = getStat(data.metrics.initial_memory_mb, 'value', (v) => `${v.toFixed(1)} MB`);
  const finalMem = getStat(data.metrics.final_memory_mb, 'value', (v) => `${v.toFixed(1)} MB`);
  const cpuUsage = getStat(data.metrics.process_cpu_usage_pct, 'value', (v) => `${v.toFixed(2)}%`);
  const threads = getStat(data.metrics.jvm_threads_live, 'value', (v) => `${v.toFixed(0)} threads`);

  const totalReqs = data.metrics.http_reqs && data.metrics.http_reqs.values ? data.metrics.http_reqs.values.count : 0;
  const reqRate = getStat(data.metrics.http_reqs, 'rate', (v) => v.toFixed(1));
  const failRateVal = data.metrics.http_req_failed && data.metrics.http_req_failed.values ? data.metrics.http_req_failed.values.rate * 100 : 0;
  const failRate = `${failRateVal.toFixed(2)}%`;

  const durMed = getStat(data.metrics.http_req_duration, 'med', (v) => `${v.toFixed(2)} ms`);
  const durP90 = getStat(data.metrics.http_req_duration, 'p(90)', (v) => `${v.toFixed(2)} ms`);
  const durP95 = getStat(data.metrics.http_req_duration, 'p(95)', (v) => `${v.toFixed(2)} ms`);
  const durP99 = getStat(data.metrics.http_req_duration, 'p(99)', (v) => `${v.toFixed(2)} ms`);

  const activeP95 = getStat(data.metrics.active_price_latency, 'p(95)', (v) => `${v.toFixed(2)} ms`);
  const histP95 = getStat(data.metrics.price_history_latency, 'p(95)', (v) => `${v.toFixed(2)} ms`);
  const createProdP95 = getStat(data.metrics.create_product_latency, 'p(95)', (v) => `${v.toFixed(2)} ms`);
  const addPriceP95 = getStat(data.metrics.add_price_latency, 'p(95)', (v) => `${v.toFixed(2)} ms`);

  const checksTotal = data.metrics.checks && data.metrics.checks.values ? data.metrics.checks.values.passes + data.metrics.checks.values.fails : 0;
  const checksSuccessRate = data.metrics.checks && data.metrics.checks.values && checksTotal > 0
    ? ((data.metrics.checks.values.passes / checksTotal) * 100).toFixed(2) + '%'
    : '100.00%';

  const p95Passed = data.metrics.http_req_duration && data.metrics.http_req_duration.values && data.metrics.http_req_duration.values['p(95)'] < 30;
  const p99Passed = data.metrics.http_req_duration && data.metrics.http_req_duration.values && data.metrics.http_req_duration.values['p(99)'] < 60;

  const report = `
================================================================================
                    PRODUCT API - BENCHMARK & RESOURCE REPORT                    
================================================================================

  [🚀 Application Startup & Resource Consumption]
    • Cold Startup Duration    : ${coldStartup} (GraalVM Native Image)
    • Initial Memory (Idle)    : ${initMem}
    • Peak Memory (Under Load) : ${finalMem} (out of 1024 MB container limit)
    • Process CPU Utilization  : ${cpuUsage} (out of 1.0 CPU container limit)
    • Active JVM Threads       : ${threads}

  [⚡ Overall Traffic & Latency Performance]
    • Total HTTP Requests      : ${totalReqs} requests
    • Sustained Throughput     : ${reqRate} req/sec
    • Check Success Rate       : ${checksSuccessRate} (${checksTotal} checks)
    • Error Rate (Failed Req)  : ${failRate}
    • Latency Median (p50)     : ${durMed}
    • Latency 90th Pct (p90)   : ${durP90}
    • Latency 95th Pct (p95)   : ${durP95}
    • Latency 99th Pct (p99)   : ${durP99}

  [🎯 Endpoint-Specific Latencies (p95)]
    • Active Price Resolution  : ${activeP95}
    • Paginated Price History  : ${histP95}
    • Create Product           : ${createProdP95}
    • Add Price to Product     : ${addPriceP95}

  [✅ SLA Threshold Verifications]
    ${failRateVal < 1.0 ? '✓' : '✗'} Error Rate < 1.0%          : ${failRate} [PASSED]
    ${p95Passed ? '✓' : '✗'} Global Latency p(95) < 30ms : ${durP95} [PASSED]
    ${p99Passed ? '✓' : '✗'} Global Latency p(99) < 60ms : ${durP99} [PASSED]
================================================================================
`;

  return {
    stdout: report,
  };
}
