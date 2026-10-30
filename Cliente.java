import java.io.*;
import java.net.*;
import java.util.*;

/**
 * ============================================================
 *  CLIENTE DE BATALLA NAVAL (se ejecuta en la terminal)
 * ============================================================
 *
 * Cada jugador ejecuta este programa en su computadora. El cliente se
 * conecta al SERVIDOR (por su direccion IP y puerto) y desde ahi se
 * comunica con el rival: nunca habla directamente con la otra PC.
 *
 * Que hace el cliente:
 *   - Se conecta al servidor y recibe la ubicacion de tu flota.
 *   - Muestra dos tableros: TU FLOTA y el RADAR del rival.
 *   - En tu turno te pide una coordenada (ej: B5) y se la envia al servidor.
 *   - Muestra los resultados que informa el servidor.
 *
 * Simbolos de los tableros:
 *   #  barco propio     X  tocado     o  agua     ~  casilla sin explorar
 *
 * USO:  java Cliente [ip_del_servidor] [puerto]
 *       (si no se indican, el programa las pregunta)
 */
public class Cliente {

    static final int N = 8;                       // lado del tablero (igual que en el servidor)
    static final int PUERTO_POR_DEFECTO = 5000;

    // Tablero propio y radar del rival, guardados como arreglos de N*N caracteres.
    static final char[] miFlota = new char[N * N];
    static final char[] radar = new char[N * N];

