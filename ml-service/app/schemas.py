"""Pydantic request/response models (validation happens automatically)."""
from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class PredictRequest(BaseModel):
    distance_km: float = Field(ge=0, description="Road distance in km")
    weight_kg: float = Field(gt=0, description="Parcel weight in kg")
    service_type: Literal["STANDARD", "EXPRESS", "PRIORITY"]
    day_of_week: int = Field(ge=0, le=6, description="0=Mon .. 6=Sun")
    month: int = Field(ge=1, le=12)
    is_festival_season: bool
    is_monsoon: bool
    hub_load: int = Field(ge=0, description="Congestion proxy at the origin hub")

    model_config = {
        "json_schema_extra": {
            "example": {
                "distance_km": 2175.0, "weight_kg": 2.5, "service_type": "EXPRESS",
                "day_of_week": 2, "month": 11, "is_festival_season": True,
                "is_monsoon": False, "hub_load": 60,
            }
        }
    }


class PredictResponse(BaseModel):
    predicted_delay_hours: float
    on_time_probability: float = Field(ge=0, le=1)


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
