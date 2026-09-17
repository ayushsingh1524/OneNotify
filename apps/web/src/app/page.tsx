import Link from "next/link";
import {
  ArrowRight,
  Check,
  FileCheck2,
  HeartHandshake,
  ShieldCheck,
  Sprout,
} from "lucide-react";
import { Button } from "@/components/ui/button";
export default function Home() {
  return (
    <div className="landing">
      <header className="landing-nav">
        <Link className="brand" href="/">
          <span className="brand-icon">
            <Sprout size={25} />
          </span>
          OneNotify<span className="brand-dot">.</span>
        </Link>
        <nav>
          <Link href="/help">How it helps</Link>
          <Link href="/privacy">Your privacy</Link>
          <Button asChild variant="secondary">
            <Link href="/login">
              Sign in <ArrowRight size={16} />
            </Link>
          </Button>
        </nav>
      </header>
      <main id="main">
        <section className="hero">
          <div className="hero-copy">
            <span className="overline">
              <span className="small-dot" />
              HERE FOR THE THINGS THAT COME AFTER
            </span>
            <h1>
              You don’t have to
              <br />
              figure it all out
              <br />
              <em>on your own.</em>
            </h1>
            <p>
              After losing someone, the paperwork can feel like too much. Bring
              the accounts, documents, and people helping you together in one
              calm place.
            </p>
            <div className="hero-actions">
              <Button asChild>
                <Link href="/register">
                  Let’s take the first step <ArrowRight size={17} />
                </Link>
              </Button>
              <Link className="text-link" href="/login">
                Explore the demo
              </Link>
            </div>
            <div className="hero-trust">
              <ShieldCheck size={17} />
              Private by design<span>•</span>Made for families in India
            </div>
          </div>
          <div className="hero-visual">
            <div className="visual-orbit orbit-one" />
            <div className="visual-orbit orbit-two" />
            <div className="plant-art">
              <Sprout size={95} strokeWidth={1} />
            </div>
            <div className="preview-card">
              <div className="preview-top">
                <span className="small-dot" />
                YOUR FAMILY WORKSPACE<span className="demo-pill">PREVIEW</span>
              </div>
              <h3>A clearer path forward.</h3>
              <p>One shared checklist. A little less to carry.</p>
              <div className="preview-row">
                <span className="provider-icon teal">SB</span>
                <div>
                  <strong>Bank accounts</strong>
                  <small>Documents together, ready for review</small>
                </div>
                <Check size={17} />
              </div>
              <div className="preview-row">
                <span className="provider-icon gold">LI</span>
                <div>
                  <strong>Insurance & policies</strong>
                  <small>A clear next step for every request</small>
                </div>
                <FileCheck2 size={17} />
              </div>
              <div className="preview-row">
                <span className="provider-icon purple">FA</span>
                <div>
                  <strong>Your family, in the loop</strong>
                  <small>Share the work with people you trust</small>
                </div>
                <HeartHandshake size={17} />
              </div>
              <div className="preview-footer">
                <ShieldCheck size={15} />
                You choose what to share, every time.
              </div>
            </div>
            <div className="floating-note">
              <span className="note-check">
                <Check size={16} />
              </span>
              One thing at a time is enough.
            </div>
          </div>
        </section>
        <section className="landing-bottom">
          <div>
            <span className="section-number">01</span>
            <h3>Tell us once.</h3>
            <p>Start with the basics. Save the rest for when you’re ready.</p>
          </div>
          <div>
            <span className="section-number">02</span>
            <h3>Bring it together.</h3>
            <p>Keep documents and organization checklists in one place.</p>
          </div>
          <div>
            <span className="section-number">03</span>
            <h3>Take the next step.</h3>
            <p>Know what needs you, what’s waiting, and what’s done.</p>
          </div>
        </section>
      </main>
      <footer className="landing-footer">
        <span>OneNotify · A little less to carry.</span>
        <span>
          Demo integrations only. OneNotify does not provide legal
          representation.
        </span>
      </footer>
    </div>
  );
}
