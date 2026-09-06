import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useNavigate } from "react-router-dom";
import { api } from "@/lib/api";

const VerifyEmail = () => {
  const [state, setState] = useState<"verifying" | "success" | "error">("verifying");
  const [message, setMessage] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    const token = new URLSearchParams(window.location.search).get("token");
    if (!token) {
      setState("error");
      setMessage("Verification link is missing or invalid.");
      return;
    }
    api("/api/v1/auth/verify-email?token=" + encodeURIComponent(token), { method: "POST" })
      .then((res: any) => {
        setState("success");
        setMessage(res.message || "Email verified.");
      })
      .catch((e) => {
        setState("error");
        setMessage(e.message || "Verification failed.");
      });
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-bold">Email Verification</CardTitle>
          <CardDescription>
            {state === "verifying" && "Verifying your email address..."}
            {state === "success" && message}
            {state === "error" && message}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Button className="w-full" onClick={() => navigate("/login")} disabled={state === "verifying"}>
            Go to Sign In
          </Button>
        </CardContent>
      </Card>
    </div>
  );
};

export default VerifyEmail;
