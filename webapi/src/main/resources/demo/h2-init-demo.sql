set referential_integrity false;
truncate table devices restart identity;
truncate table species restart identity;
truncate table species_targets restart identity;
truncate table users restart identity;
truncate table device_registration_tokens restart identity;
truncate table login_one_time_tokens restart identity;
set referential_integrity true;

insert into users (username, password, enabled) values ('alice@test.com', '{noop}password', true);
insert into authorities (username, authority) values ('alice@test.com', 'ROLE_USER');

insert into species(species, description)
values
-- Core targets
('APIS_MELLIFERA', 'Western honey bee (Apis mellifera).'),
('VESPA_CRABRO', 'European hornet (Vespa crabro).'),
('VESPA_VELUTINA_NIGRITHORAX', 'Asian hornet (Vespa velutina nigrithorax).'),
('VESPULA_VULGARIS', 'Common wasp (Vespula vulgaris).'),
('VESPULA_GERMANICA', 'German wasp (Vespula germanica).'),
('DOLICHOVESPULA_MEDIA', 'Median wasp (Dolichovespula media).'),
('POLISTES_DOMINULA', 'European paper wasp (Polistes dominula).'),
('BOMBUS_TERRESTRIS', 'Buff-tailed bumblebee (Bombus terrestris).'),
('BOMBUS_PASCUORUM', 'Common carder bee (Bombus pascuorum).'),
('EPISYRPHUS_BALTEATUS', 'Marmalade hoverfly (Episyrphus balteatus).'),
('VOLUCELLA_ZONARIA', 'Hornet mimic hoverfly (Volucella zonaria).'),
('CALLIPHORA_VICINA', 'Bluebottle fly (Calliphora vicina).'),
('LUCILIA_SERICATA', 'Green bottle fly (Lucilia sericata).');

insert into devices (id, name, run_mode, device_state, device_enabled, username, created_at, updated_at, last_seen_at)
values
    ('d1000000-0000-0000-0000-000000000001', 'Orchard Hive North',      'ALWAYS_ON',          'PROVISIONED',   true,  'alice@test.com', '2025-11-01 08:00:00', '2026-01-10 09:15:00', '2026-03-18 04:12:00'),
    ('d1000000-0000-0000-0000-000000000002', 'Orchard Hive South',      'ALWAYS_ON',          'PROVISIONED',   true,  'alice@test.com', '2025-11-02 08:00:00', '2026-01-12 10:30:00', '2026-03-18 04:08:00'),
    ('d1000000-0000-0000-0000-000000000003', 'Barn Roof Trap',           'DEFAULT',            'PROVISIONED',   true,  'alice@test.com', '2025-11-15 09:00:00', '2026-02-01 14:00:00', '2026-03-17 22:45:00'),
    ('d1000000-0000-0000-0000-000000000004', 'Polytunnel Monitor',       'ALWAYS_ON',          'PROVISIONED',   true,  'alice@test.com', '2025-12-01 10:00:00', '2026-02-14 11:45:00', '2026-03-18 03:55:00'),
    ('d1000000-0000-0000-0000-000000000005', 'Garden Fence East',        'DEFAULT',            'PROVISIONED',   true,  'alice@test.com', '2025-12-10 07:30:00', '2026-02-20 08:00:00', '2026-03-15 14:30:00'),
    ('d1000000-0000-0000-0000-000000000006', 'Meadow Post A',            'ALWAYS_ON',          'PROVISIONED',   false, 'alice@test.com', '2025-12-15 11:00:00', '2026-03-01 09:00:00', '2026-03-18 02:00:00'),
    ('d1000000-0000-0000-0000-000000000007', 'Meadow Post B',            'DEFAULT',            'UNPROVISIONED', true,  'alice@test.com', '2026-01-05 12:00:00', '2026-01-05 12:00:00', null),
    ('d1000000-0000-0000-0000-000000000008', 'Shed Roof Unit',           'TRAINING_UPLOADER',  'PROVISIONED',   true,  'alice@test.com', '2026-01-08 14:00:00', '2026-02-28 16:30:00', '2026-03-18 05:30:00'),
    ('d1000000-0000-0000-0000-000000000009', 'Driveway Gate Sensor',     'ALWAYS_ON',          'PROVISIONED',   true,  'alice@test.com', '2026-01-20 08:45:00', '2026-03-05 10:00:00', '2026-03-12 08:00:00'),
    ('d1000000-0000-0000-0000-000000000010', 'Compost Bay Trap',         'DEFAULT',            'UNPROVISIONED', true,  'alice@test.com', '2026-02-01 09:00:00', '2026-02-01 09:00:00', null),
    ('d1000000-0000-0000-0000-000000000011', 'Hedgerow Station 1',       'ALWAYS_ON',          'PROVISIONED',   true,  'alice@test.com', '2026-02-10 07:00:00', '2026-03-10 08:30:00', '2026-03-18 01:45:00'),
    ('d1000000-0000-0000-0000-000000000012', 'Hedgerow Station 2',       'ALWAYS_ON',          'PROVISIONED',   false, 'alice@test.com', '2026-02-10 07:05:00', '2026-03-10 08:35:00', '2026-03-10 08:35:00'),
    ('d1000000-0000-0000-0000-000000000013', 'Hive Row Centre',          'DEFAULT',            'PROVISIONED',   true,  'alice@test.com', '2026-02-20 10:00:00', '2026-03-12 11:00:00', '2026-03-18 00:10:00'),
    ('d1000000-0000-0000-0000-000000000014', 'Field Corner Unit',        'TRAINING_UPLOADER',  'UNPROVISIONED', true,  'alice@test.com', '2026-03-01 09:30:00', '2026-03-01 09:30:00', null),
    ('d1000000-0000-0000-0000-000000000015', 'Roof Garden Node',         'ALWAYS_ON',          'PROVISIONED',   true,  'alice@test.com', '2026-03-05 08:00:00', '2026-03-15 09:00:00', '2026-03-18 03:20:00');

