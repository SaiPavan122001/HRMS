import { useCallback, useEffect, useState } from "react";
import { api, connectNotifications } from "@/lib/api";

export interface HrmsNotification {
  id: string;
  title: string;
  message?: string;
  status: string;
  link?: string;
  createdAt: string;
}

export function useNotifications() {
  const [items, setItems] = useState<HrmsNotification[]>([]);
  const [unread, setUnread] = useState(0);

  const load = useCallback(async () => {
    try {
      const data = await api("/api/v1/notifications");
      setItems(data.items || []);
      setUnread(data.unread || 0);
    } catch {
      // not signed in yet - ignore
    }
  }, []);

  useEffect(() => {
    load();
    const disconnect = connectNotifications((n: HrmsNotification) => {
      setItems((prev) => [n, ...prev].slice(0, 50));
      setUnread((u) => u + 1);
    });
    return disconnect;
  }, [load]);

  const markRead = async (id: string) => {
    await api(`/api/v1/notifications/${id}/read`, { method: "POST" });
    setItems((prev) => prev.map((n) => (n.id === id ? { ...n, status: "READ" } : n)));
    setUnread((u) => Math.max(0, u - 1));
  };

  const markAllRead = async () => {
    await api("/api/v1/notifications/read-all", { method: "POST" });
    setItems((prev) => prev.map((n) => ({ ...n, status: "READ" })));
    setUnread(0);
  };

  return { items, unread, load, markRead, markAllRead };
}
