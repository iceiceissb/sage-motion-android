import json
import logging

from sage_backend.observability import JsonLogFormatter


def test_json_logs_include_correlation_but_not_arbitrary_extras() -> None:
    record = logging.LogRecord("sage", logging.INFO, __file__, 1, "done", (), None)
    record.sage_request_id = "request006"
    record.openai_request_id = "req_safe"
    record.secret = "must-not-be-serialized"

    payload = json.loads(JsonLogFormatter().format(record))

    assert payload["sage_request_id"] == "request006"
    assert payload["openai_request_id"] == "req_safe"
    assert "secret" not in payload
