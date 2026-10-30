# Batalla Naval en red (Java)

Juego de dos jugadores para la terminal. Las dos computadoras **no se conectan entre sí**:
ambas se conectan por IP a un **servidor intermedio** que maneja la comunicación, valida los
disparos y decide quién gana.

```
 PC jugador 1  ---\
                   >---  Servidor (IP:5000)  ---<   
 PC jugador 2  ---/
```

## Archivos

| Archivo | Qué hace |
|---|---|
| `Servidor.java` | Espera a 2 jugadores, coloca las flotas al azar, administra turnos y resultados |
| `Cliente.java`  | Se conecta por IP, muestra los tableros y envía los disparos |

## Cómo ejecutarlo (terminal de VS Code)

Requiere tener el JDK instalado (`javac -version` para comprobarlo).

**1. Compilar** (en la carpeta de los archivos):

```
javac -encoding UTF-8 Servidor.java Cliente.java
```

**2. Iniciar el servidor** (en una de las PC, o en una tercera):

```
java Servidor
```

El servidor muestra las direcciones IP en las que se puede conectar. Si hace falta, también
se ve con `ipconfig` (Windows) o `ip a` / `ifconfig` (Linux/Mac).

**3. Iniciar cada cliente** (una terminal en cada PC):

```
java Cliente <ip_del_servidor> 5000
```

Ejemplo: `java Cliente 192.168.1.20 5000`. Para probar en una sola PC, usar `127.0.0.1`
y abrir dos terminales.

> Las dos PC deben estar en la misma red. Si no conecta, revisar que el firewall del equipo
> del servidor permita el puerto 5000 (o usar otro: `java Servidor 6000`).

## Cómo se juega

- Cada jugador recibe una flota de 5 barcos (4, 3, 3, 2 y 2 casillas) en un tablero de 8x8.
- En tu turno escribí una coordenada: letra de columna (A-H) + número de fila (1-8), por ejemplo `B5`.
- Se juega por turnos: el servidor responde **agua**, **tocado** o **hundido**.
- Gana quien hunde primero toda la flota rival.

Símbolos: `#` barco propio · `X` tocado · `o` agua · `~` sin explorar.

## Protocolo de comunicación

Mensajes de texto, una línea por mensaje, sobre TCP.

| Servidor → Cliente | Significado |
|---|---|
| `BIENVENIDO <1\|2>` | número de jugador asignado |
| `ESPERANDO` | falta que se conecte el rival |
| `FLOTA <casillas>` | ubicación de tus barcos |
| `INICIO` | empieza la partida |
| `TU_TURNO` / `TURNO_RIVAL` | de quién es el turno |
| `RESULTADO <casilla> <res>` | resultado de tu disparo |
| `DISPARO_RIVAL <casilla> <res>` | el rival disparó a tu tablero |
| `ERROR <texto>` | disparo inválido (fuera de rango o repetido) |
| `FIN <GANASTE\|PERDISTE\|RIVAL_DESCONECTO>` | termina la partida |

| Cliente → Servidor | Significado |
|---|---|
| `DISPARO <casilla>` | casilla = fila × 8 + columna (0 a 63) |
