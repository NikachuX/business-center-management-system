from utils import (
    collect_busy_periods,
    periods_overlap,
    PREMISE_AVAILABLE
)

def find_available_premises(application, premises, reservations, offer_items, lease_contracts, now):
    busy_periods = collect_busy_periods(reservations, offer_items, lease_contracts, now)
    start = application["rental_start_date"]
    end = application["rental_end_date"]
    preferred_floor = application["preferred_floor"]

    candidates = []
    for premise in premises:
        if premise["operational_status"] != PREMISE_AVAILABLE:
            continue
        if not (application["min_area"] <= premise["area"] <= application["max_area"]):
            continue
        monthly_cost = premise["base_monthly_rate"] * premise["area"]
        if monthly_cost > application["max_monthly_budget"]:
            continue

        is_free = True
        for period in busy_periods:
            if period["premise_id"] != premise["id"]:
                continue
            if periods_overlap(start, end, period["start"], period["end"]):
                is_free = False
                break
        if not is_free:
            continue

        floor_match = preferred_floor is None or premise["floor"] == preferred_floor
        candidates.append({
            "premise_id": premise["id"],
            "number": premise["number"],
            "floor": premise["floor"],
            "area": premise["area"],
            "monthly_cost": monthly_cost,
            "floor_match": floor_match,
        })

    return sorted(candidates, key=lambda c: (not c["floor_match"], c["monthly_cost"]))
