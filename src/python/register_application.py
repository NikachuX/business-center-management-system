from utils import next_id, APPLICATION_NEW
from validators import validate_requirements, validate_tenant_data


def register_application(tenant_data, requirements, tenants, applications, now):
    errors = validate_tenant_data(tenant_data) + validate_requirements(requirements)
    if errors:
        raise ValueError("; ".join(errors))

    if requirements["min_area"] > requirements["max_area"]:
        errors.append("минимальная площадь больше максимальной")
    if requirements["rental_start_date"] > requirements["rental_end_date"]:
        errors.append("дата начала аренды позже даты окончания")
    if errors:
        raise ValueError("; ".join(errors))

    inn = str(tenant_data["inn"]).strip()

    tenant = None
    for existing in tenants:
        if existing["inn"] == inn:
            tenant = existing
            break
    tenant_created = tenant is None
    if tenant_created:
        tenant = {
            "id": next_id(tenants),
            "name": tenant_data["name"].strip(),
            "inn": inn,
            "contact_person": tenant_data["contact_person"].strip(),
            "phone": tenant_data["phone"].strip(),
            "email": tenant_data.get("email"),
        }

    application = {
        "id": next_id(applications),
        "tenant_id": tenant["id"],
        "created_at": now,
        "activity_type": requirements["activity_type"],
        "min_area": requirements["min_area"],
        "max_area": requirements["max_area"],
        "preferred_floor": requirements.get("preferred_floor"),
        "max_monthly_budget": requirements["max_monthly_budget"],
        "rental_start_date": requirements["rental_start_date"],
        "rental_end_date": requirements["rental_end_date"],
        "status": APPLICATION_NEW,
    }
    return {"application": application, "tenant": tenant, "tenant_created": tenant_created}