-- la eliminacion de cuenta ya no se hace con una funcion sql
-- supabase no permite borrar objetos de storage tocando storage.objects directamente
-- ahora se usa la edge function eliminar-cuenta y la storage api oficial

drop function if exists public.eliminar_cuenta_actual();
