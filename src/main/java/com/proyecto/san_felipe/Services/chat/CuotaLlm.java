package com.proyecto.san_felipe.Services.chat;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lleva la cuenta de la cuota del proveedor para no lanzar peticiones condenadas al 429.
 *
 * Los proveedores limitan los tokens por minuto y una sola consulta del asistente puede
 * encadenar varias rondas de herramientas. Las respuestas traen en las cabeceras cuanta
 * cuota queda y cuando se repone; aqui se guarda ese estado y se espera antes de enviar
 * cuando la siguiente ronda no cabria. Si el proveedor no envia esas cabeceras, no se
 * espera nunca y el 429 se maneja igualmente al reintentar.
 */
@Component
public class CuotaLlm {

    /** Duraciones tipo "6.19s" o "1m30s" que devuelven los proveedores. */
    private static final Pattern DURACION =
            Pattern.compile("(?:(\\d+)m)?(?:([0-9.]+)s)?");

    private static final Pattern ESPERA_EN_ERROR = Pattern.compile("try again in ([0-9.]+)s");

    /** Margen para no quedarse justo en el limite. */
    private static final long TOKENS_DE_MARGEN = 500;

    private volatile long tokensRestantes = Long.MAX_VALUE;
    private volatile long instanteDeReposicion = 0L;

    /** Guarda el estado de cuota que viene en las cabeceras de cada respuesta. */
    public void registrar(HttpHeaders cabeceras) {
        if (cabeceras == null) {
            return;
        }
        String restantes = cabeceras.getFirst("x-ratelimit-remaining-tokens");
        if (restantes != null) {
            try {
                tokensRestantes = Long.parseLong(restantes.trim());
            } catch (NumberFormatException ignorado) {
                // Si la cabecera no es numerica se sigue sin informacion de cuota.
            }
        }
        long reposicionMs = aMilisegundos(cabeceras.getFirst("x-ratelimit-reset-tokens"));
        if (reposicionMs > 0) {
            instanteDeReposicion = System.currentTimeMillis() + reposicionMs;
        }
    }

    /**
     * Espera si la peticion no cabe en la cuota que queda.
     *
     * @param costeEstimado tokens que se calcula que va a consumir la peticion
     * @param esperaMaximaMs cuanto se acepta esperar antes de rendirse
     */
    public void esperarSiNoCabe(long costeEstimado, long esperaMaximaMs) {
        if (tokensRestantes >= costeEstimado + TOKENS_DE_MARGEN) {
            return;
        }
        long espera = instanteDeReposicion - System.currentTimeMillis();
        if (espera <= 0) {
            // La ventana ya se repuso; la proxima respuesta traera el dato actualizado.
            tokensRestantes = Long.MAX_VALUE;
            return;
        }
        if (espera > esperaMaximaMs) {
            throw new IllegalStateException(String.format(
                    "El asistente agoto su cuota por este minuto. Vuelve a intentarlo en %d segundos.",
                    Math.round(espera / 1000.0)));
        }
        dormir(espera + 250);
        tokensRestantes = Long.MAX_VALUE;
    }

    /** Descuenta el coste estimado para que las rondas siguientes lo tengan en cuenta. */
    public void descontar(long tokens) {
        if (tokensRestantes != Long.MAX_VALUE) {
            tokensRestantes = Math.max(0, tokensRestantes - tokens);
        }
    }

    /** Espera lo que pida un 429 concreto. Devuelve false si la espera es demasiado larga. */
    public boolean esperarTrasRechazo(String cuerpoDelError, long esperaMaximaMs) {
        Matcher m = ESPERA_EN_ERROR.matcher(cuerpoDelError == null ? "" : cuerpoDelError);
        if (!m.find()) {
            return false;
        }
        long espera;
        try {
            espera = Math.round(Double.parseDouble(m.group(1)) * 1000) + 250;
        } catch (NumberFormatException e) {
            return false;
        }
        if (espera > esperaMaximaMs) {
            return false;
        }
        dormir(espera);
        tokensRestantes = Long.MAX_VALUE;
        return true;
    }

    /** Estima los tokens de un texto. Para espaniol, unos 4 caracteres por token. */
    public static long estimarTokens(int caracteres) {
        return caracteres / 4L;
    }

    private long aMilisegundos(String duracion) {
        if (duracion == null || duracion.isBlank()) {
            return 0;
        }
        Matcher m = DURACION.matcher(duracion.trim());
        if (!m.matches()) {
            return 0;
        }
        long total = 0;
        if (m.group(1) != null) {
            total += Long.parseLong(m.group(1)) * 60_000;
        }
        if (m.group(2) != null) {
            total += Math.round(Double.parseDouble(m.group(2)) * 1000);
        }
        return total;
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta interrumpida mientras se esperaba la cuota del proveedor.");
        }
    }
}
