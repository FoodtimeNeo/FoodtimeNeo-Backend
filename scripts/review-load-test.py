"""Exercise only explicit loopback review fixtures. Password comes from FOODTIME_LOAD_PASSWORD."""
import argparse
import concurrent.futures
import datetime
import http.cookiejar
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from collections import Counter, defaultdict
from pathlib import Path


class Client:
    def __init__(self, base, email, password):
        self.base = base.rstrip('/') + '/'
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.csrf = self.call('GET', 'auth/csrf')[1]['data']['token']
        status, _ = self.call('POST', 'auth/login', {'account': email, 'password': password})
        if status != 200:
            raise RuntimeError('Fixture login failed with status ' + str(status))
        self.csrf = self.call('GET', 'auth/csrf')[1]['data']['token']

    def call(self, method, path, payload=None):
        headers = {'Accept': 'application/json'}
        if method != 'GET':
            headers['X-CSRF-TOKEN'] = self.csrf
        body = None
        if payload is not None:
            body = json.dumps(payload).encode()
            headers['Content-Type'] = 'application/json'
        request = urllib.request.Request(self.base + path, data=body, method=method, headers=headers)
        try:
            with self.opener.open(request, timeout=8) as response:
                return response.status, json.load(response)
        except urllib.error.HTTPError as error:
            return error.code, json.load(error)


def percentile(values, p):
    ordered = sorted(values)
    return round(ordered[min(len(ordered) - 1, int((len(ordered) - 1) * p))], 2) if ordered else None


def phase(name, clients, duration, fixtures):
    dish, root, reply = fixtures['dish'], fixtures['review'], fixtures['reply']
    reads = [('rating', f'dishes/{dish}/rating'), ('comments', f'dishes/{dish}/reviews?page=1&size=20'),
             ('replies', f'reviews/{root}/replies?size=20')]
    started = time.perf_counter()
    deadline = started + duration

    def run(index, client):
        samples = []
        states = {'review-like': False, 'reply-like': False}
        versions = defaultdict(int)
        iteration = 0
        while time.perf_counter() < deadline:
            writing = name == 'hot-writes' or (name == 'mixed' and iteration % 5 >= 3)
            if writing:
                key = 'review-like' if iteration % 2 == 0 else 'reply-like'
                path = f'reviews/{root}/like' if key == 'review-like' else f'review-replies/{reply}/like'
                desired = not states[key]
                method = 'PUT' if desired else 'DELETE'
            else:
                key, path = reads[(iteration + index) % len(reads)]
                method = 'GET'
            before = time.perf_counter()
            error = ''
            try:
                status, response = client.call(method, path)
                if status == 200:
                    data = response['data']
                    if writing:
                        if data['likedByMe'] != desired or data['likeCount'] < 0 or data['counterVersion'] < versions[key]:
                            raise ValueError('Invalid or regressing counter response')
                        states[key], versions[key] = desired, data['counterVersion']
                    elif key == 'rating' and data['reviewCount'] != fixtures['review_count']:
                        raise ValueError('Rating count drift')
                    elif key == 'comments' and (data['total'] != fixtures['review_count'] or len(data['items']) > 20):
                        raise ValueError('Public comment count/page drift')
                    elif key == 'replies' and (data['total'] != fixtures['reply_count'] or len(data['items']) > 20):
                        raise ValueError('Public reply count/page drift')
                else:
                    error = 'HTTP ' + str(status)
            except Exception as failure:
                status, error = 0, type(failure).__name__ + ': ' + str(failure)
            samples.append((key, status, (time.perf_counter() - before) * 1000, error))
            iteration += 1
        return samples

    with concurrent.futures.ThreadPoolExecutor(max_workers=len(clients)) as pool:
        batches = list(pool.map(lambda pair: run(*pair), enumerate(clients)))
    elapsed = time.perf_counter() - started
    samples = [sample for batch in batches for sample in batch]
    endpoints = {}
    for key in sorted({sample[0] for sample in samples}):
        rows = [sample for sample in samples if sample[0] == key]
        latency = [row[2] for row in rows]
        endpoints[key] = {'requests': len(rows), 'statuses': dict(Counter(str(row[1]) for row in rows)),
                          'p50_ms': percentile(latency, .50), 'p95_ms': percentile(latency, .95),
                          'p99_ms': percentile(latency, .99)}
    errors = [sample[3] for sample in samples if sample[3]]
    return {'name': name, 'concurrency': len(clients), 'seconds': round(elapsed, 2), 'requests': len(samples),
            'rps': round(len(samples) / elapsed, 2), 'p95_ms': percentile([sample[2] for sample in samples], .95),
            'errors': len(errors), 'error_examples': list(dict.fromkeys(errors))[:5], 'endpoints': endpoints}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', required=True)
    parser.add_argument('--fixtures', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--seconds', type=int, default=20)
    parser.add_argument('--concurrency', type=int, default=32)
    parser.add_argument('--confirm-isolated-writes', action='store_true')
    args = parser.parse_args()
    address = urllib.parse.urlparse(args.base_url)
    if address.hostname not in {'127.0.0.1', 'localhost', '::1'} or not args.confirm_isolated_writes:
        parser.error('Only explicit isolated loopback writes are supported')
    fixtures = json.loads(args.fixtures.read_text(encoding='utf-8'))
    for key in ['dish', 'review', 'reply']:
        uuid.UUID(fixtures[key])
    count = args.concurrency
    if count < 1 or count > len(fixtures['emails']) or not 1 <= args.seconds <= 120:
        parser.error('Invalid concurrency or duration')
    password = os.environ['FOODTIME_LOAD_PASSWORD']
    clients = [Client(args.base_url, email, password) for email in fixtures['emails'][:count]]
    report = {'started_at': datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat(),
              'scope': 'Isolated loopback; one hot dish/review/reply; no production data',
              'review_count': fixtures['review_count'], 'reply_count': fixtures['reply_count'], 'phases': []}
    try:
        for name in ['reads', 'hot-writes', 'mixed']:
            result = phase(name, clients, args.seconds, fixtures)
            report['phases'].append(result)
            print(json.dumps(result), flush=True)
    finally:
        for client in clients:
            for path in [f"reviews/{fixtures['review']}/like", f"review-replies/{fixtures['reply']}/like"]:
                status, _ = client.call('DELETE', path)
                if status != 200:
                    raise RuntimeError('Could not clear a fixture like: ' + str(status))
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    if any(row['errors'] or row['p95_ms'] >= 2000 for row in report['phases']):
        raise SystemExit('Review load acceptance failed: errors or p95 >= 2 seconds')


if __name__ == '__main__':
    main()
