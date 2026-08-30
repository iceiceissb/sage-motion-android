from __future__ import annotations

import asyncio
import hashlib
import hmac
import time
from collections import defaultdict, deque

from fastapi import HTTPException, Request, status

from .config import Settings


class SlidingWindowRateLimiter:
    def __init__(self, requests_per_minute: int) -> None:
        self.limit = requests_per_minute
        self._requests: dict[str, deque[float]] = defaultdict(deque)
        self._lock = asyncio.Lock()

    async def check(self, subject: str) -> None:
        now = time.monotonic()
        async with self._lock:
            timestamps = self._requests[subject]
            while timestamps and now - timestamps[0] >= 60:
                timestamps.popleft()
            if len(timestamps) >= self.limit:
                retry_after = max(1, int(60 - (now - timestamps[0])))
                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail="rate limit exceeded",
                    headers={"Retry-After": str(retry_after)},
                )
            timestamps.append(now)


def authenticate_request(request: Request, settings: Settings) -> str:
    expected = settings.backend_auth_token.get_secret_value()
    if expected:
        authorization = request.headers.get("Authorization", "")
        scheme, _, token = authorization.partition(" ")
        if scheme.lower() != "bearer" or not hmac.compare_digest(token, expected):
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid bearer token")
        return "token:" + hashlib.sha256(token.encode()).hexdigest()[:16]
    if settings.environment == "production":
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="authentication unavailable",
        )
    host = request.client.host if request.client else "unknown"
    return f"dev:{host}"
