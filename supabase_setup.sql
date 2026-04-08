-- ejecuta este script en el editor sql de supabase
-- supabase dashboard > sql editor > new query > pega esto y dale a run

-- tabla de vehiculos
create table if not exists vehiculos (
    id text primary key,
    usuario_id uuid not null references auth.users(id) on delete cascade,
    marca text not null,
    modelo text not null,
    anio integer not null,
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
