# ⚓ Batalla Naval en Red (Java)

##  Presentación del proyecto

Este proyecto consiste en el desarrollo de un videojuego de **Batalla Naval para dos jugadores**, programado en Java y diseñado para funcionar mediante una conexión de red.

El objetivo principal es que dos jugadores puedan enfrentarse desde computadoras diferentes, comunicándose a través de un servidor intermediario. Los clientes no se conectan directamente entre sí, sino que ambos establecen una conexión con el servidor, que se encarga de administrar la partida, gestionar los turnos, validar los disparos y determinar al ganador.

El proyecto permite poner en práctica conceptos de **programación sobre redes**, comunicación entre equipos, conexiones TCP/IP y desarrollo de aplicaciones cliente-servidor.

##  Archivos del proyecto

El proyecto está compuesto principalmente por dos archivos Java:

- **`Servidor.java`**: espera la conexión de dos jugadores, genera aleatoriamente las flotas, administra los turnos, recibe y valida los disparos, comunica los resultados y determina cuándo finaliza la partida.
- **`Cliente.java`**: se conecta al servidor mediante una dirección IP, muestra los tableros de juego y permite enviar disparos al servidor.

##  Cómo ejecutar el juego

Para ejecutar el proyecto es necesario tener instalado el **JDK de Java**.

### 1. Comprobar la instalación de Java

Abrir una terminal y ejecutar:

```bash
javac -version
```

### 2. Compilar los archivos

Ubicarse en la carpeta donde se encuentran `Servidor.java` y `Cliente.java` y ejecutar:

```bash
javac -encoding UTF-8 Servidor.java Cliente.java
```

### 3. Iniciar el servidor

Ejecutar el siguiente comando:

```bash
java Servidor
```

El servidor quedará esperando la conexión de los dos jugadores.

Para conocer la dirección IP de la computadora que ejecuta el servidor se puede utilizar:

- **Windows:** `ipconfig`
- **Linux:** `ip a` o `ifconfig`
- **Mac:** `ifconfig`

### 4. Conectar los clientes

Desde cada computadora que participa en la partida, ejecutar:

```bash
java Cliente <ip_del_servidor> 5000
```

Por ejemplo:

```bash
java Cliente 192.168.1.20 5000
```

Ambos jugadores deben conectarse a la misma dirección IP del servidor.

**Importante:**
- Las dos computadoras deben estar conectadas a la misma red.
- El firewall debe permitir las conexiones por el puerto `5000`.
- Si se desea utilizar otro puerto, se puede iniciar el servidor con:

```bash
java Servidor 6000
```

En ese caso, los clientes también deberán conectarse utilizando el puerto `6000`.

Si se quiere probar el juego desde una sola computadora, se pueden abrir dos terminales y ejecutar dos clientes utilizando la dirección `127.0.0.1`.

##  Cómo se juega?

La partida se desarrolla entre dos jugadores, cada uno con su propio tablero de **8 x 8 casillas**.

Cada jugador recibe una flota de cinco barcos, distribuidos aleatoriamente:

- 1 barco de 4 casillas.
- 2 barcos de 3 casillas.
- 2 barcos de 2 casillas.

### Reglas del juego

1. Cada jugador dispone de un tablero con sus barcos.
2. Los jugadores se turnan para realizar disparos.
3. Para disparar se debe ingresar una coordenada formada por una columna (A-H) y una fila (1-8).
4. El servidor recibe el disparo y comprueba si la coordenada es válida.
5. El resultado del disparo puede ser agua, tocado o hundido.
6. No se permite disparar dos veces sobre una misma casilla.
7. Gana el jugador que logra hundir todos los barcos del rival.

##  Símbolos del tablero

| Símbolo | Significado |
|---|---|
| `#` | Barco propio |
| `X` | Disparo acertado |
| `o` | Agua |
| `~` | Casilla sin explorar |

##  Protocolo de comunicación

La comunicación entre el servidor y los clientes se realiza mediante **TCP**, utilizando mensajes de texto intercambiados entre ambos.

### Mensajes del servidor al cliente

| Mensaje | Descripción |
|---|---|
| `BIENVENIDO <1\|2>` | Indica el número de jugador asignado. |
| `ESPERANDO` | Informa que se está esperando al segundo jugador. |
| `FLOTA <casillas>` | Envía la información de la flota. |
| `INICIO` | Indica que comenzó la partida. |
| `TU_TURNO` | Indica que es el turno del jugador. |
| `TURNO_RIVAL` | Indica que debe jugar el rival. |
| `RESULTADO <casilla> <res>` | Informa el resultado del disparo propio. |
| `DISPARO_RIVAL <casilla> <res>` | Informa el disparo realizado por el rival. |
| `ERROR <texto>` | Informa un error, como una coordenada fuera de rango o un disparo repetido. |
| `FIN <GANASTE\|PERDISTE\|RIVAL_DESCONECTO>` | Informa el resultado final de la partida. |

### Mensajes del cliente al servidor

El cliente envía el siguiente mensaje para realizar un disparo:

```text
DISPARO <casilla>
```

La casilla se representa mediante un número entre `0` y `63`, calculado de la siguiente manera:

```text
casilla = fila × 8 + columna
```

##  Objetivo del proyecto

El objetivo de este trabajo es desarrollar un videojuego funcional que permita comprender y aplicar los fundamentos de la programación sobre redes mediante una arquitectura cliente-servidor.

A través de su desarrollo se trabajan conceptos como:

- Comunicación entre computadoras.
- Conexiones mediante TCP/IP.
- Arquitectura cliente-servidor.
- Intercambio de mensajes.
- Administración de turnos.
- Validación de datos.
- Programación en Java.

---

**Tecnologías utilizadas:** Java · TCP/IP · Sockets · Programación sobre Redes

**Modalidad:** Multijugador · Dos jugadores · Cliente-servidor
