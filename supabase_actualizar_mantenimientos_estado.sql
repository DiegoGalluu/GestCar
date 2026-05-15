alter table mantenimientos
add column if not exists realizado boolean not null default true;

alter table mantenimientos
add column if not exists fecha_realizado bigint;

update mantenimientos
set realizado = true
where realizado is null;

create index if not exists idx_mantenimientos_vehiculo_id on mantenimientos(vehiculo_id);
alter table mantenimientos enable row level security;

drop policy if exists "los usuarios ven sus mantenimientos" on mantenimientos;
create policy "los usuarios ven sus mantenimientos"
    on mantenimientos for select
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = mantenimientos.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios crean sus mantenimientos" on mantenimientos;
create policy "los usuarios crean sus mantenimientos"
    on mantenimientos for insert
    with check (
        exists (
            select 1
            from vehiculos v
            where v.id = mantenimientos.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios actualizan sus mantenimientos" on mantenimientos;
create policy "los usuarios actualizan sus mantenimientos"
    on mantenimientos for update
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = mantenimientos.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    )
    with check (
        exists (
            select 1
            from vehiculos v
            where v.id = mantenimientos.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios eliminan sus mantenimientos" on mantenimientos;
create policy "los usuarios eliminan sus mantenimientos"
    on mantenimientos for delete
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = mantenimientos.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );
