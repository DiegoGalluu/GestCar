import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const cabecerasCors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

type VehiculoCuenta = {
  id: string;
  imagen_uri: string | null;
};

type DocumentoCuenta = {
  id: string;
};

type AdjuntoCuenta = {
  ruta_storage: string | null;
};

serve(async (request) => {
  if (request.method === "OPTIONS") {
    return respuestaJson({ ok: true }, 200);
  }

  if (request.method !== "POST") {
    return respuestaJson({ error: "metodo no permitido" }, 405);
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
    const serviceRoleKey = Deno.env.get("SERVICE_ROLE_KEY");
    const authorization = request.headers.get("Authorization");

    if (!supabaseUrl || !anonKey || !serviceRoleKey) {
      return respuestaJson(
        { error: "faltan variables de entorno en la edge function" },
        500,
      );
    }

    if (!authorization) {
      return respuestaJson({ error: "falta cabecera authorization" }, 401);
    }

    // validamos el jwt del usuario con el cliente anonimo
    // asi nadie puede pedir borrar una cuenta que no es la suya
    const clienteUsuario = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authorization } },
      auth: { persistSession: false, autoRefreshToken: false },
    });

    const { data: datosUsuario, error: errorUsuario } =
      await clienteUsuario.auth.getUser();
    const usuario = datosUsuario.user;

    if (errorUsuario || !usuario) {
      return respuestaJson(
        { error: "sesion no valida", detalle: errorUsuario?.message },
        401,
      );
    }

    // a partir de aqui usamos service role solo dentro del servidor
    // esta clave nunca viaja a la app ni queda dentro del apk
    const clienteAdmin = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    const usuarioId = usuario.id;

    const { data: vehiculos, error: errorVehiculos } = await clienteAdmin
      .from("vehiculos")
      .select("id, imagen_uri")
      .eq("usuario_id", usuarioId);

    if (errorVehiculos) {
      throw errorVehiculos;
    }

    const vehiculosCuenta = (vehiculos ?? []) as VehiculoCuenta[];
    const vehiculoIds = vehiculosCuenta.map((vehiculo) => vehiculo.id);

    const documentosCuenta = await obtenerDocumentosCuenta(
      clienteAdmin,
      vehiculoIds,
    );
    const documentoIds = documentosCuenta.map((documento) => documento.id);
    const adjuntosCuenta = await obtenerAdjuntosCuenta(
      clienteAdmin,
      documentoIds,
    );

    await borrarStorageCuenta(
      clienteAdmin,
      usuarioId,
      vehiculosCuenta,
      adjuntosCuenta,
    );
    await borrarDatosCuenta(clienteAdmin, usuarioId, vehiculoIds, documentoIds);

    const { error: errorBorradoUsuario } =
      await clienteAdmin.auth.admin.deleteUser(usuarioId);

    if (errorBorradoUsuario) {
      throw errorBorradoUsuario;
    }

    return respuestaJson({ ok: true }, 200);
  } catch (error) {
    return respuestaJson(
      {
        error: "no se ha podido eliminar la cuenta",
        detalle: error instanceof Error ? error.message : String(error),
      },
      500,
    );
  }
});

async function obtenerDocumentosCuenta(
  clienteAdmin: ReturnType<typeof createClient>,
  vehiculoIds: string[],
): Promise<DocumentoCuenta[]> {
  if (vehiculoIds.length === 0) {
    return [];
  }

  const { data, error } = await clienteAdmin
    .from("documentos_vehiculo")
    .select("id")
    .in("vehiculo_id", vehiculoIds);

  if (error) {
    throw error;
  }

  return (data ?? []) as DocumentoCuenta[];
}

async function obtenerAdjuntosCuenta(
  clienteAdmin: ReturnType<typeof createClient>,
  documentoIds: string[],
): Promise<AdjuntoCuenta[]> {
  if (documentoIds.length === 0) {
    return [];
  }

  const { data, error } = await clienteAdmin
    .from("adjuntos_documento")
    .select("ruta_storage")
    .in("documento_id", documentoIds);

  if (error) {
    throw error;
  }

  return (data ?? []) as AdjuntoCuenta[];
}

