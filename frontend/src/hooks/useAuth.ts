import { useCallback, useEffect, useState } from "react";
import { api, ApiError, tokens } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";

export interface AuthState {
  user: { id: string; email: string; firstName?: string; lastName?: string } | null;
  session: { user: { id: string; email: string } } | null;
  profile: any | null;
  userRole: string | null;
  loading: boolean;
}

export function useAuth() {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    session: null,
    profile: null,
    userRole: null,
    loading: !!tokens.access,
  });
  const { toast } = useToast();

  const refreshAuthState = useCallback(async () => {
    if (!tokens.access) {
      setAuthState({ user: null, session: null, profile: null, userRole: null, loading: false });
      return;
    }
    try {
      const me = await api("/api/v1/auth/me");
      setAuthState({
        user: me.user,
        session: { user: { id: me.user.id, email: me.user.email } },
        profile: me.profile,
        userRole: me.role,
        loading: false,
      });
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) tokens.clear();
      setAuthState({ user: null, session: null, profile: null, userRole: null, loading: false });
    }
  }, []);

  useEffect(() => {
    refreshAuthState();
  }, [refreshAuthState]);

  const signUp = async (email: string, password: string, firstName: string, lastName: string) => {
    try {
      await api("/api/v1/auth/signup", { method: "POST", body: { email, password, firstName, lastName } });
      toast({ title: "Account created", description: "Check your email to verify your account before signing in." });
      return { error: null };
    } catch (e: any) {
      toast({ title: "Sign Up Error", description: e.message, variant: "destructive" });
      return { error: e };
    }
  };

  const signIn = async (email: string, password: string) => {
    try {
      const res = await api("/api/v1/auth/login", { method: "POST", body: { email, password } });
      tokens.set(res.accessToken, res.refreshToken);
      await refreshAuthState();
      return { error: null };
    } catch (e: any) {
      toast({ title: "Sign In Error", description: e.message, variant: "destructive" });
      return { error: e };
    }
  };

  const signOut = async () => {
    try {
      if (tokens.refresh) {
        await api("/api/v1/auth/logout", { method: "POST", body: { refreshToken: tokens.refresh } });
      }
    } catch { /* ignore */ }
    tokens.clear();
    setAuthState({ user: null, session: null, profile: null, userRole: null, loading: false });
    return { error: null };
  };

  const resetPassword = async (email: string) => {
    try {
      await api("/api/v1/auth/forgot-password", { method: "POST", body: { email } });
      toast({ title: "Reset Link Sent", description: "Check your email for the password reset link." });
      return { error: null };
    } catch (e: any) {
      toast({ title: "Reset Password Error", description: e.message, variant: "destructive" });
      return { error: e };
    }
  };

  const signInWithGoogle = async () => {
    window.location.href = "/oauth2/authorization/google";
    return { error: null };
  };

  const signInWithGitHub = async () => {
    window.location.href = "/oauth2/authorization/github";
    return { error: null };
  };

  return { ...authState, signUp, signIn, signOut, resetPassword, signInWithGoogle, signInWithGitHub };
}
