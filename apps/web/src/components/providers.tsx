"use client";
import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
} from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { api, refresh, post, setToken } from "@/lib/api";
import { messages, Locale } from "@/lib/i18n";
type User = { id: string; name: string; email: string; system_role: string };
const Context = createContext<{
  user: User | null;
  loading: boolean;
  signIn: (data: { accessToken: string; user: User }) => void;
  logout: () => Promise<void>;
  locale: Locale;
  setLocale: (l: Locale) => void;
  t: typeof messages.en;
}>({
  user: null,
  loading: true,
  signIn: () => {},
  logout: async () => {},
  locale: "en",
  setLocale: () => {},
  t: messages.en,
});
export function Providers({ children }: { children: React.ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 10000 },
        },
      }),
  );
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [locale, setLocaleState] = useState<Locale>("en");
  useEffect(() => {
    const saved = localStorage.getItem("onenotify-locale");
    if (saved === "hi") setLocaleState("hi");
    refresh()
      .then((ok) => (ok ? api<User>("/auth/me") : null))
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);
  const signIn = useCallback(
    (data: { accessToken: string; user: User }) => {
      setToken(data.accessToken);
      setUser(data.user);
      client.clear();
    },
    [client],
  );
  const logout = async () => {
    await post("/auth/logout");
    setToken("");
    setUser(null);
    client.clear();
  };
  const setLocale = (l: Locale) => {
    setLocaleState(l);
    localStorage.setItem("onenotify-locale", l);
    document.documentElement.lang = l;
  };
  return (
    <QueryClientProvider client={client}>
      <Context.Provider
        value={{
          user,
          loading,
          signIn,
          logout,
          locale,
          setLocale,
          t: messages[locale],
        }}
      >
        {children}
      </Context.Provider>
    </QueryClientProvider>
  );
}
export const useSession = () => useContext(Context);
