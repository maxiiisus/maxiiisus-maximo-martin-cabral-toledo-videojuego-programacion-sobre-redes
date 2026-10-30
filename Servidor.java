import java.io.*;
import java.net.*;
import java.util.*;

/**
 * ============================================================
 *  SERVIDOR INTERMEDIO DE BATALLA NAVAL
 * ============================================================
 *
 * Este programa es el "intermediario" entre las dos computadoras:
 * los jugadores NUNCA se conectan entre si, sino que ambos se
 * conectan a este servidor usando su direccion IP y un puerto.
 *
 * El servidor se encarga de:
 *   1. Esperar a que se conecten dos clientes (jugadores).
 *   2. Colocar la flota de cada jugador al azar (asi nadie puede hacer trampa
 *      modificando su propio cliente).
 *   3. Administrar los turnos, validar cada disparo y decidir si fue
 *      AGUA, TOCADO o HUNDIDO.
 *   4. Avisar a ambos jugadores el resultado y detectar al ganador.
 *
 * PROTOCOLO (mensajes de texto, una linea por mensaje):
 *   Servidor -> Cliente:
 *     BIENVENIDO <1|2>               numero de jugador asignado
 *     ESPERANDO                      todavia falta el rival
 *     FLOTA <c1,c2,c3,...>           casillas donde estan tus barcos
 *     INICIO                         comienza la partida
 *     TU_TURNO / TURNO_RIVAL         de quien es el turno
 *     RESULTADO <casilla> <res>      resultado de TU disparo
 *     DISPARO_RIVAL <casilla> <res>  el rival disparo a tu tablero
 *     ERROR <mensaje>               disparo invalido
 *     FIN <GANASTE|PERDISTE|RIVAL_DESCONECTO>
 *   Cliente -> Servidor:
 *     DISPARO <casilla>              casilla = fila * 8 + columna (0 a 63)
 *
 * USO:  java Servidor [puerto]      (puerto por defecto: 5000)
 */
public class Servidor {

    /** Puerto TCP en el que escucha el servidor si no se indica otro. */
    static final int PUERTO_POR_DEFECTO = 5000;

    /** Lado del tablero (8x8 = 64 casillas). */
    static final int N = 8;

    /** Tamanios de los barcos que recibe cada jugador. */
    static final int[] TAMANIOS_BARCOS = {4, 3, 3, 2, 2};

    // ------------------------------------------------------------------
    //  CLASE Barco: guarda las casillas que ocupa y cuantas siguen "vivas"
    // ------------------------------------------------------------------
    static class Barco {
        final int[] celdas;   // casillas que ocupa el barco
        int restantes;        // casillas que todavia no fueron tocadas

        Barco(int[] celdas) {
            this.celdas = celdas;
            this.restantes = celdas.length;
        }
    }

    // ------------------------------------------------------------------
    //  CLASE Jugador: conexion de red + tablero propio del jugador
    // ------------------------------------------------------------------
    static class Jugador {
        final int numero;                         // 1 o 2
        final Socket socket;                      // conexion con el cliente
        final BufferedReader entrada;             // para leer lo que envia el cliente
        final PrintWriter salida;                 // para enviarle mensajes al cliente
        final List<Barco> barcos = new ArrayList<>();
        final int[] barcoEnCelda = new int[N * N];     // indice del barco en cada casilla (-1 = agua)
        final boolean[] recibido = new boolean[N * N]; // casillas que el rival ya disparo
        int celdasVivas = 0;                      // casillas de barco sin tocar

        Jugador(int numero, Socket socket) throws IOException {
            this.numero = numero;
            this.socket = socket;
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // El 'true' activa el autoflush: cada println se envia de inmediato.
            this.salida = new PrintWriter(socket.getOutputStream(), true);
            Arrays.fill(barcoEnCelda, -1);
            colocarFlota();
        }

        /** Coloca todos los barcos al azar, sin superponerse. */
        private void colocarFlota() {
            Random azar = new Random();
            for (int tam : TAMANIOS_BARCOS) {
                boolean colocado = false;
                while (!colocado) {
                    boolean horizontal = azar.nextBoolean();
                    // Se elige un punto de partida que deje al barco dentro del tablero.
                    int fila = azar.nextInt(horizontal ? N : N - tam + 1);
                    int col = azar.nextInt(horizontal ? N - tam + 1 : N);

                    int[] celdas = new int[tam];
                    boolean libre = true;
                    for (int i = 0; i < tam; i++) {
                        int f = horizontal ? fila : fila + i;
                        int c = horizontal ? col + i : col;
                        celdas[i] = f * N + c;
                        if (barcoEnCelda[celdas[i]] != -1) {
                            libre = false;   // ya hay otro barco en esa casilla
                        }
                    }
                    if (libre) {
                        int id = barcos.size();
                        barcos.add(new Barco(celdas));
                        for (int celda : celdas) {
                            barcoEnCelda[celda] = id;
                        }
                        celdasVivas += tam;
                        colocado = true;
                    }
                }
            }
        }

        /** Devuelve la flota como texto "c1,c2,c3,..." para enviarla al cliente. */
        String flotaComoTexto() {
            StringBuilder sb = new StringBuilder();
            for (Barco b : barcos) {
                for (int celda : b.celdas) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(celda);
                }
            }
            return sb.toString();
        }

        /**
         * Aplica un disparo del rival sobre este tablero.
         * @return "AGUA", "TOCADO" o "HUNDIDO <tamanio>"
         */
        String resolverDisparo(int casilla) {
            recibido[casilla] = true;
            int id = barcoEnCelda[casilla];
            if (id == -1) {
                return "AGUA";
            }
            Barco barco = barcos.get(id);
            barco.restantes--;
            celdasVivas--;
            return barco.restantes == 0 ? "HUNDIDO " + barco.celdas.length : "TOCADO";
        }

