set
referential_integrity false;
truncate table devices restart identity;
truncate table species restart identity;
truncate table species_targets restart identity;
truncate table users restart identity;
truncate table device_registration_tokens restart identity;
truncate table login_one_time_tokens restart identity;
truncate table authorities restart identity;
set
referential_integrity true;

insert into users (username, password, enabled)
values ('alice@test.com', '{noop}password', 1);

insert into authorities(username, authority)
values ('alice@test.com', 'ROLE_USER');

insert into species(species, description)
values ('APIS_MELLIFERA', 'Honey bee.'),
       ('VESPA_CABRO', 'European Hornet.'),
       ('VESPA_VELUTINA_NIGRITHORAX', 'Yellow Legged Asian Hornet - Nigrithorax.');

insert into devices (id, name, run_mode, device_state, device_enabled, username, created_at, updated_at, last_seen_at)
values ('00000000-0000-0000-0000-000000000001', 'Shed Hive',      'ALWAYS_ON', 'PROVISIONED',   true,  'alice@test.com', '2026-01-01 00:00:00', '2026-01-01 00:00:00', '2026-03-18 06:00:00'),
       ('00000000-0000-0000-0000-000000000002', 'Garden Monitor', 'DEFAULT',   'UNPROVISIONED', true,  'alice@test.com', '2026-01-02 00:00:00', '2026-01-02 00:00:00', null),
       ('00000000-0000-0000-0000-000000000003', 'Orchard Trap',   'ALWAYS_ON', 'PROVISIONED',   false, 'alice@test.com', '2026-01-03 00:00:00', '2026-01-03 00:00:00', '2026-03-10 12:00:00');

