create table if not exists recordatorios (
    id text primary key,
    vehiculo_id text not null references vehiculos(id) on delete cascade,
    concepto text not null,
    fecha_limite bigint,
    kilometraje_limite double precision,
    periodicidad_tiempo_cantidad integer,
    periodicidad_tiempo_unidad text,
    periodicidad_kilometros double precision,
    completado boolean not null default false,
    fecha_completado bigint,
    notas text,
    actualizado_en bigint not null,
    created_at timestamptz default now()
);

alter table recordatorios
add column if not exists periodicidad_tiempo_cantidad integer;

alter table recordatorios
add column if not exists periodicidad_tiempo_unidad text;

alter table recordatorios
add column if not exists periodicidad_kilometros double precision;

alter table recordatorios
add column if not exists completado boolean not null default false;

alter table recordatorios
add column if not exists fecha_completado bigint;

alter table recordatorios
add column if not exists notas text;

alter table recordatorios
add column if not exists actualizado_en bigint not null default 0;

alter table recordatorios
add column if not exists created_at timestamptz default now();

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'recordatorios_vehiculo_id_fkey'
          and conrelid = 'recordatorios'::regclass
    ) then
        alter table recordatorios
        add constraint recordatorios_vehiculo_id_fkey
        foreign key (vehiculo_id) references vehiculos(id) on delete cascade;
    end if;
end $$;

create index if not exists idx_recordatorios_vehiculo_id on recordatorios(vehiculo_id);
alter table recordatorios enable row level security;

drop policy if exists "los usuarios ven sus recordatorios" on recordatorios;
create policy "los usuarios ven sus recordatorios"
    on recordatorios for select
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = recordatorios.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios crean sus recordatorios" on recordatorios;
create policy "los usuarios crean sus recordatorios"
    on recordatorios for insert
    with check (
        exists (
            select 1
            from vehiculos v
            where v.id = recordatorios.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios actualizan sus recordatorios" on recordatorios;
create policy "los usuarios actualizan sus recordatorios"
    on recordatorios for update
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = recordatorios.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    )
    with check (
        exists (
            select 1
            from vehiculos v
            where v.id = recordatorios.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios eliminan sus recordatorios" on recordatorios;
create policy "los usuarios eliminan sus recordatorios"
    on recordatorios for delete
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = recordatorios.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );
