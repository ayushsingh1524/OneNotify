import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { Status, ErrorBox } from "./shared";
describe("accessible workflow feedback", () => {
  it("uses plain language for an unstarted request", () => {
    render(<Status state="DRAFT" />);
    expect(screen.getByText("Not started")).toBeInTheDocument();
  });
  it("announces errors", () => {
    render(<ErrorBox error={new Error("A document is missing")} />);
    expect(screen.getByRole("alert")).toHaveTextContent(
      "A document is missing",
    );
  });
});
