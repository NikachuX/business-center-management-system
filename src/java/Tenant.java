public final class Tenant {
    private final long id;
    private final TenantData data;

    public Tenant(long id, TenantData data) {
        this.id = id;
        this.data = new TenantData(data.name().strip(), data.inn().strip(),
                data.contactPerson().strip(), data.phone().strip(), data.email());
    }

    public long id() { return id; }
    public TenantData data() { return data; }
}
