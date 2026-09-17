"use client";
import { use } from "react";
import { Workspace } from "@/components/workspace";
import { AllCases, CaseDashboard } from "@/components/dashboard";
import { CaseWizard } from "@/components/case-wizard";
import { Organizations, Discovery } from "@/components/organizations";
import { ProviderDetail } from "@/components/provider-detail";
import { Documents } from "@/components/documents";
import {
  Family,
  Tasks,
  Correspondence,
  Timeline,
  Notifications,
  CaseSettings,
  UserSettings,
} from "@/components/case-tools";
import { Admin } from "@/components/admin";
export default function Page({
  params,
}: {
  params: Promise<{ path?: string[] }>;
}) {
  const { path = [] } = use(params);
  const caseId = path[0] === "cases" ? path[1] : undefined;
  const section = caseId ? path[2] || "overview" : path[0] || "all";
  let content;
  if (caseId) {
    switch (section) {
      case "overview":
        content = <CaseDashboard caseId={caseId} />;
        break;
      case "organizations":
        content = path[3] ? (
          <ProviderDetail caseId={caseId} id={path[3]} />
        ) : (
          <Organizations caseId={caseId} />
        );
        break;
      case "discovery":
        content = <Discovery caseId={caseId} />;
        break;
      case "documents":
        content = <Documents caseId={caseId} />;
        break;
      case "family":
        content = <Family caseId={caseId} />;
        break;
      case "tasks":
        content = <Tasks caseId={caseId} />;
        break;
      case "correspondence":
        content = <Correspondence caseId={caseId} />;
        break;
      case "timeline":
        content = <Timeline caseId={caseId} />;
        break;
      case "settings":
        content = <CaseSettings caseId={caseId} />;
        break;
      case "profile":
        content = <CaseWizard caseId={caseId} />;
        break;
      default:
        content = <p>This page could not be found.</p>;
    }
  } else if (section === "new") content = <CaseWizard />;
  else if (section === "notifications") content = <Notifications />;
  else if (section === "settings") content = <UserSettings />;
  else if (section === "admin") content = <Admin />;
  else content = <AllCases />;
  return (
    <Workspace caseId={caseId} section={section}>
      {content}
    </Workspace>
  );
}
