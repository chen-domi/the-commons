ALTER TABLE inventory_items
    ADD COLUMN organization_id BIGINT;

UPDATE inventory_items AS item
SET organization_id = organization.id
FROM organizations AS organization
WHERE LOWER(TRIM(item.organization)) = LOWER(TRIM(organization.name));

ALTER TABLE inventory_items
    ADD CONSTRAINT fk_inventory_item_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id);

CREATE INDEX idx_inventory_item_organization_id
    ON inventory_items(organization_id);
