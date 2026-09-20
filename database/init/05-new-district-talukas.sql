-- Newer project districts whose LGD talukas are currently stored
-- under their historical parent districts.
--
-- These records are intentionally inserted into the newer
-- project district IDs.
--
-- Generated manually from the verified LGD/project mapping.

INSERT IGNORE INTO talukas (id, name, district_id) VALUES

-- Chhattisgarh
(27000, 'Khairagarh', 616),
(27001, 'Manendragarh', 621),
(27002, 'Mohla', 622),
(27003, 'Manpur', 622),
(27004, 'Sarangarh', 628),

-- Madhya Pradesh
(27005, 'Maihar', 1427),
(27006, 'Nagda', 1431),

-- Nagaland
(27007, 'Chumoukedima', 1800),
(27008, 'Niuland', 1807),
(27009, 'Noklak', 1808),
(27010, 'Shamator', 1811),
(27011, 'Tseminyu', 1812),

-- Punjab
(27012, 'Malerkotla', 2012),

-- Sikkim
(27013, 'Pakyong', 2202),
(27014, 'Soreng', 2203),

-- Tamil Nadu
(27015, 'Mayiladuthurai', 2314),

-- Telangana
(27016, 'Hanamkonda', 2402);