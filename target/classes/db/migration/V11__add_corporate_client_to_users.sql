ALTER TABLE users ADD COLUMN corporate_client_id UUID REFERENCES corporate_clients(id);
