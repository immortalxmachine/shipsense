import { NavLink, Navigate, Route, Routes } from "react-router-dom";
import TrackPage from "./pages/TrackPage";
import RoutePlannerPage from "./pages/RoutePlannerPage";
import AnalyticsPage from "./pages/AnalyticsPage";

const navItems = [
  { to: "/track", label: "Track a parcel" },
  { to: "/route", label: "Route planner" },
  { to: "/analytics", label: "Analytics" },
];

export default function App() {
  return (
    <div className="min-h-screen">
      <header className="bg-brand text-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <div className="flex items-baseline gap-2">
            <span className="text-xl font-bold">ShipSense</span>
            <span className="text-xs text-purple-200">Parcel ETA & Route Intelligence</span>
          </div>
          <nav className="flex gap-1">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `rounded-md px-3 py-1.5 text-sm font-medium transition ${
                    isActive ? "bg-white/20" : "hover:bg-white/10"
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-8">
        <Routes>
          <Route path="/" element={<Navigate to="/track" replace />} />
          <Route path="/track" element={<TrackPage />} />
          <Route path="/route" element={<RoutePlannerPage />} />
          <Route path="/analytics" element={<AnalyticsPage />} />
          <Route path="*" element={<Navigate to="/track" replace />} />
        </Routes>
      </main>

      <footer className="mx-auto max-w-5xl px-6 py-6 text-center text-xs text-slate-400">
        ShipSense · real Olist order sample · portfolio project
      </footer>
    </div>
  );
}
