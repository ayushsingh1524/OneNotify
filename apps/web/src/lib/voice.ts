export interface VoiceIntent {
  provider: string | null;
  category: string | null;
  accountKnown: boolean;
  guidance: string;
}
export function parseVoiceIntent(text: string): VoiceIntent {
  const normalized = text.toLowerCase();
  const provider =
    normalized.includes("lic") || normalized.includes("एलआईसी")
      ? "LIC"
      : normalized.includes("sbi")
        ? "SBI"
        : normalized.includes("airtel")
          ? "Airtel"
          : null;
  return {
    provider,
    category:
      provider === "LIC"
        ? "Insurance"
        : provider === "SBI"
          ? "Banking"
          : provider
            ? "Telecom"
            : null,
    accountKnown: !/(nahi|नहीं|unknown|don't know)/i.test(text),
    guidance:
      "Confirm the suggested organization with your family. Account numbers are not required to begin.",
  };
}
export interface VoiceAssistant {
  listen(language: string): Promise<string>;
}
export class BrowserVoiceAssistant implements VoiceAssistant {
  listen(language: string): Promise<string> {
    return new Promise((resolve, reject) => {
      const host = window as any;
      const Recognition =
        host.SpeechRecognition || host.webkitSpeechRecognition;
      if (!Recognition) {
        reject(
          new Error(
            "Voice input is not supported in this browser. You can type instead.",
          ),
        );
        return;
      }
      const recognition = new Recognition();
      recognition.lang = language;
      recognition.interimResults = false;
      recognition.onresult = (event: any) =>
        resolve(event.results[0][0].transcript);
      recognition.onerror = () =>
        reject(new Error("Voice input could not finish. Please type instead."));
      recognition.start();
    });
  }
}
