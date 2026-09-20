import csv
import re
from collections import defaultdict

PROJECT_FILE = "database/init/project-districts.txt"
LGD_FILE = "database/init/lgd-subdistricts.csv"
OUTPUT_FILE = "database/init/04-locations-complete.sql"


# ============================================================
# PROJECT DISTRICT ID RANGES
# ============================================================

STATE_RANGES = {
    "ANDAMAN AND NICOBAR ISLANDS": (2900, 2999),
    "ANDHRA PRADESH": (200, 299),
    "ARUNACHAL PRADESH": (300, 399),
    "ASSAM": (400, 499),
    "BIHAR": (500, 599),
    "CHANDIGARH": (3000, 3099),
    "CHHATTISGARH": (600, 699),
    "DADRA AND NAGAR HAVELI": (3100, 3199),
    "DAMAN AND DIU": (3100, 3199),
    "DELHI": (3200, 3299),
    "GOA": (700, 799),
    "GUJARAT": (800, 899),
    "HARYANA": (900, 999),
    "HIMACHAL PRADESH": (1000, 1099),
    "JAMMU AND KASHMIR": (3300, 3399),
    "JHARKHAND": (1100, 1199),
    "KARNATAKA": (1200, 1299),
    "KERALA": (1300, 1399),
    "LADAKH": (3400, 3499),
    "LAKSHADWEEP": (3500, 3599),
    "MADHYA PRADESH": (1400, 1499),
    "MAHARASHTRA": (1, 134),
    "MANIPUR": (1500, 1599),
    "MEGHALAYA": (1600, 1699),
    "MIZORAM": (1700, 1799),
    "NAGALAND": (1800, 1899),
    "ODISHA": (1900, 1999),
    "PUDUCHERRY": (3600, 3699),
    "PUNJAB": (2000, 2099),
    "RAJASTHAN": (2100, 2199),
    "SIKKIM": (2200, 2299),
    "TAMIL NADU": (2300, 2399),
    "TELANGANA": (2400, 2499),
    "TRIPURA": (2500, 2599),
    "UTTAR PRADESH": (2600, 2699),
    "UTTARAKHAND": (2700, 2799),
    "WEST BENGAL": (2800, 2899),
}


# ============================================================
# LGD NAME -> PROJECT NAME ALIASES
# ============================================================

