ALTER TABLE users ADD COLUMN email_verified boolean NOT NULL DEFAULT false;
CREATE TABLE email_verifications(id uuid PRIMARY KEY,user_id uuid NOT NULL REFERENCES users,token_hash text UNIQUE NOT NULL,expires_at timestamptz NOT NULL,used boolean NOT NULL DEFAULT false);
