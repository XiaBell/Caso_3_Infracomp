
# 🛫 Sistema de Consulta Segura para Aerolínea - Proyecto de Comunicación Segura Cliente-Servidor — Caso 3 InfraComp
## 🧾 Descripción

Este proyecto implementa un sistema **cliente-servidor** con **protocolos de seguridad criptográfica** para garantizar la **autenticidad**, **confidencialidad** e **integridad** de las comunicaciones. Está desarrollado en **Java** como parte del curso de **Infraestructura Computacional**.

El sistema permite a un cliente consultar información de vuelos a través de un servidor principal, que delega la atención a uno de varios **servidores delegados**, según el servicio seleccionado.

---

## 🚀 Estructura del protocolo

El protocolo implementado sigue estas 3 fases:

### 🟠 Fase 1: Autenticación
1. El cliente envía un saludo `HELLO`.
2. El servidor responde con un reto aleatorio.
3. El cliente **firma el reto** con su llave privada.
4. El servidor valida la firma con la llave pública del cliente.
5. Si la firma es válida, responde con `OK`.

### 🟡 Fase 2: Intercambio de claves (Diffie-Hellman)
6. El servidor envía los parámetros `p`, `g`, `g^a mod p` **firmados con RSA**.
7. El cliente valida la firma y responde con `g^b mod p`.
8. Ambos generan la clave maestra `K_master` y derivan:
   - `K_AB1`: clave AES (cifrado)
   - `K_AB2`: clave HMAC (integridad)

### 🟢 Fase 3: Comunicación segura
9. El servidor envía la tabla de servicios cifrada (AES) + HMAC.
10. El cliente elige un servicio y lo envía de forma segura.
11. El servidor responde con la dirección del **servidor delegado** correspondiente.
12. El cliente se conecta automáticamente al delegado, realiza una consulta y recibe la respuesta.

---

## 🧱 Componentes

| Clase                   | Descripción |
|------------------------|-------------|
| `ServidorPrincipal`    | Atiende al cliente, verifica identidad y delega |
| `ClienteConsulta`      | Cliente que solicita un servicio y consulta al delegado |
| `ServidorDelegadoRunnable` | Servidores específicos que responden por servicio |
| `TablaServicios`       | Maneja las IPs y puertos por ID de servicio |
| `ProtocoloSeguridad`   | Contiene toda la lógica de criptografía: RSA, AES, HMAC, DH |
| `LanzadorConcurrente`  | Lanza múltiples clientes para pruebas concurrentes |

---

## 🔐 Seguridad implementada

- **Firma Digital**: `SHA256withRSA` para retos y parámetros DH
- **Cifrado Simétrico**: `AES/CBC/PKCS5Padding` para datos
- **Integridad**: `HMAC-SHA256`
- **Intercambio de claves**: `Diffie-Hellman` con SHA-512
- **Cifrado Asimétrico**: utilizado para comparación de rendimiento (no en el protocolo)

---

## 📈 Pruebas y Medición de Rendimiento

### Escenarios evaluados:

1. **Iterativo**: 1 cliente haciendo 32 solicitudes consecutivas
2. **Concurrente**: 4, 16, 32, 64 clientes simultáneos

### Métricas recolectadas:

- Tiempo para **firmar** con RSA
- Tiempo para **cifrar la tabla**
- Tiempo para **verificar la consulta** (HMAC)
- Comparación de tiempos: **cifrado simétrico vs asimétrico**

### Herramientas:

- Se utilizaron `System.nanoTime()` y contadores internos en el servidor.
- Los datos se almacenan en consola para exportar y graficar posteriormente.

---

Este prototipo es parte de un proyecto académico del curso **ISIS2203 – Infraestructura Computacional**, Universidad de los Andes, 2025-10.
