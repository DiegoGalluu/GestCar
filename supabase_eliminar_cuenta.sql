create or replace function public.eliminar_cuenta_actual()
returns void
language plpgsql
security definer
set search_path = public, auth, storage
as $$
declare
    usuario_actual uuid := auth.uid();
begin
    if usuario_actual is null then
        raise exception 'no hay usuario autenticado';
    end if;

    delete from storage.objects
    where bucket_id = 'vehiculos'
      and (
          owner::text = usuario_actual::text
          or name like usuario_actual::text || '/%'
      );

    delete from public.vehiculos
    where usuario_id = usuario_actual;

    delete from auth.users
    where id = usuario_actual;
end;
$$;

revoke all on function public.eliminar_cuenta_actual() from public;
grant execute on function public.eliminar_cuenta_actual() to authenticated;
