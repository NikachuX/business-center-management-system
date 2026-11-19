from datetime import timedelta

PREMISE_AVAILABLE = "available"
RESERVATION_ACTIVE = "active"
CONTRACT_SIGNED = "signed"
APPLICATION_NEW = "new"
APPLICATION_CONTRACT_SIGNED = "contract_signed"
OFFER_FORMED = "formed"
RESERVATION_CONVERTED = "converted"
RESERVATION_CANCELLED = "cancelled"

def periods_overlap(start_a, end_a, start_b, end_b):
    return start_a <= end_b and start_b <= end_a


def is_reservation_active(reservation, now):
    if reservation["status"] != RESERVATION_ACTIVE:
        return False
    if reservation["expires_at"] <= now:
        return False
    return True


def collect_busy_periods(reservations, offer_items, lease_contracts, now):
    items_by_id = {}
    for item in offer_items:
        items_by_id[item["id"]] = item

    busy = []
    for res in reservations:
        if is_reservation_active(res, now):
            busy.append({
                "premise_id": res["premise_id"],
                "start": res["rental_start_date"],
                "end": res["rental_end_date"],
            })
    for contract in lease_contracts:
        if contract["status"] == CONTRACT_SIGNED:
            item = items_by_id[contract["commercial_offer_item_id"]]
            busy.append({
                "premise_id": item["premise_id"],
                "start": item["rental_start_date"],
                "end": item["rental_end_date"],
            })
    return busy

def next_id(entities):
    max_id = 0
    for entity in entities:
        if entity["id"] > max_id:
            max_id = entity["id"]
    return max_id + 1


def find_by_id(entities, entity_id):
    for entity in entities:
        if entity["id"] == entity_id:
            return entity
    return None


def count_rental_months(start, end):
    end_excl = end + timedelta(days=1)
    months = (end_excl.year - start.year) * 12 + (end_excl.month - start.month)
    if end_excl.day > start.day:
        months += 1
    if months < 1:
        months = 1
    return months