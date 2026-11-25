import { useEffect, useMemo, useState } from "react";
import { api, type Hub, type RoutePlan } from "../api/client";
import { Card, ErrorBanner, Spinner } from "../components/ui";

export default function RoutePlannerPage() {
  const [hubs, setHubs] = useState<Hub[]>([]);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [plan, setPlan] = useState<RoutePlan | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load hubs once to populate the dropdowns.
  useEffect(() => {
    api
      .getHubs()
      .then((h) => {
        setHubs(h);
        if (h.length >= 2) {
          setFrom(h[0].code);
          setTo(h[1].code);
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load hubs"));
  }, []);

  const cityByCode = useMemo(
    () => Object.fromEntries(hubs.map((h) => [h.code, h.city])),
    [hubs],
  );

  async function handlePlan(e: React.FormEvent) {
    e.preventDefault();
    if (!from || !to) return;
    setLoading(true);
    setError(null);
    setPlan(null);
    try {
      setPlan(await api.getOptimalRoute(from, to));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not compute a route");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Route planner</h1>
        <p className="text-sm text-slate-500">
          Shortest path between two hubs, computed by Dijkstra's algorithm on the road network.
        </p>
      </div>

      <form onSubmit={handlePlan} className="flex flex-wrap items-end gap-3">
        <label className="text-sm">
          <span className="mb-1 block text-slate-500">From</span>
          <select
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-brand focus:outline-none"
          >
            {hubs.map((h) => (
              <option key={h.code} value={h.code}>
                {h.code} — {h.city}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm">
          <span className="mb-1 block text-slate-500">To</span>
          <select
            value={to}
            onChange={(e) => setTo(e.target.value)}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-brand focus:outline-none"
          >
            {hubs.map((h) => (
              <option key={h.code} value={h.code}>
                {h.code} — {h.city}
              </option>
            ))}
          </select>
        </label>
        <button
          type="submit"
          className="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-dark"
        >
          Find route
        </button>
      </form>

      {loading && <Spinner label="Computing shortest path…" />}
      {error && <ErrorBanner message={error} />}

      {plan && (
        <div className="grid gap-6 md:grid-cols-3">
          <div className="md:col-span-2">
            <Card title="Optimal path">
              <div className="flex flex-wrap items-center gap-2">
                {plan.path.map((code, i) => (
                  <span key={`${code}-${i}`} className="flex items-center gap-2">
                    <span className="rounded-md bg-slate-100 px-2.5 py-1 text-sm font-medium">
                      {code}
                      <span className="ml-1 text-xs text-slate-400">{cityByCode[code] ?? ""}</span>
                    </span>
                    {i < plan.path.length - 1 && <span className="text-accent">→</span>}
                  </span>
                ))}
              </div>
            </Card>
          </div>
          <Card title="Summary">
            <div className="space-y-3">
              <div>
                <div className="text-2xl font-bold text-slate-800">
                  {plan.totalDistanceKm.toFixed(0)}
                  <span className="ml-1 text-sm font-normal text-slate-500">km</span>
                </div>
                <p className="text-xs text-slate-500">total road distance</p>
              </div>
              <div>
                <div className="text-2xl font-bold text-slate-800">
                  {plan.estimatedHours.toFixed(1)}
                  <span className="ml-1 text-sm font-normal text-slate-500">hours</span>
                </div>
                <p className="text-xs text-slate-500">estimated travel time</p>
              </div>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
