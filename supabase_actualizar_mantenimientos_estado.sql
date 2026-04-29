alter table mantenimientos
add column if not exists realizado boolean not null default true;

alter table mantenimientos
add column if not exists fecha_realizado bigint;

update mantenimientos
set realizado = true
where realizado is null;
