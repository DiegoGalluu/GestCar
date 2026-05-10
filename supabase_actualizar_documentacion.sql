create table if not exists documentos_vehiculo (
    id text primary key,
    vehiculo_id text not null references vehiculos(id) on delete cascade,
    titulo text not null,
    notas text,
    actualizado_en bigint not null,
    created_at timestamptz default now()
);

alter table documentos_vehiculo
add column if not exists vehiculo_id text;

alter table documentos_vehiculo
add column if not exists titulo text;

alter table documentos_vehiculo
add column if not exists notas text;

alter table documentos_vehiculo
add column if not exists actualizado_en bigint;

alter table documentos_vehiculo
add column if not exists created_at timestamptz default now();

create index if not exists idx_documentos_vehiculo_vehiculo_id
on documentos_vehiculo(vehiculo_id);

alter table documentos_vehiculo enable row level security;

drop policy if exists "los usuarios ven sus documentos de vehiculo" on documentos_vehiculo;
create policy "los usuarios ven sus documentos de vehiculo"
    on documentos_vehiculo for select
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = documentos_vehiculo.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios crean sus documentos de vehiculo" on documentos_vehiculo;
create policy "los usuarios crean sus documentos de vehiculo"
    on documentos_vehiculo for insert
    with check (
        exists (
            select 1
            from vehiculos v
            where v.id = documentos_vehiculo.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios actualizan sus documentos de vehiculo" on documentos_vehiculo;
create policy "los usuarios actualizan sus documentos de vehiculo"
    on documentos_vehiculo for update
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = documentos_vehiculo.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    )
    with check (
        exists (
            select 1
            from vehiculos v
            where v.id = documentos_vehiculo.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios eliminan sus documentos de vehiculo" on documentos_vehiculo;
create policy "los usuarios eliminan sus documentos de vehiculo"
    on documentos_vehiculo for delete
    using (
        exists (
            select 1
            from vehiculos v
            where v.id = documentos_vehiculo.vehiculo_id
              and v.usuario_id = auth.uid()
        )
    );

create table if not exists campos_documento (
    id text primary key,
    documento_id text not null references documentos_vehiculo(id) on delete cascade,
    nombre text not null,
    valor text not null,
    orden integer not null default 0,
    actualizado_en bigint not null,
    created_at timestamptz default now()
);

alter table campos_documento
add column if not exists documento_id text;

alter table campos_documento
add column if not exists nombre text;

alter table campos_documento
add column if not exists valor text;

alter table campos_documento
add column if not exists orden integer default 0;

alter table campos_documento
add column if not exists actualizado_en bigint;

alter table campos_documento
add column if not exists created_at timestamptz default now();

create index if not exists idx_campos_documento_documento_id
on campos_documento(documento_id);

alter table campos_documento enable row level security;

drop policy if exists "los usuarios ven sus campos de documento" on campos_documento;
create policy "los usuarios ven sus campos de documento"
    on campos_documento for select
    using (
        exists (
            select 1
            from documentos_vehiculo d
            join vehiculos v on v.id = d.vehiculo_id
            where d.id = campos_documento.documento_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios crean sus campos de documento" on campos_documento;
create policy "los usuarios crean sus campos de documento"
    on campos_documento for insert
    with check (
        exists (
            select 1
            from documentos_vehiculo d
            join vehiculos v on v.id = d.vehiculo_id
            where d.id = campos_documento.documento_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios actualizan sus campos de documento" on campos_documento;
create policy "los usuarios actualizan sus campos de documento"
    on campos_documento for update
    using (
        exists (
            select 1
            from documentos_vehiculo d
            join vehiculos v on v.id = d.vehiculo_id
            where d.id = campos_documento.documento_id
              and v.usuario_id = auth.uid()
        )
    )
    with check (
        exists (
            select 1
            from documentos_vehiculo d
            join vehiculos v on v.id = d.vehiculo_id
            where d.id = campos_documento.documento_id
              and v.usuario_id = auth.uid()
        )
    );

drop policy if exists "los usuarios eliminan sus campos de documento" on campos_documento;
create policy "los usuarios eliminan sus campos de documento"
    on campos_documento for delete
    using (
        exists (
            select 1
            from documentos_vehiculo d
            join vehiculos v on v.id = d.vehiculo_id
            where d.id = campos_documento.documento_id
              and v.usuario_id = auth.uid()
        )
    );
