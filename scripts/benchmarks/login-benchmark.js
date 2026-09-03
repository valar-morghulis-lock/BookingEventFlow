import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        brute_force: {
            executor: 'constant-arrival-rate',
            rate: 50,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 20,
            maxVUs: 100,
        },
    },
};

export default function () {
    const payload = JSON.stringify({
        email: 'ahmed@instance.com',
        password: 'wrong-password',
    });

    const res = http.post('http://localhost:8084/api/v1/auth/login', payload, {
        headers: { 'Content-Type': 'application/json' },
    });

    check(res, {
        'is 401 or 429': (r) => r.status === 401 || r.status === 429,
    });

    sleep(0.01);
}