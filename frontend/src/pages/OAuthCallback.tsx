import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

/**
 * OAuth callback landing: the backend redirects here with the access token in the URL
 * fragment (#access_token=...), which browsers keep out of server logs and referrers.
 */
const OAuthCallback = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const fragment = new URLSearchParams(window.location.hash.substring(1));
    const accessToken = fragment.get("access_token");
    if (accessToken) {
      localStorage.setItem("hrms_access_token", accessToken);
      // The refresh token arrives via the /auth/me flow; fetch it once authenticated.
      navigate("/dashboard", { replace: true });
    } else {
      navigate("/login?error=oauth_failed", { replace: true });
    }
  }, [navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <p className="text-muted-foreground">Completing sign in...</p>
    </div>
  );
};

export default OAuthCallback;