ALIASES = {
    "AHMADABAD": "Ahmedabad",
    "ANUGUL": "Angul",
    "24 PARAGANAS NORTH": "North 24 Parganas",
    "24 PARAGANAS SOUTH": "South 24 Parganas",
    "ARVALLI": "Aravalli",
    "BAGALKOTE": "Bagalkot",
    "BALESHWAR": "Balasore",
    "BANAS KANTHA": "Banaskantha",
    "CHAMARAJANAGARA": "Chamarajanagar",
    "CHARKI DADRI": "Charkhi Dadri",
    "CHHOTAUDEPUR": "Chhota Udaipur",
    "CHIKKABALLAPURA": "Chikkaballapur",
    "COOCHBEHAR": "Cooch Behar",
    "DAVANGERE": "Davanagere",
    "DEVBHUMI DWARKA": "Devbhoomi Dwarka",
    "DINAJPUR DAKSHIN": "Dakshin Dinajpur",
    "DINAJPUR UTTAR": "Uttar Dinajpur",
    "DOHAD": "Dahod",
    "EAST NIMAR": "Khandwa",
    "EAST SINGHBUM": "East Singhbhum",
    "FIROZEPUR": "Ferozepur",
    "GANGANAGAR": "Sri Ganganagar",
    "GARIYABAND": "Gariaband",
    "JAGATSINGHAPUR": "Jagatsinghpur",
    "JAJAPUR": "Jajpur",
    "JANGOAN": "Jangaon",
    "KACHCHH": "Kutch",
    "KAIMUR (BHABUA)": "Kaimur",
    "KAMRUP METRO": "Kamrup Metropolitan",
    "KANCHIPURAM": "Kancheepuram",
    "KOREA": "Koriya",
    "KRA  DAADI": "Kra Daadi",
    "KUMURAM BHEEM ASIFABAD": "Komaram Bheem",
    "KUSHI NAGAR": "Kushinagar",
    "LAHUL AND SPITI": "Lahaul and Spiti",
    "LEH LADAKH": "Leh",
    "LEPARADA": "Lepa Rada",
    "MAHESANA": "Mehsana",
    "MALDAH": "Malda",
    "MARIGAON": "Morigaon",
    "MEDCHAL MALKAJGIRI": "Medchal-Malkajgiri",
    "MEDINIPUR EAST": "Purba Medinipur",
    "MEDINIPUR WEST": "Paschim Medinipur",
    "NICOBARS": "Nicobar",
    "PANCH MAHALS": "Panchmahal",
    "PASHCHIM CHAMPARAN": "West Champaran",
    "PONDICHERRY": "Puducherry",
    "PURBI CHAMPARAN": "East Champaran",
    "RAE BARELI": "Raebareli",
    "RANGA REDDY": "Rangareddy",
    "RUDRA PRAYAG": "Rudraprayag",
    "S.A.S NAGAR": "Mohali",
    "SABAR KANTHA": "Sabarkantha",
    "SAIHA": "Siaha",
    "SANT KABEER NAGAR": "Sant Kabir Nagar",
    "SARAIKELA KHARSAWAN": "Seraikela Kharsawan",
    "SHAHID BHAGAT SINGH NAGAR": "Nawanshahr",
    "SIDDHARTH NAGAR": "Siddharthnagar",
    "SONEPUR": "Subarnapur",
    "SOUTH SALMARA MANCACHAR": "South Salmara-Mankachar",
    "SPSR NELLORE": "Nellore",
    "SRI MUKTSAR SAHIB": "Muktsar",
    "SUNDARGARH": "Sundergarh",
    "THE NILGIRIS": "Nilgiris",
    "THIRUVALLUR": "Tiruvallur",
    "THIRUVARUR": "Tiruvarur",
    "TUTICORIN": "Thoothukudi",
    "UDAM SINGH NAGAR": "Udham Singh Nagar",
    "UTTAR KASHI": "Uttarkashi",
    "VILLUPURAM": "Viluppuram",
    "VISAKHAPATANAM": "Visakhapatnam",
    "Y.S.R.": "YSR Kadapa",

    # Sikkim
    "EAST DISTRICT": "East Sikkim",
    "NORTH DISTRICT": "North Sikkim",
    "SOUTH DISTRICT": "South Sikkim",
    "WEST DISTRICT": "West Sikkim",

    # Delhi
    "CENTRAL": "Central Delhi",
    "EAST": "East Delhi",
    "NEW DELHI": "New Delhi",
    "NORTH": "North Delhi",
    "NORTH EAST": "North East Delhi",
    "NORTH WEST": "North West Delhi",
    "SOUTH": "South Delhi",
    "SOUTH EAST": "South East Delhi",
    "SOUTH WEST": "South West Delhi",
    "WEST": "West Delhi",

    # Andaman and Nicobar
    "SOUTH ANDAMANS": "South Andaman",

    # Union Territories
    "LAKSHADWEEP DISTRICT": "Lakshadweep",
    "DADRA AND NAGAR HAVELI": "Dadra and Nagar Haveli",
    "DAMAN": "Daman",
    "DIU": "Diu",
}


# ============================================================
# NORMALIZATION
# ============================================================

def norm(value):
    return re.sub(
        r"[^a-z0-9]+",
        " ",
        value.lower()
    ).strip()


# ============================================================
# READ PROJECT DISTRICTS
# ============================================================

project = {}

with open(
    PROJECT_FILE,
    encoding="utf-16"
) as f:

    for line in f:

        match = re.match(
            r"^\s*(\d+)\s+(.+?)\s*$",
            line
        )

        if match:
            district_id = int(match.group(1))
            district_name = match.group(2).strip()

            project[district_id] = district_name


# ============================================================
# BUILD PROJECT NAME LOOKUP
# ============================================================

project_by_name = defaultdict(list)

for district_id, district_name in project.items():

    project_by_name[
        norm(district_name)
    ].append(district_id)


# ============================================================
# READ LGD DATA
# ============================================================

with open(
    LGD_FILE,
    encoding="utf-8-sig",
    newline=""
) as f:

    rows = list(
        csv.DictReader(f)
    )


# ============================================================
# RESOLVE DISTRICT MAPPINGS
# ============================================================

resolved = {}
unresolved = set()

for row in rows:

    state = row[
        "State Name (In English)"
    ].strip().upper()

    lgd_name = row[
        "District Name (In English)"
    ].strip()

    if state not in STATE_RANGES:

        unresolved.add(
            (state, lgd_name)
        )

        continue

    candidate_name = ALIASES.get(
        lgd_name.upper(),
        lgd_name
    )

    candidates = project_by_name.get(
        norm(candidate_name),
        []
    )

    low, high = STATE_RANGES[state]

    candidates = [
        district_id
        for district_id in candidates
        if low <= district_id <= high
    ]

    if len(candidates) == 1:

        resolved[
            (state, lgd_name)
        ] = candidates[0]

    else:

        unresolved.add(
            (state, lgd_name)
        )


# ============================================================
# TELANGANA SPECIAL CASE
#
# Project has one Warangal district: 2431.
# LGD has Warangal Rural + Warangal Urban.
# Both map to project district 2431.
# ============================================================

resolved[
    ("TELANGANA", "WARANGAL RURAL")
] = 2431

