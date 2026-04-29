-- setup de supabase para las funcionalidades principales de gestcar
-- es seguro ejecutarlo varias veces porque usa create table if not exists y drop policy if exists

create table if not exists repostajes (
    id text primary key,
    vehiculo_id text not null references vehiculos(id) on delete cascade,
    fecha bigint not null,
    kilometros double precision not null,
    litros double precision not null,
    precio_por_litro double precision not null,
    importe_total double precision not null,
    lleno_completo boolean not null default true,
    gasolinera text,
    notas text,
    actualizado_en bigint not null,
    created_at timestamptz default now()
);

create index if not exists idx_repostajes_vehiculo_id on repostajes(vehiculo_id);
alter table repostajes enable row level security;

drop policy if exists "los usuarios ven sus repostajes" on repostajes;
create policy "los usuarios ven sus repostajes"
    on repostajes for select
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = repostajes.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios crean sus repostajes" on repostajes;
create policy "los usuarios crean sus repostajes"
    on repostajes for insert
    with check (
        exists (
            select 1
            from vehiculos v
            where v.id = repostajes.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios actualizan sus repostajes" on repostajes;
create policy "los usuarios actualizan sus repostajes"
    on repostajes for update
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = repostajes.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    )
    with check (
        exists (
            select 1
            from vehiculos v
            where v.id = repostajes.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios eliminan sus repostajes" on repostajes;
create policy "los usuarios eliminan sus repostajes"
    on repostajes for delete
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = repostajes.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

create table if not exists mantenimientos (
    id text primary key,
    vehiculo_id text not null references vehiculos(id) on delete cascade,
    tipo text not null,
    categoria text not null default 'MANTENIMIENTO',
    fecha bigint not null,
    kilometros double precision,
    coste double precision not null default 0,
    taller text,
    descripcion text,
    actualizado_en bigint not null,
    created_at timestamptz default now()
);

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

create table if not exists gastos_periodicos (
    id text primary key,
    vehiculo_id text not null references vehiculos(id) on delete cascade,
    concepto text not null,
    importe double precision not null,
    fecha bigint not null,
    periodicidad text,
    fecha_vencimiento bigint,
    pagado boolean not null default false,
    fecha_pago bigint,
    notas text,
    actualizado_en bigint not null,
    created_at timestamptz default now()
);

alter table gastos_periodicos add column if not exists pagado boolean not null default false;
alter table gastos_periodicos add column if not exists fecha_pago bigint;

create index if not exists idx_gastos_periodicos_vehiculo_id on gastos_periodicos(vehiculo_id);
alter table gastos_periodicos enable row level security;

drop policy if exists "los usuarios ven sus gastos periodicos" on gastos_periodicos;
create policy "los usuarios ven sus gastos periodicos"
    on gastos_periodicos for select
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = gastos_periodicos.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios crean sus gastos periodicos" on gastos_periodicos;
create policy "los usuarios crean sus gastos periodicos"
    on gastos_periodicos for insert
    with check (
        exists (
            select 1
            from vehiculos v
            where v.id = gastos_periodicos.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios actualizan sus gastos periodicos" on gastos_periodicos;
create policy "los usuarios actualizan sus gastos periodicos"
    on gastos_periodicos for update
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = gastos_periodicos.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    )
    with check (
        exists (
            select 1
            from vehiculos v
            where v.id = gastos_periodicos.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios eliminan sus gastos periodicos" on gastos_periodicos;
create policy "los usuarios eliminan sus gastos periodicos"
    on gastos_periodicos for delete
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = gastos_periodicos.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

create table if not exists recordatorios (
    id text primary key,
    vehiculo_id text not null references vehiculos(id) on delete cascade,
    concepto text not null,
    fecha_limite bigint,
    kilometraje_limite double precision,
    completado boolean not null default false,
    fecha_completado bigint,
    notas text,
    actualizado_en bigint not null,
    created_at timestamptz default now()
);

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