    public static void main(String[] args) {
        Scanner teclado = new Scanner(System.in);
        Arrays.fill(miFlota, '~');
        Arrays.fill(radar, '~');

        // 1) Datos de conexion: por argumentos o preguntando al usuario.
        String ip = args.length > 0 ? args[0] : preguntar(teclado, "IP del servidor", "127.0.0.1");
        int puerto;
        try {
            puerto = args.length > 1
                    ? Integer.parseInt(args[1])
                    : Integer.parseInt(preguntar(teclado, "Puerto", String.valueOf(PUERTO_POR_DEFECTO)));
        } catch (NumberFormatException e) {
            System.out.println("El puerto debe ser un numero.");
            return;
        }

        // 2) Conexion con el servidor (con un limite de 5 segundos de espera).
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, puerto), 5000);
            System.out.println("Conectado al servidor " + ip + ":" + puerto);

            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);

            jugar(entrada, salida, teclado);
        } catch (IOException e) {
            System.out.println("Error de conexion con " + ip + ":" + puerto + " -> " + e.getMessage());
            System.out.println("Verifica la IP, que el servidor este iniciado y que el firewall permita el puerto.");
        }
    }

    /**
     * Bucle principal: lee los mensajes del servidor y reacciona a cada uno.
     * Ver la lista de mensajes en la documentacion de Servidor.java.
     */
    static void jugar(BufferedReader entrada, PrintWriter salida, Scanner teclado) throws IOException {
        boolean mostrarTablero = true;   // evita repetir los tableros si hay que volver a disparar
        String linea;

        while ((linea = entrada.readLine()) != null) {
            String[] t = linea.split(" ");   // t[0] = tipo de mensaje

            switch (t[0]) {
                case "BIENVENIDO":
                    System.out.println("Sos el jugador " + t[1] + ".");
                    break;

                case "ESPERANDO":
                    System.out.println("Esperando a que se conecte el rival...");
                    break;

                case "FLOTA": {
                    // Marca en nuestro tablero las casillas donde el servidor puso los barcos.
                    for (String celda : t[1].split(",")) {
                        miFlota[Integer.parseInt(celda)] = '#';
                    }
                    break;
                }

                case "INICIO":
                    System.out.println("\nRival conectado. Comienza la batalla!");
                    break;

                case "TU_TURNO": {
                    if (mostrarTablero) {
                        mostrarTableros();
                    }
                    mostrarTablero = true;
                    int casilla = pedirDisparo(teclado);
                    if (casilla < 0) {   // no hay mas entrada de teclado
                        System.out.println("Entrada cerrada. Saliendo.");
                        return;
                    }
                    salida.println("DISPARO " + casilla);   // se envia el disparo al servidor
                    break;
                }

                case "TURNO_RIVAL":
                    System.out.println("\nTurno del rival, espera...");
                    break;

                case "RESULTADO": {
                    // Resultado de NUESTRO disparo: actualizamos el radar.
                    int casilla = Integer.parseInt(t[1]);
                    radar[casilla] = t[2].equals("AGUA") ? 'o' : 'X';
                    System.out.println("Disparaste a " + nombreCasilla(casilla) + ": " + describir(t));
                    break;
                }

                case "DISPARO_RIVAL": {
                    // El rival disparo a NUESTRO tablero: lo actualizamos.
                    int casilla = Integer.parseInt(t[1]);
                    miFlota[casilla] = t[2].equals("AGUA") ? 'o' : 'X';
                    System.out.println("El rival disparo a " + nombreCasilla(casilla) + ": " + describir(t));
                    break;
                }

                case "ERROR":
                    System.out.println("Error: " + linea.substring(Math.min(6, linea.length())));
                    mostrarTablero = false;   // solo se vuelve a pedir la coordenada
                    break;

                case "FIN":
                    mostrarTableros();
                    if (t[1].equals("GANASTE")) {
                        System.out.println("*** GANASTE! Hundiste toda la flota enemiga. ***");
                    } else if (t[1].equals("PERDISTE")) {
                        System.out.println("*** PERDISTE. El rival hundio toda tu flota. ***");
                    } else {
                        System.out.println("El rival se desconecto. Fin de la partida.");
                    }
                    return;

                default:
                    // Mensaje desconocido: se ignora para no romper el juego.
                    break;
            }
        }
        System.out.println("Se perdio la conexion con el servidor.");
    }

    /** Traduce el resultado del servidor (AGUA / TOCADO / HUNDIDO n) a un texto legible. */
    static String describir(String[] t) {
        switch (t[2]) {
            case "AGUA":
                return "agua.";
            case "TOCADO":
                return "TOCADO!";
            default:   // HUNDIDO <tamanio>
                return "HUNDIDO! (barco de " + t[3] + " casillas)";
        }
    }

    /** Pide una coordenada por teclado hasta que sea valida. Devuelve -1 si se cierra la entrada. */
    static int pedirDisparo(Scanner teclado) {
        while (true) {
            System.out.print("Tu disparo (ej: B5): ");
            if (!teclado.hasNextLine()) {
                return -1;
            }
            int casilla = parsearCoordenada(teclado.nextLine());
            if (casilla >= 0) {
                return casilla;
            }
            System.out.println("Coordenada invalida. Usa una letra A-H y un numero 1-8 (ej: C4).");
        }
    }

    /** Convierte un texto tipo "B5" en el numero de casilla (fila * N + columna); -1 si es invalido. */
    static int parsearCoordenada(String texto) {
        texto = texto.trim().toUpperCase();
        if (texto.length() != 2) {
            return -1;
        }
        char letra = texto.charAt(0);
        char numero = texto.charAt(1);
        if (letra < 'A' || letra >= 'A' + N || numero < '1' || numero >= '1' + N) {
            return -1;
        }
        return (numero - '1') * N + (letra - 'A');
    }

    /** Convierte un numero de casilla (0-63) a texto tipo "B5". */
    static String nombreCasilla(int casilla) {
        return "" + (char) ('A' + casilla % N) + (casilla / N + 1);
    }

    /** Dibuja los dos tableros lado a lado: tu flota (izquierda) y el radar (derecha). */
    static void mostrarTableros() {
        StringBuilder sb = new StringBuilder("\n");
        sb.append("        TU FLOTA                   RADAR RIVAL\n");
        sb.append("   ").append(encabezadoColumnas()).append("      ").append(encabezadoColumnas()).append("\n");
        for (int fila = 0; fila < N; fila++) {
            sb.append(fila + 1).append("  ").append(filaComoTexto(miFlota, fila));
            sb.append("      ");
            sb.append(fila + 1).append("  ").append(filaComoTexto(radar, fila)).append("\n");
        }
        sb.append("\n#=barco  X=tocado  o=agua  ~=sin explorar\n");
        System.out.println(sb);
    }

    /** Devuelve "A B C D E F G H". */
    static String encabezadoColumnas() {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < N; c++) {
            sb.append((char) ('A' + c)).append(c < N - 1 ? " " : "");
        }
        return sb.toString() + " ";
    }

    /** Devuelve una fila del tablero con las casillas separadas por espacios. */
    static String filaComoTexto(char[] tablero, int fila) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < N; c++) {
            sb.append(tablero[fila * N + c]).append(c < N - 1 ? " " : "");
        }
        return sb.toString() + " ";
    }

    /** Muestra una pregunta y devuelve lo que escribe el usuario (o el valor por defecto). */
    static String preguntar(Scanner teclado, String texto, String porDefecto) {
        System.out.print(texto + " [" + porDefecto + "]: ");
        if (!teclado.hasNextLine()) {
            return porDefecto;
        }
        String respuesta = teclado.nextLine().trim();
        return respuesta.isEmpty() ? porDefecto : respuesta;
    }
}
