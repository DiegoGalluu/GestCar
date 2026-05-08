alter table recordatorios
add column if not exists periodicidad_tiempo_cantidad integer;

alter table recordatorios
add column if not exists periodicidad_tiempo_unidad text;

alter table recordatorios
add column if not exists periodicidad_kilometros double precision;