        void enviar(String mensaje) {
            salida.println(mensaje);
        }

        /** Lee una linea del cliente; devuelve null si el cliente se desconecto. */
        String leer() {
            try {
                return entrada.readLine();
            } catch (IOException e) {
                return null;
            }
        }

        void cerrar() {
            try {
                socket.close();
            } catch (IOException e) {
                // Si ya estaba cerrado no hay nada que hacer.
            }
        }
    }

    // ------------------------------------------------------------------
    //  PROGRAMA PRINCIPAL
    // ------------------------------------------------------------------
    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : PUERTO_POR_DEFECTO;

        // ServerSocket = "puerta" por la que entran las conexiones de los clientes.
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("=== Servidor de Batalla Naval iniciado en el puerto " + puerto + " ===");
            System.out.println("Los jugadores deben conectarse a alguna de estas IP:");
            mostrarDireccionesIP();
            System.out.println("(Ctrl+C para detener el servidor)");

            // El servidor atiende partidas una tras otra, indefinidamente.
            while (true) {
                try {
                    jugarPartida(servidor);
                } catch (IOException e) {
                    System.out.println("Error en la partida: " + e.getMessage());
                }
            }
        }
    }

    /** Espera a dos jugadores y administra una partida completa. */
    static void jugarPartida(ServerSocket servidor) throws IOException {
        System.out.println("\n--- Nueva partida: esperando jugadores ---");

        // accept() se bloquea hasta que un cliente se conecta.
        Jugador j1 = new Jugador(1, servidor.accept());
        System.out.println("Jugador 1 conectado desde " + j1.socket.getInetAddress().getHostAddress());
        j1.enviar("BIENVENIDO 1");
        j1.enviar("ESPERANDO");

        Jugador j2 = new Jugador(2, servidor.accept());
        System.out.println("Jugador 2 conectado desde " + j2.socket.getInetAddress().getHostAddress());
        j2.enviar("BIENVENIDO 2");

        try {
            // Cada jugador recibe la ubicacion de SU flota (la del rival queda secreta).
            j1.enviar("FLOTA " + j1.flotaComoTexto());
            j2.enviar("FLOTA " + j2.flotaComoTexto());
            j1.enviar("INICIO");
            j2.enviar("INICIO");
            System.out.println("Comienza la batalla.");

            Jugador[] jugadores = {j1, j2};
            int turno = 0;   // indice del jugador que dispara ahora

            while (true) {
                Jugador atacante = jugadores[turno];
                Jugador defensor = jugadores[1 - turno];

                defensor.enviar("TURNO_RIVAL");
                int casilla = pedirDisparo(atacante, defensor);

                if (casilla < 0) {   // el atacante se desconecto
                    System.out.println("El jugador " + atacante.numero + " se desconecto.");
                    defensor.enviar("FIN RIVAL_DESCONECTO");
                    break;
                }

                String resultado = defensor.resolverDisparo(casilla);
                System.out.println("Jugador " + atacante.numero + " dispara a "
                        + nombreCasilla(casilla) + " -> " + resultado);

                // Se informa a ambos: al atacante su resultado y al defensor el ataque recibido.
                atacante.enviar("RESULTADO " + casilla + " " + resultado);
                defensor.enviar("DISPARO_RIVAL " + casilla + " " + resultado);

                if (defensor.celdasVivas == 0) {   // no quedan barcos: termina la partida
                    atacante.enviar("FIN GANASTE");
                    defensor.enviar("FIN PERDISTE");
                    System.out.println("Gana el jugador " + atacante.numero + ".");
                    break;
                }

                turno = 1 - turno;   // cambia el turno
            }
        } finally {
            // Se cierran las conexiones para poder atender una partida nueva.
            j1.cerrar();
            j2.cerrar();
        }
    }

    /**
     * Le pide un disparo al atacante hasta recibir uno valido.
     * @return la casilla (0-63) o -1 si el atacante se desconecto.
     */
    static int pedirDisparo(Jugador atacante, Jugador defensor) {
        while (true) {
            atacante.enviar("TU_TURNO");
            String mensaje = atacante.leer();
            if (mensaje == null) {
                return -1;
            }

            // Se espera el formato exacto "DISPARO <numero>".
            String[] partes = mensaje.trim().split(" ");
            int casilla = -1;
            if (partes.length == 2 && partes[0].equals("DISPARO")) {
                try {
                    casilla = Integer.parseInt(partes[1]);
                } catch (NumberFormatException e) {
                    casilla = -1;
                }
            }

            if (casilla < 0 || casilla >= N * N) {
                atacante.enviar("ERROR Coordenada invalida");
            } else if (defensor.recibido[casilla]) {
                atacante.enviar("ERROR Ya disparaste a esa casilla");
            } else {
                return casilla;   // disparo valido
            }
        }
    }

    /** Convierte una casilla (0-63) a texto tipo "B5" para mostrar en el log. */
    static String nombreCasilla(int casilla) {
        return "" + (char) ('A' + casilla % N) + (casilla / N + 1);
    }

    /** Lista las direcciones IPv4 de esta computadora para que los clientes sepan a cual conectarse. */
    static void mostrarDireccionesIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> direcciones = ni.getInetAddresses();
                while (direcciones.hasMoreElements()) {
                    InetAddress d = direcciones.nextElement();
                    if (d instanceof Inet4Address) {
                        System.out.println("   -> " + d.getHostAddress() + "   (" + ni.getDisplayName() + ")");
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("   No se pudieron listar las IP. Usa ipconfig (Windows) o ifconfig/ip a (Linux/Mac).");
        }
    }
}
