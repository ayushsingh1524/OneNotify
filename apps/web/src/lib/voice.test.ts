import { describe, it, expect } from "vitest";
import { parseVoiceIntent } from "./voice";
describe("voice intent", () => {
  it("keeps policy ownership unconfirmed in Hindi/English mixed input", () => {
    const intent = parseVoiceIntent(
      "Mere papa ka LIC tha lekin policy number nahi pata",
    );
    expect(intent.provider).toBe("LIC");
    expect(intent.category).toBe("Insurance");
    expect(intent.accountKnown).toBe(false);
    expect(intent.guidance).toContain("Confirm");
  });
  it("does not invent a provider", () =>
    expect(parseVoiceIntent("I am not sure").provider).toBeNull());
});
