"""API tests using FastAPI's TestClient and a stubbed model (no training needed)."""
from fastapi.testclient import TestClient

from app import main as app_main
from app.main import app, get_predictor
from app.predictor import on_time_probability
from app.schemas import PredictResponse


def test_on_time_probability_monotonic():
    # Predicting well inside the estimate -> high on-time prob; past it -> low.
    early = on_time_probability(predicted_days=8.0, estimated_days=18.0)
    on_estimate = on_time_probability(predicted_days=18.0, estimated_days=18.0)
    late = on_time_probability(predicted_days=28.0, estimated_days=18.0)
    assert early > on_estimate > late
    assert abs(on_estimate - 0.5) < 1e-6
    assert 0.0 <= late < 0.5 < early <= 1.0

VALID_PAYLOAD = {
    "distance_km": 430.0, "weight_kg": 0.9, "freight_value": 15.1,
    "same_state": False, "purchase_dow": 1, "purchase_month": 11,
    "cust_region": "SE", "seller_region": "SE", "estimated_days": 18.0,
}


class StubPredictor:
    """Stand-in for the real Predictor so tests don't need a trained model."""
    def predict(self, req):
        return PredictResponse(predicted_delivery_days=11.5, on_time_probability=0.83)


def test_health_ok():
    with TestClient(app) as client:
        resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"


def test_predict_with_stub_model():
    app.dependency_overrides[get_predictor] = lambda: StubPredictor()
    try:
        with TestClient(app) as client:
            resp = client.post("/predict", json=VALID_PAYLOAD)
        assert resp.status_code == 200
        body = resp.json()
        assert body["predicted_delivery_days"] == 11.5
        assert body["on_time_probability"] == 0.83
    finally:
        app.dependency_overrides.clear()


def test_predict_validation_error():
    # Override the predictor so the dependency doesn't 503 before validation
    # (there may be no trained model in CI). month=13 is out of range -> 422.
    app.dependency_overrides[get_predictor] = lambda: StubPredictor()
    try:
        bad = dict(VALID_PAYLOAD, purchase_month=13)
        with TestClient(app) as client:
            resp = client.post("/predict", json=bad)
        assert resp.status_code == 422
    finally:
        app.dependency_overrides.clear()


def test_predict_rejects_unknown_region():
    app.dependency_overrides[get_predictor] = lambda: StubPredictor()
    try:
        bad = dict(VALID_PAYLOAD, cust_region="ZZ")
        with TestClient(app) as client:
            resp = client.post("/predict", json=bad)
        assert resp.status_code == 422
    finally:
        app.dependency_overrides.clear()


def test_predict_returns_503_when_model_missing():
    app.dependency_overrides.clear()
    with TestClient(app) as client:
        app_main._state["predictor"] = None  # force "not loaded" after startup
        resp = client.post("/predict", json=VALID_PAYLOAD)
    assert resp.status_code == 503
