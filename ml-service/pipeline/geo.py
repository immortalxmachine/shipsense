"""Brazilian geography reference + helpers (shared by dataset build, training and seed).

Everything here is grounded in the real Olist dataset or in official Brazilian
geography:

* :data:`STATE_NAME` / :data:`STATE_REGION` — the 27 federative units (26 states
  + the Federal District) and their IBGE macro-region.
* :data:`STATE_ADJACENCY` — the real land-border adjacency between those units.
* :func:`state_centroids` — the mean latitude/longitude of each state, computed
  from the real ``olist_geolocation_dataset.csv`` (no coordinates are invented).

The routing graph (hubs = states, edges = bordering states) and the ``distance_km``
feature are both derived from these, so every distance traces back to real data.
"""
from __future__ import annotations

import numpy as np
import pandas as pd

# 27 federative units: UF code -> full name.
STATE_NAME: dict[str, str] = {
    "AC": "Acre", "AL": "Alagoas", "AP": "Amapá", "AM": "Amazonas",
    "BA": "Bahia", "CE": "Ceará", "DF": "Distrito Federal", "ES": "Espírito Santo",
    "GO": "Goiás", "MA": "Maranhão", "MT": "Mato Grosso", "MS": "Mato Grosso do Sul",
    "MG": "Minas Gerais", "PA": "Pará", "PB": "Paraíba", "PR": "Paraná",
    "PE": "Pernambuco", "PI": "Piauí", "RJ": "Rio de Janeiro",
    "RN": "Rio Grande do Norte", "RS": "Rio Grande do Sul", "RO": "Rondônia",
    "RR": "Roraima", "SC": "Santa Catarina", "SP": "São Paulo", "SE": "Sergipe",
    "TO": "Tocantins",
}

# IBGE macro-region: UF -> {N, NE, CO, SE, S}.
STATE_REGION: dict[str, str] = {
    "AC": "N", "AP": "N", "AM": "N", "PA": "N", "RO": "N", "RR": "N", "TO": "N",
    "AL": "NE", "BA": "NE", "CE": "NE", "MA": "NE", "PB": "NE", "PE": "NE",
    "PI": "NE", "RN": "NE", "SE": "NE",
    "DF": "CO", "GO": "CO", "MT": "CO", "MS": "CO",
    "ES": "SE", "MG": "SE", "RJ": "SE", "SP": "SE",
    "PR": "S", "RS": "S", "SC": "S",
}

REGIONS = ["N", "NE", "CO", "SE", "S"]

# Real land-border adjacency between federative units (undirected; each pair
# appears on both sides). This is standard Brazilian political geography.
STATE_ADJACENCY: dict[str, list[str]] = {
    "AC": ["AM", "RO"],
    "AM": ["AC", "RO", "RR", "PA", "MT"],
    "RR": ["AM", "PA"],
    "RO": ["AC", "AM", "MT"],
    "PA": ["AP", "RR", "AM", "MT", "TO", "MA"],
    "AP": ["PA"],
    "MA": ["PA", "TO", "PI"],
    "PI": ["MA", "TO", "BA", "PE", "CE"],
    "CE": ["PI", "RN", "PB", "PE"],
    "RN": ["CE", "PB"],
    "PB": ["RN", "CE", "PE"],
    "PE": ["PB", "CE", "PI", "BA", "AL"],
    "AL": ["PE", "BA", "SE"],
    "SE": ["AL", "BA"],
    "BA": ["SE", "AL", "PE", "PI", "TO", "GO", "MG", "ES"],
    "TO": ["PA", "MA", "PI", "BA", "GO", "MT"],
    "MT": ["RO", "AM", "PA", "TO", "GO", "MS"],
    "GO": ["TO", "BA", "MG", "MS", "MT", "DF"],
    "DF": ["GO", "MG"],
    "MS": ["MT", "GO", "MG", "SP", "PR"],
    "MG": ["BA", "GO", "DF", "MS", "SP", "RJ", "ES"],
    "ES": ["BA", "MG", "RJ"],
    "RJ": ["ES", "MG", "SP"],
    "SP": ["MG", "MS", "PR", "RJ"],
    "PR": ["SP", "MS", "SC"],
    "SC": ["PR", "RS"],
    "RS": ["SC"],
}

EARTH_RADIUS_KM = 6371.0
# Assumed average road speed used to turn a real great-circle distance into an
# estimated drive time for the routing graph's ``avg_travel_hours`` edge weight.
# A transparent modelling assumption applied to real distances (documented in
# the README) — not a value taken from the data.
ASSUMED_ROAD_KMH = 60.0


def haversine_km(lat1, lon1, lat2, lon2):
    """Great-circle distance in km between two (lat, lon) points.

    Works element-wise: pass scalars for one distance or pandas Series/arrays
    to compute a whole column at once.
    """
    lat1, lon1, lat2, lon2 = map(np.radians, [lat1, lon1, lat2, lon2])
    dlat, dlon = lat2 - lat1, lon2 - lon1
    h = np.sin(dlat / 2) ** 2 + np.cos(lat1) * np.cos(lat2) * np.sin(dlon / 2) ** 2
    return 2 * EARTH_RADIUS_KM * np.arcsin(np.sqrt(h))


def state_centroids(geolocation_csv: str) -> pd.DataFrame:
    """Mean (lat, lng) per state from the real geolocation dataset.

    Returns a DataFrame indexed by UF code with ``lat`` and ``lng`` columns.
    A handful of rows in the raw file carry out-of-range coordinates; they are
    clipped to Brazil's bounding box before averaging so a few bad points do
    not drag a centroid into the ocean.
    """
    geo = pd.read_csv(
        geolocation_csv,
        usecols=["geolocation_state", "geolocation_lat", "geolocation_lng"],
    )
    # Brazil roughly spans lat [-34, 6], lng [-74, -34]; drop clearly bad rows.
    geo = geo[
        geo.geolocation_lat.between(-34.0, 6.0)
        & geo.geolocation_lng.between(-74.0, -34.0)
    ]
    cent = geo.groupby("geolocation_state")[["geolocation_lat", "geolocation_lng"]].mean()
    cent.columns = ["lat", "lng"]
    return cent.loc[[s for s in STATE_NAME if s in cent.index]]


def border_edges() -> list[tuple[str, str]]:
    """Unique undirected (a, b) border pairs from :data:`STATE_ADJACENCY`."""
    seen: set[frozenset[str]] = set()
    edges: list[tuple[str, str]] = []
    for a, neighbours in STATE_ADJACENCY.items():
        for b in neighbours:
            key = frozenset((a, b))
            if key not in seen:
                seen.add(key)
                edges.append((a, b))
    return edges
