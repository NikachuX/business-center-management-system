from utils import (
    CONTRACT_SIGNED,
    APPLICATION_CONTRACT_SIGNED,
    RESERVATION_ACTIVE,
    RESERVATION_CANCELLED,
    find_by_id,
    RESERVATION_CONVERTED
)

def update_premise_and_lease(contract, offer_items, offers, applications,
                             tenants, premises, reservations, now):
    if contract["status"] == CONTRACT_SIGNED:
        raise ValueError("договор уже заключён")

    item = find_by_id(offer_items, contract["commercial_offer_item_id"])
    if item is None:
        raise ValueError("не найдена позиция предложения договора")
    offer = find_by_id(offers, item["commercial_offer_id"])
    application = find_by_id(applications, offer["application_id"]) if offer else None
    tenant = find_by_id(tenants, application["tenant_id"]) if application else None
    premise = find_by_id(premises, item["premise_id"])
    if offer is None or application is None or tenant is None or premise is None:
        raise ValueError("нарушена цепочка связей договора")

    new_contract = dict(contract)
    new_contract["status"] = CONTRACT_SIGNED
    new_contract["signed_at"] = now

    new_application = dict(application)
    new_application["status"] = APPLICATION_CONTRACT_SIGNED

    updated_reservations = []
    for res in reservations:
        res_copy = dict(res)
        if (res["application_id"] == application["id"]
                and res["status"] == RESERVATION_ACTIVE):
            if res["premise_id"] == premise["id"]:
                res_copy["status"] = RESERVATION_CONVERTED
            else:
                res_copy["status"] = RESERVATION_CANCELLED
        updated_reservations.append(res_copy)

    lease_info = {
        "contract_number": contract["number"],
        "tenant_id": tenant["id"],
        "tenant_name": tenant["name"],
        "premise_id": premise["id"],
        "premise_number": premise["number"],
        "rental_start_date": item["rental_start_date"],
        "rental_end_date": item["rental_end_date"],
        "monthly_rate": item["monthly_rate"],
        "monthly_cost": item["monthly_rate"] * premise["area"],
        "signed_at": now,
    }
    return {"contract": new_contract, "application": new_application,
                  "reservations": updated_reservations, "lease_info": lease_info}