insert into species_targets (device, species, detection_threshold)
values
    ('d1000000-0000-0000-0000-000000000001', 'APIS_MELLIFERA',              75.0),
    ('d1000000-0000-0000-0000-000000000001', 'VESPA_VELUTINA_NIGRITHORAX',  80.0),
    ('d1000000-0000-0000-0000-000000000002', 'APIS_MELLIFERA',              70.0),
    ('d1000000-0000-0000-0000-000000000002', 'VESPA_CRABRO',                 65.0),
    ('d1000000-0000-0000-0000-000000000003', 'VESPA_VELUTINA_NIGRITHORAX',  85.0),
    ('d1000000-0000-0000-0000-000000000004', 'APIS_MELLIFERA',              72.0),
    ('d1000000-0000-0000-0000-000000000005', 'VESPA_CRABRO',                 60.0),
    ('d1000000-0000-0000-0000-000000000005', 'VESPA_VELUTINA_NIGRITHORAX',  78.0),
    ('d1000000-0000-0000-0000-000000000006', 'APIS_MELLIFERA',              70.0),
    ('d1000000-0000-0000-0000-000000000008', 'VESPA_VELUTINA_NIGRITHORAX',  90.0),
    ('d1000000-0000-0000-0000-000000000009', 'APIS_MELLIFERA',              68.0),
    ('d1000000-0000-0000-0000-000000000009', 'VESPA_CRABRO',                 72.0),
    ('d1000000-0000-0000-0000-000000000011', 'VESPA_VELUTINA_NIGRITHORAX',  82.0),
    ('d1000000-0000-0000-0000-000000000012', 'APIS_MELLIFERA',              75.0),
    ('d1000000-0000-0000-0000-000000000013', 'APIS_MELLIFERA',              71.0),
    ('d1000000-0000-0000-0000-000000000013', 'VESPA_VELUTINA_NIGRITHORAX',  77.0),
    ('d1000000-0000-0000-0000-000000000015', 'VESPA_CRABRO',                 65.0),
    ('d1000000-0000-0000-0000-000000000015', 'VESPA_VELUTINA_NIGRITHORAX',  88.0);
