from datetime import timedelta

from utils import count_rental_months, next_id, OFFER_FORMED
from validators import validate_offer_input

def generate_commercial_offer(application, premises, offers, offer_items, now,
                              valid_days=14, proposed_rates=None):
    if proposed_rates is None:
        proposed_rates = {}

    errors = validate_offer_input(premises, proposed_rates)
    if errors:
        raise ValueError("; ".join(errors))

    start = application["rental_start_date"]
    end = application["rental_end_date"]
    if start > end:
        raise ValueError("дата начала аренды позже даты окончания")

    months = count_rental_months(start, end)
    offer_id = next_id(offers)
    offer = {
        "id": offer_id,
        "application_id": application["id"],
        "created_at": now,
        "valid_until": now + timedelta(days=valid_days),
        "status": OFFER_FORMED,
    }

    items = []
    costs = {}
    seen_premises = set()
    item_id = next_id(offer_items)
    total_cost = 0
    for premise in premises:
        if premise["id"] in seen_premises:
            raise ValueError(
                f"помещение {premise['number']} указано в предложении повторно"
            )
        seen_premises.add(premise["id"])

        rate = proposed_rates.get(premise["id"], premise["base_monthly_rate"])
        monthly_cost = rate * premise["area"]
        item_total = monthly_cost * months
        items.append({
            "id": item_id,
            "commercial_offer_id": offer_id,
            "premise_id": premise["id"],
            "monthly_rate": rate,
            "rental_start_date": start,
            "rental_end_date": end,
        })
        costs[item_id] = {"monthly_cost": monthly_cost, "months": months,
                          "total_cost": item_total}
        total_cost += item_total
        item_id += 1

    return {"offer": offer, "items": items, "costs": costs,
                  "total_cost": total_cost}