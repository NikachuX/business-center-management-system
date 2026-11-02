from datetime import date, datetime

def is_blank(value):
    return value is None or str(value).strip() == ""

def is_number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool)

def is_date(value):
    return isinstance(value, date) and not isinstance(value, datetime)

def is_valid_inn(inn):
    text = str(inn).strip()
    return text.isdigit() and len(text) in (10, 12)

def is_valid_email(email):
    parts = str(email).split("@")
    return len(parts) == 2 and parts[0] != "" and "." in parts[1]

def find_missing_fields(data, required_fields, label):
    errors = []
    for field in required_fields:
        if is_blank(data.get(field)):
            errors.append("не заполнено поле %s: %s" % (label, field))
    return errors


def validate_tenant_data(tenant_data):
    errors = find_missing_fields(
        tenant_data, ("name", "inn", "contact_person", "phone"), "арендатора")

    inn = tenant_data.get("inn")
    if not is_blank(inn) and not is_valid_inn(inn):
        errors.append("ИНН должен состоять из 10 или 12 цифр")

    email = tenant_data.get("email")
    if not is_blank(email) and not is_valid_email(email):
        errors.append("некорректный email")
    return errors


def validate_requirements(requirements):
    errors = find_missing_fields(
        requirements,
        ("activity_type", "min_area", "max_area", "max_monthly_budget",
         "rental_start_date", "rental_end_date"),
        "заявки")

    for field in ("min_area", "max_area", "max_monthly_budget"):
        value = requirements.get(field)
        if is_blank(value):
            continue
        if not is_number(value):
            errors.append("поле заявки %s должно быть числом" % field)
        elif value <= 0:
            errors.append("поле заявки %s должно быть положительным" % field)

    for field in ("rental_start_date", "rental_end_date"):
        value = requirements.get(field)
        if not is_blank(value) and not is_date(value):
            errors.append("поле заявки %s должно быть датой" % field)

    floor = requirements.get("preferred_floor")
    if floor is not None and (not isinstance(floor, int) or isinstance(floor, bool)):
        errors.append("поле заявки preferred_floor должно быть целым числом")
    return errors


def validate_offer_input(premises, proposed_rates):
    errors = []
    if not premises:
        errors.append("не выбрано ни одного помещения")
        return errors

    for premise in premises:
        rate = proposed_rates.get(premise["id"], premise["base_monthly_rate"])
        if not is_number(rate) or rate <= 0:
            errors.append("ставка для помещения %s должна быть положительным числом"
                          % premise["number"])
    return errors
