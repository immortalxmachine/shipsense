import { useEffect, useMemo, useState } from "react";
import {
  Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer,
  Tooltip, XAxis, YAxis,
} from "recharts";
import { api, type Shipment } from "../api/client";
import { Card, ErrorBanner, Spinner } from "../components/ui";

const BRAND = "#4D148C";
const ACCENT = "#FF6600";

function hoursBetween(a: string, b: string): number {
  return (new Date(a).getTime() - new Date(b).getTime()) / 3_600_000;
}

export default function AnalyticsPage() {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .listShipments()
      .then(setShipments)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load analytics"))
      .finally(() => setLoading(false));
  }, []);

  const delivered = useMemo(
    () => shipments.filter((s) => s.status === "DELIVERED" && s.deliveredAt),
    [shipments],
  );

  const onTimeByHub = useMemo(() => {
    const map = new Map<string, { total: number; onTime: number }>();
    for (const s of delivered) {
      const entry = map.get(s.originCode) ?? { total: 0, onTime: 0 };
      entry.total += 1;
      if (hoursBetween(s.deliveredAt!, s.promisedAt) <= 0) entry.onTime += 1;
      map.set(s.originCode, entry);
    }
    return [...map.entries()].map(([hub, v]) => ({
      hub,
      onTimePct: Math.round((100 * v.onTime) / v.total),
    }));
  }, [delivered]);

  // Average purchase-to-delivery time (days) per destination state.
  const deliveryTimeByDest = useMemo(() => {
    const map = new Map<string, { sum: number; n: number }>();
    for (const s of delivered) {
      const entry = map.get(s.destCode) ?? { sum: 0, n: 0 };
      entry.sum += hoursBetween(s.deliveredAt!, s.createdAt) / 24;
      entry.n += 1;
      map.set(s.destCode, entry);
    }
    return [...map.entries()]
      .map(([dest, v]) => ({ dest, avgDays: Number((v.sum / v.n).toFixed(2)) }))
      .sort((a, b) => b.avgDays - a.avgDays);
  }, [delivered]);

  const volumeOverTime = useMemo(() => {
    const map = new Map<string, number>();
    for (const s of shipments) {
      const day = s.createdAt.slice(0, 10);
      map.set(day, (map.get(day) ?? 0) + 1);
    }
    return [...map.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, count]) => ({ date, count }));
  }, [shipments]);

  if (loading) return <Spinner label="Loading analytics…" />;
  if (error) return <ErrorBanner message={error} />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Analytics</h1>
        <p className="text-sm text-slate-500">
          Aggregated from {shipments.length} shipments ({delivered.length} delivered).
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card title="On-time % by origin hub">
          {onTimeByHub.length === 0 ? (
            <Empty />
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={onTimeByHub}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="hub" fontSize={12} />
                <YAxis domain={[0, 100]} fontSize={12} unit="%" />
                <Tooltip />
                <Bar dataKey="onTimePct" fill={BRAND} radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </Card>

        <Card title="Avg delivery time by destination state (days)">
          {deliveryTimeByDest.length === 0 ? (
            <Empty />
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={deliveryTimeByDest}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="dest" fontSize={12} />
                <YAxis fontSize={12} />
                <Tooltip />
                <Bar dataKey="avgDays" fill={ACCENT} radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </Card>

        <div className="md:col-span-2">
          <Card title="Shipment volume over time">
            {volumeOverTime.length === 0 ? (
              <Empty />
            ) : (
              <ResponsiveContainer width="100%" height={240}>
                <LineChart data={volumeOverTime}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="date" fontSize={12} />
                  <YAxis allowDecimals={false} fontSize={12} />
                  <Tooltip />
                  <Line type="monotone" dataKey="count" stroke={BRAND} strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}

function Empty() {
  return <p className="py-8 text-center text-sm text-slate-400">Not enough data yet.</p>;
}
