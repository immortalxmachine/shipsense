// Typed client for the ShipSense Java backend. Base URL comes from env config.
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export type ServiceType = "STANDARD" | "EXPRESS" | "PRIORITY";

export interface Hub {
  id: number;
  code: string;
  name: string;
  city: string;
  lat: number;
  lng: number;
}

export interface Shipment {
  id: number;
  trackingNo: string;
  originCode: string;
  originCity: string;
  destCode: string;
  destCity: string;
  weightKg: number;
  serviceType: ServiceType;
  status: string;
  createdAt: string;
  promisedAt: string;
  deliveredAt: string | null;
  predictedDelayHours: number | null;
  onTimeProbability: number | null;
}

export interface TrackingEvent {
  id: number;
  hubCode: string;
  hubCity: string;
  eventType: string;
  eventTime: string;
}

export interface RoutePlan {
  from: string;
  to: string;
  path: string[];
  totalDistanceKm: number;
  estimatedHours: number;
}

/** Fetch wrapper that surfaces the backend's JSON error message. */
async function apiFetch<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`);
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      /* non-JSON error body; keep the default message */
    }
    throw new Error(message);
  }
  return res.json() as Promise<T>;
}

export const api = {
  getHubs: () => apiFetch<Hub[]>("/api/hubs"),
  listShipments: () => apiFetch<Shipment[]>("/api/shipments"),
  trackShipment: (trackingNo: string) =>
    apiFetch<Shipment>(`/api/shipments/${encodeURIComponent(trackingNo)}`),
  getEvents: (trackingNo: string) =>
    apiFetch<TrackingEvent[]>(`/api/shipments/${encodeURIComponent(trackingNo)}/events`),
  getOptimalRoute: (from: string, to: string) =>
    apiFetch<RoutePlan>(`/api/routes/optimal?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`),
};

export { BASE_URL };
