from utils import (
    is_reservation_active,
    periods_overlap,
    PREMISE_AVAILABLE,
    RESERVATION_ACTIVE
)


def reserve_premise(application, premise, reservations, contract_periods,
                    now, new_id, hold_period):
    start = application["rental_start_date"]
    end = application["rental_end_date"]

    if premise["operational_status"] != PREMISE_AVAILABLE:
        raise ValueError("помещение недоступно для сдачи")

    if start > end:
        raise ValueError("дата начала аренды позже даты окончания")

    for res in reservations:
        if res["premise_id"] != premise["id"]:
            continue
        if not is_reservation_active(res, now):
            continue
        if periods_overlap(
                start, end,
                res["rental_start_date"], res["rental_end_date"]
        ):
            raise ValueError(
                f"конфликт с действующим резервом (id={res['id']})"
            )

    for period in contract_periods:
        if period["premise_id"] != premise["id"]:
            continue
        if periods_overlap(start, end, period["start"], period["end"]):
            raise ValueError("конфликт с заключённым договором аренды")

    reservation = {
        "id": new_id,
        "application_id": application["id"],
        "premise_id": premise["id"],
        "created_at": now,
        "rental_start_date": start,
        "rental_end_date": end,
        "expires_at": now + hold_period,
        "status": RESERVATION_ACTIVE,
    }
    return reservation