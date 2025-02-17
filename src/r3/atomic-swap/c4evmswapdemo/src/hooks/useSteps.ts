import { useState } from "react";

export function useSteps() {
  const [activeStep, setActiveStep] = useState(0);
  const [stepResult, setStepResult] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  return {
    activeStep,
    setActiveStep,
    stepResult,
    setStepResult,
    loading,
    setLoading,
    error,
    setError,
  };
}
