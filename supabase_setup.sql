-- ejecuta este script en el editor sql de supabase
-- supabase dashboard > sql editor > new query > pega esto y dale a run

-- tabla de vehiculos
create table if not exists vehiculos (
    id text primary key,
    usuario_id uuid not null references auth.users(id) on delete cascade,
    marca text not null,
    modelo text not null,
    anio_fabricacion integer not null,
    mes_fabricacion integer,
    dia_fabricacion integer,
    tipo text not null default 'COCHE',
    matricula text not null,
    kilometraje double precision not null default 0,
    tipo_combustible text,
    imagen_uri text,
    fecha_alta bigint not null,
    notas text,
    actualizado_en bigint not null,
    created_at timestamptz default now()
);

alter table vehiculos add column if not exists anio_fabricacion integer;
alter table vehiculos add column if not exists mes_fabricacion integer;
alter table vehiculos add column if not exists dia_fabricacion integer;

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'vehiculos'
          and column_name = 'anio'
    ) then
        execute 'update vehiculos
                 set anio_fabricacion = coalesce(anio_fabricacion, anio)
                 where anio_fabricacion is null';
    end if;
end $$;

-- activamos row level security para que cada usuario solo vea sus vehiculos
alter table vehiculos enable row level security;

-- politica para que los usuarios solo puedan ver sus propios vehiculos
create policy "los usuarios ven sus vehiculos"
    on vehiculos for select
    using (auth.uid() = usuario_id);

-- politica para que los usuarios solo puedan insertar sus propios vehiculos
create policy "los usuarios crean sus vehiculos"
    on vehiculos for insert
    with check (auth.uid() = usuario_id);

-- politica para que los usuarios solo puedan actualizar sus propios vehiculos
create policy "los usuarios actualizan sus vehiculos"
    on vehiculos for update
    using (auth.uid() = usuario_id);

-- politica para que los usuarios solo puedan eliminar sus propios vehiculos
create policy "los usuarios eliminan sus vehiculos"
    on vehiculos for delete
    using (auth.uid() = usuario_id);

-- bucket publico para las fotos de vehiculos
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'vehiculos',
    'vehiculos',
    true,
    2097152,
    array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do update
set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

-- cada usuario puede subir fotos solo dentro de su carpeta
drop policy if exists "los usuarios suben sus fotos de vehiculos" on storage.objects;
create policy "los usuarios suben sus fotos de vehiculos"
    on storage.objects for insert
    to authenticated
    with check (
        bucket_id = 'vehiculos'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

drop policy if exists "los usuarios actualizan sus fotos de vehiculos" on storage.objects;
create policy "los usuarios actualizan sus fotos de vehiculos"
    on storage.objects for update
    to authenticated
    using (
        bucket_id = 'vehiculos'
        and (storage.foldername(name))[1] = auth.uid()::text
    )
    with check (
        bucket_id = 'vehiculos'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

drop policy if exists "los usuarios eliminan sus fotos de vehiculos" on storage.objects;
create policy "los usuarios eliminan sus fotos de vehiculos"
    on storage.objects for delete
    to authenticated
    using (
        bucket_id = 'vehiculos'
        and (storage.foldername(name))[1] = auth.uid()::text
    );
