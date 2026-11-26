import { useState } from "react";
import { api, type Shipment, type TrackingEvent } from "../api/client";
import { Card, ErrorBanner, Spinner, StatusBadge } from "../components/ui";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString();
}

export default function TrackPage() {
  const [trackingNo, setTrackingNo] = useState("SS0000000001");
  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [events, setEvents] = useState<TrackingEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    const tn = trackingNo.trim();
    if (!tn) return;
    setLoading(true);
    setError(null);
    setShipment(null);
    setEvents([]);
    try {
      const [s, ev] = await Promise.all([api.trackShipment(tn), api.getEvents(tn)]);
      setShipment(s);
      setEvents(ev);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Track a parcel</h1>
        <p className="text-sm text-slate-500">
          Enter a tracking number to see its status, scan history and predicted delivery time.
        </p>
      </div>

      <form onSubmit={handleSearch} className="flex gap-2">
        <input
          value={trackingNo}
          onChange={(e) => setTrackingNo(e.target.value)}
          placeholder="e.g. SS0000000001"
          className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
        />
        <button
          type="submit"
          className="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-dark"
        >
          Track
        </button>
      </form>

      {loading && <Spinner label="Fetching shipment…" />}
      {error && <ErrorBanner message={error} />}

      {shipment && (
        <div className="grid gap-6 md:grid-cols-2">
          <Card title="Shipment">
            <div className="space-y-2 text-sm">
              <div className="flex items-center justify-between">
                <span className="font-mono text-base font-semibold">{shipment.trackingNo}</span>
                <StatusBadge status={shipment.status} />
              </div>
              <div className="text-lg font-semibold text-slate-800">
                {shipment.originCity} <span className="text-slate-400">→</span> {shipment.destCity}
              </div>
              <dl className="grid grid-cols-2 gap-x-4 gap-y-1 pt-2 text-slate-600">
                <dt>Weight</dt><dd className="text-right font-medium">{shipment.weightKg} kg</dd>
                <dt>Freight</dt><dd className="text-right font-medium">R$ {shipment.freightValue.toFixed(2)}</dd>
                <dt>Purchased</dt><dd className="text-right">{formatDate(shipment.createdAt)}</dd>
                <dt>Estimated</dt><dd className="text-right">{formatDate(shipment.promisedAt)}</dd>
                <dt>Delivered</dt><dd className="text-right">{formatDate(shipment.deliveredAt)}</dd>
              </dl>
            </div>
          </Card>

          <Card title="Predicted delivery time (ML)">
            {shipment.predictedDeliveryDays === null ? (
              <p className="text-sm text-slate-500">
                ML service unavailable — showing tracking only.
              </p>
            ) : (
              <div className="space-y-3">
                <div>
                  <div className="text-3xl font-bold text-brand">
                    {shipment.predictedDeliveryDays.toFixed(1)}
                    <span className="ml-1 text-base font-normal text-slate-500">days</span>
                  </div>
                  <p className="text-xs text-slate-500">predicted purchase-to-delivery time</p>
                </div>
                {shipment.onTimeProbability !== null && (
                  <div>
                    <div className="mb-1 flex justify-between text-xs text-slate-500">
                      <span>On-time probability</span>
                      <span>{(shipment.onTimeProbability * 100).toFixed(0)}%</span>
                    </div>
                    <div className="h-2 w-full rounded-full bg-slate-200">
                      <div
                        className="h-2 rounded-full bg-accent"
                        style={{ width: `${Math.round(shipment.onTimeProbability * 100)}%` }}
                      />
                    </div>
                  </div>
                )}
              </div>
            )}
          </Card>

          <div className="md:col-span-2">
            <Card title="Scan history">
              {events.length === 0 ? (
                <p className="text-sm text-slate-500">No tracking events recorded yet.</p>
              ) : (
                <ol className="relative space-y-4 border-l border-slate-200 pl-6">
                  {events.map((ev) => (
                    <li key={ev.id} className="relative">
                      <span className="absolute -left-[27px] top-1 h-3 w-3 rounded-full bg-brand" />
                      <div className="text-sm font-medium text-slate-800">
                        {ev.eventType.replace(/_/g, " ")}
                      </div>
                      <div className="text-xs text-slate-500">
                        {ev.hubCity} · {formatDate(ev.eventTime)}
                      </div>
                    </li>
                  ))}
                </ol>
              )}
            </Card>
          </div>
        </div>
      )}
    </div>
  );
}
