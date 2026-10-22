create index pet_events_device_id_idx on public.pet_events(device_id)
    where device_id is not null;

create index pet_commands_device_id_idx on public.pet_commands(device_id)
    where device_id is not null;
