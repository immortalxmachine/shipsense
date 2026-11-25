"""API tests using FastAPI's TestClient and a stubbed model (no training needed)."""
from fastapi.testclient import TestClient

from app import main as app_main
from app.main import app, get_predictor
from app.schemas import PredictResponse

VALID_PAYLOAD = {
    "distance_km": 2175.0, "weight_kg": 2.5, "service_type": "EXPRESS",
    "day_of_week": 2, "month": 11, "is_festival_season": True,
    "is_monsoon": False, "hub_load": 60,
}


class StubPredictor:
    """Stand-in for the real Predictor so tests don't need a trained model."""
    def predict(self, req):
        return PredictResponse(predicted_delay_hours=7.5, on_time_probability=0.42)


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
        assert body["predicted_delay_hours"] == 7.5
        assert body["on_time_probability"] == 0.42
    finally:
        app.dependency_overrides.clear()


def test_predict_validation_error():
    # month=13 is out of range -> 422 from Pydantic validation.
    bad = dict(VALID_PAYLOAD, month=13)
    with TestClient(app) as client:
        resp = client.post("/predict", json=bad)
    assert resp.status_code == 422


def test_predict_returns_503_when_model_missing():
    app.dependency_overrides.clear()
    with TestClient(app) as client:
        app_main._state["predictor"] = None  # force "not loaded" after startup
        resp = client.post("/predict", json=VALID_PAYLOAD)
    assert resp.status_code == 503
