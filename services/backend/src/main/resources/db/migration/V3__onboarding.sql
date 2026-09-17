CREATE TABLE onboarding_drafts(user_id uuid PRIMARY KEY REFERENCES users, data jsonb NOT NULL, updated_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE product_metrics(id uuid PRIMARY KEY, user_id uuid REFERENCES users, name varchar(60) NOT NULL, value numeric NOT NULL, created_at timestamptz NOT NULL DEFAULT now());
