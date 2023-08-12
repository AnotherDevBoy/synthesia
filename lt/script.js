import http from "k6/http";

import { check } from "k6";

export const options = {
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<2000"],
  },
};

export default function () {
  const result = http.get(
    "http://localhost:7070/crypto/sign?message=test&webhookUrl=http%3A%2F%2F127.0.0.1%3A7070%2Fwebhook"
  );

  check(result, {
    "is successful status code": (r) => r.status === 200 || r.status === 202,
  });
}
