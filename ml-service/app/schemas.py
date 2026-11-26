"""Pydantic request/response models (validation happens automatically)."""
from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel, Field

Region = Literal["N", "NE", "CO", "SE", "S"]


class PredictRequest(BaseModel):
    distance_km: float = Field(ge=0, description="Great-circle distance between seller and customer state centroids (km)")
    weight_kg: float = Field(ge=0, description="Total parcel weight in kg")
    freight_value: float = Field(ge=0, description="Total freight paid (BRL)")
    same_state: bool = Field(description="True when seller and customer are in the same state")
    purchase_dow: int = Field(ge=0, le=6, description="Purchase weekday, 0=Mon .. 6=Sun")
    purchase_month: int = Field(ge=1, le=12)
    cust_region: Region = Field(description="Customer IBGE macro-region")
    seller_region: Region = Field(description="Seller IBGE macro-region")
    estimated_days: Optional[float] = Field(
        default=None, ge=0,
        description="Carrier's estimated delivery window in days (purchase -> estimated date). "
                    "Used only to derive on_time_probability; not a model feature.",
    )

    model_config = {
        "json_schema_extra": {
            "example": {
                "distance_km": 430.0, "weight_kg": 0.9, "freight_value": 15.1,
                "same_state": False, "purchase_dow": 1, "purchase_month": 11,
                "cust_region": "SE", "seller_region": "SE", "estimated_days": 18.0,
            }
        }
    }


class PredictResponse(BaseModel):
    predicted_delivery_days: float
    on_time_probability: Optional[float] = Field(
        default=None, ge=0, le=1,
        description="P(delivered on or before the estimate); null when estimated_days is omitted.",
    )


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