async function borrarStorageCuenta(
  clienteAdmin: ReturnType<typeof createClient>,
  usuarioId: string,
  vehiculos: VehiculoCuenta[],
  adjuntos: AdjuntoCuenta[],
) {
  const rutasVehiculos = new Set<string>();
  const rutasPorCarpetaUsuario = await listarRutasBucket(
    clienteAdmin,
    "vehiculos",
    usuarioId,
  );

  rutasPorCarpetaUsuario.forEach((ruta) => rutasVehiculos.add(ruta));
  vehiculos.forEach((vehiculo) => {
    const ruta = limpiarRutaStorage(vehiculo.imagen_uri);
    if (ruta) {
      rutasVehiculos.add(ruta);
    }
  });

  await borrarRutasBucket(clienteAdmin, "vehiculos", Array.from(rutasVehiculos));

  const rutasDocumentos = adjuntos
    .map((adjunto) => limpiarRutaStorage(adjunto.ruta_storage))
    .filter((ruta): ruta is string => ruta !== null);

  await borrarRutasBucket(clienteAdmin, "documentos", rutasDocumentos);
}

async function borrarDatosCuenta(
  clienteAdmin: ReturnType<typeof createClient>,
  usuarioId: string,
  vehiculoIds: string[],
  documentoIds: string[],
) {
  if (documentoIds.length > 0) {
    await borrarPorFiltro(clienteAdmin, "adjuntos_documento", "documento_id", documentoIds);
    await borrarPorFiltro(clienteAdmin, "campos_documento", "documento_id", documentoIds);
    await borrarPorFiltro(clienteAdmin, "documentos_vehiculo", "id", documentoIds);
  }

  if (vehiculoIds.length > 0) {
    await borrarPorFiltro(clienteAdmin, "repostajes", "vehiculo_id", vehiculoIds);
    await borrarPorFiltro(clienteAdmin, "mantenimientos", "vehiculo_id", vehiculoIds);
    await borrarPorFiltro(clienteAdmin, "gastos_periodicos", "vehiculo_id", vehiculoIds);
    await borrarPorFiltro(clienteAdmin, "recordatorios", "vehiculo_id", vehiculoIds);
  }

  const { error } = await clienteAdmin
    .from("vehiculos")
    .delete()
    .eq("usuario_id", usuarioId);

  if (error) {
    throw error;
  }
}

async function borrarPorFiltro(
  clienteAdmin: ReturnType<typeof createClient>,
  tabla: string,
  columna: string,
  valores: string[],
) {
  if (valores.length === 0) {
    return;
  }

  const { error } = await clienteAdmin
    .from(tabla)
    .delete()
    .in(columna, valores);

  if (error) {
    throw error;
  }
}

async function listarRutasBucket(
  clienteAdmin: ReturnType<typeof createClient>,
  bucket: string,
  carpeta: string,
): Promise<string[]> {
  const rutas: string[] = [];

  async function recorrer(prefijo: string) {
    const { data, error } = await clienteAdmin.storage
      .from(bucket)
      .list(prefijo, { limit: 1000 });

    if (error) {
      throw error;
    }

    for (const item of data ?? []) {
      const ruta = prefijo ? `${prefijo}/${item.name}` : item.name;
      const esCarpeta = !item.id && !item.metadata;
      if (esCarpeta) {
        await recorrer(ruta);
      } else {
        rutas.push(ruta);
      }
    }
  }

  await recorrer(carpeta);
  return rutas;
}

async function borrarRutasBucket(
  clienteAdmin: ReturnType<typeof createClient>,
  bucket: string,
  rutas: string[],
) {
  const rutasUnicas = Array.from(new Set(rutas))
    .filter((ruta) => ruta.trim().length > 0);

  for (let indice = 0; indice < rutasUnicas.length; indice += 100) {
    const lote = rutasUnicas.slice(indice, indice + 100);
    const { error } = await clienteAdmin.storage.from(bucket).remove(lote);

    if (error) {
      throw error;
    }
  }
}

function limpiarRutaStorage(valor: string | null): string | null {
  const ruta = valor?.trim();
  if (!ruta) {
    return null;
  }

  // ignoramos rutas locales o urls firmadas
  // solo borramos paths internos del bucket
  if (
    ruta.startsWith("file:") ||
    ruta.startsWith("content:") ||
    ruta.startsWith("http://") ||
    ruta.startsWith("https://")
  ) {
    return null;
  }

  return ruta;
}

function respuestaJson(cuerpo: unknown, estado: number): Response {
  return new Response(JSON.stringify(cuerpo), {
    status: estado,
    headers: {
      ...cabecerasCors,
      "Content-Type": "application/json; charset=utf-8",
    },
  });
}
