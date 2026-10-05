-- Reference data (not demo data): cuisine catalogue.
INSERT INTO cuisine (id, name)
SELECT gen_random_uuid(), c FROM (VALUES
  ('Maharashtrian'), ('North Indian'), ('South Indian'), ('Bengali'), ('Gujarati'),
  ('Bakery'), ('Snacks'), ('Biryani'), ('Kolhapuri'), ('Sweets')
) AS t(c);
