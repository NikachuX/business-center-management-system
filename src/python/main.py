from datetime import date, datetime, timedelta

from register_application import register_application
from find_available_premises import find_available_premises
from reserve_premise import reserve_premise
from generate_commercial_offer import generate_commercial_offer
from update_premise_and_lease import update_premise_and_lease
from utils import collect_busy_periods, next_id


def replace_record(records, updated):
    for index, record in enumerate(records):
        if record['id'] == updated['id']:
            records[index] = updated
            return
    raise ValueError(f"Не найдена запись для обновления: {updated['id']}")


def main():
    now = datetime(2026, 11, 2, 10, 0)
    tenants, applications, reservations = [], [], []
    offers, offer_items, lease_contracts = [], [], []
    premises = [
        {'id': 1, 'number': '201', 'floor': 2, 'area': 50,
         'base_monthly_rate': 1200, 'operational_status': 'available'},
        {'id': 2, 'number': '301', 'floor': 3, 'area': 60,
         'base_monthly_rate': 900, 'operational_status': 'available'},
        {'id': 3, 'number': '202', 'floor': 2, 'area': 45,
         'base_monthly_rate': 1000, 'operational_status': 'maintenance'},
    ]
    tenant_data = {
        'name': 'ООО Пример', 'inn': '7700000001',
        'contact_person': 'Иван Иванов', 'phone': '+79000000001',
        'email': 'office@example.ru',
    }
    requirements = {
        'activity_type': 'Разработка программного обеспечения',
        'min_area': 40, 'max_area': 70, 'preferred_floor': 2,
        'max_monthly_budget': 80000,
        'rental_start_date': date(2026, 12, 1),
        'rental_end_date': date(2026, 12, 31),
    }

    print('1. Регистрация заявки')
    result = register_application(
        tenant_data, requirements, tenants, applications, now)
    if result['tenant_created']:
        tenants.append(result['tenant'])
    application = result['application']
    applications.append(application)
    print(
        f"Арендатор: {result['tenant']['name']}; "
        f"заявка: {application['id']}; статус: {application['status']}"
    )

    print('\n2. Подбор свободных помещений')
    candidates = find_available_premises(
        application, premises, reservations, offer_items, lease_contracts, now)
    for candidate in candidates:
        print(
            f"Помещение {candidate['number']}; этаж {candidate['floor']}; "
            f"стоимость {candidate['monthly_cost']:.2f} руб./мес.; "
            f"совпадение этажа: {'да' if candidate['floor_match'] else 'нет'}"
        )
    if not candidates:
        print('Подходящих помещений нет. Демонстрация завершена.')
        return
    selected_id = candidates[0]['premise_id']
    selected = next(p for p in premises if p['id'] == selected_id)

    print('\n3. Резервирование выбранного помещения')
    contract_periods = collect_busy_periods([], offer_items, lease_contracts, now)
    reservation = reserve_premise(
        application, selected, reservations, contract_periods,
        now, next_id(reservations), timedelta(days=3))
    reservations.append(reservation)
    print(
        f"Резерв: {reservation['id']}; помещение: {selected['number']}; "
        f"действует до: {reservation['expires_at'].isoformat(' ')}"
    )

    print('\n4. Формирование коммерческого предложения')
    result = generate_commercial_offer(
        application, [selected], offers, offer_items, now)
    offers.append(result['offer'])
    offer_items.extend(result['items'])
    chosen_item = result['items'][0]
    cost = result['costs'][chosen_item['id']]
    print(
        f"Предложение: {result['offer']['id']}; "
        f"позиция: {chosen_item['id']}; месяцев: {cost['months']}"
    )
    print(
        f"Стоимость в месяц: {cost['monthly_cost']:.2f} руб.; "
        f"за период: {cost['total_cost']:.2f} руб."
    )

    contract = {
        'id': next_id(lease_contracts),
        'commercial_offer_item_id': chosen_item['id'],
        'number': 'АР-001', 'created_at': now, 'signed_at': None,
        'status': 'draft',
    }
    if any(c['commercial_offer_item_id'] == chosen_item['id'] for c in lease_contracts):
        raise ValueError('Для выбранной позиции уже существует договор')
    lease_contracts.append(contract)
    signed_at = now + timedelta(days=1)

    print('\n5. Фиксация подписания договора')
    result = update_premise_and_lease(
        contract, offer_items, offers, applications,
        tenants, premises, reservations, signed_at)
    replace_record(lease_contracts, result['contract'])
    replace_record(applications, result['application'])
    reservations[:] = result['reservations']
    print(
        f"Договор: {result['contract']['number']}; "
        f"статус: {result['contract']['status']}"
    )
    print(
        f"Статус заявки: {applications[0]['status']}; "
        f"статус резерва: {reservations[0]['status']}"
    )
    print(
        f"Арендатор: {result['lease_info']['tenant_name']}; "
        f"помещение: {result['lease_info']['premise_number']}"
    )

    print('\nПроверка доступности после заключения договора')
    remaining = find_available_premises(
        applications[0], premises, reservations, offer_items, lease_contracts, signed_at)
    print(
        f"Доступные варианты: {', '.join(c['number'] for c in remaining) or 'нет'}"
    )
    print(
        f"Сохранено: арендаторов {len(tenants)}; заявок {len(applications)}; "
        f"резервов {len(reservations)}; предложений {len(offers)}; "
        f"позиций {len(offer_items)}; договоров {len(lease_contracts)}"
    )


if __name__ == '__main__':
    try:
        main()
    except ValueError as error:
        raise SystemExit(f"Ошибка: {error}")