resolved[
    ("TELANGANA", "WARANGAL URBAN")
] = 2431

unresolved.discard(
    ("TELANGANA", "WARANGAL RURAL")
)

unresolved.discard(
    ("TELANGANA", "WARANGAL URBAN")
)


# ============================================================
# MAPPING CHECK
# ============================================================

print()
print("========================================")
print("LGD / PROJECT DISTRICT MAPPING CHECK")
print("========================================")
print()

print(
    "LGD rows:",
    len(rows)
)

print(
    "Unique LGD district mappings:",
    len(resolved)
)

print(
    "Unresolved/ambiguous mappings:",
    len(unresolved)
)

print()

if unresolved:

    for state, name in sorted(unresolved):

        print(
            f"UNRESOLVED: {state} | {name}"
        )

    print()
    print(
        "SQL generation stopped because mappings are unresolved."
    )

    raise SystemExit(1)

else:

    print(
        "ALL DISTRICT MAPPINGS RESOLVED."
    )


# ============================================================
# EXISTING TALUKAS
#
# These records are already present in 03-locations.sql.
#
# Pune:
#   Haveli has ID 1.
#   The remaining manually seeded Pune talukas use 10000+ IDs.
#
# LGD spelling variations such as Mawal/Maval and
# Purandhar/Purandar are explicitly handled below.
# ============================================================

existing_talukas = {
    (1, "haveli"),
    (1, "ambegaon"),
    (1, "baramati"),
    (1, "bhor"),
    (1, "daund"),
    (1, "indapur"),
    (1, "junnar"),
    (1, "khed"),
    (1, "maval"),
    (1, "mawal"),
    (1, "mulshi"),
    (1, "purandar"),
    (1, "purandhar"),
    (1, "shirur"),
    (1, "velhe"),
}


# ============================================================
# EXISTING TALUKA ALIASES
#
# These are spelling variations that refer to an existing
# taluka already present in the project database.
# ============================================================

existing_taluka_aliases = {
    (1, "mawal"),
    (1, "purandhar"),
}


# ============================================================
# COLLECT LGD TALUKAS
# ============================================================

taluka_records = set()

for row in rows:

    state = row[
        "State Name (In English)"
    ].strip().upper()

    district_name = row[
        "District Name (In English)"
    ].strip()

    taluka_name = row[
        "Sub-District Name"
    ].strip()

    if not taluka_name:
        continue

    district_id = resolved.get(
        (state, district_name)
    )

    if district_id is None:
        continue

    key = (
        district_id,
        norm(taluka_name)
    )

    # Skip exact existing records.
    if key in existing_talukas:
        continue

    # Skip known spelling variants of existing records.
    if key in existing_taluka_aliases:
        continue

    taluka_records.add(
        (
            district_id,
            taluka_name
        )
    )


# ============================================================
# SORT DETERMINISTICALLY
# ============================================================

taluka_records = sorted(
    taluka_records,
    key=lambda x: (
        x[0],
        norm(x[1])
    )
)


# ============================================================
# GENERATE UNIQUE IDS
#
# Existing taluka IDs:
#   1
#   10000+
#
# New LGD records:
#   20000+
#
# This prevents ID collisions.
# ============================================================

next_id = 20000

values = []

for district_id, taluka_name in taluka_records:

    safe_name = taluka_name.replace(
        "'",
        "''"
    )

    values.append(
        f"({next_id}, '{safe_name}', {district_id})"
    )

    next_id += 1


# ============================================================
# WRITE SQL FILE
# ============================================================

output_lines = [
    "-- Auto-generated from LGD sub-district data.",
    "-- Generated by generate-taluka-sql.py",
    "-- Existing 03-locations.sql records are preserved.",
    "-- New records use IDs starting from 20000.",
    "",
    "INSERT IGNORE INTO talukas (id, name, district_id) VALUES",
]


if values:

    output_lines.append(
        ",\n".join(values) + ";"
    )

else:

    output_lines.append(
        "-- No new taluka records required."
    )


output_lines.append("")


with open(
    OUTPUT_FILE,
    "w",
    encoding="utf-8"
) as f:

    f.write(
        "\n".join(output_lines)
    )


# ============================================================
# FINAL SUMMARY
# ============================================================

print()
print("========================================")
print("TALUKA SQL GENERATED")
print("========================================")
print()

print(
    "LGD rows:",
    len(rows)
)

print(
    "Existing talukas preserved:",
    len(existing_talukas)
)

print(
    "New taluka records:",
    len(taluka_records)
)

if taluka_records:

    print(
        "Generated ID range:",
        f"20000 - {next_id - 1}"
    )

else:

    print(
        "Generated ID range: none"
    )

print(
    "Output file:",
    OUTPUT_FILE
)

print()