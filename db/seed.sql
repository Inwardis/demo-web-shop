INSERT INTO catalog.product (sku, name, description, price_cents, stock) VALUES
  ('MUG-01',  'Enamel camp mug',      'Holds 350 ml. Survives a fire, a dishwasher and a toddler.', 1800, 120),
  ('KNF-02',  'Folding pocket knife', 'Carbon steel, ash handle, liner lock.',                        4900,  40),
  ('LMP-03',  'Rechargeable lantern', '300 lumens, USB-C, 40 hours on low.',                         6500,  25),
  ('BAG-04',  'Waxed canvas daypack', '22 litres, leather straps, a laptop sleeve.',                 12900,  12)
ON CONFLICT (sku) DO NOTHING;
