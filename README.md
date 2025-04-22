# Caso_3_Infracomp

# 🛫 Sistema de Consulta Segura para Aerolínea

Este proyecto implementa un sistema de consulta de vuelos entre un cliente y un servidor principal, garantizando **comunicación segura** usando técnicas de **criptografía moderna**.

## 🧩 Descripción General

El sistema simula cómo una aerolínea podría permitir a sus usuarios consultar en línea:

- El estado de un vuelo
- La disponibilidad de vuelos para un trayecto
- El costo de un vuelo

Para garantizar la **confidencialidad** y **la integridad** de la información intercambiada, el cliente y el servidor principal se comunican de forma segura mediante **cifrado y firmas digitales**.

## 🔒 Seguridad Implementada

- **Intercambio de llaves Diffie-Hellman** para establecer una llave maestra compartida.
- A partir de la llave maestra:
  - Se genera una **llave de cifrado simétrico AES-256 (modo CBC)**
  - Y una **llave para HMAC (SHA-256)** para validación de integridad.
- **RSA (1024 bits)** y **SHA256withRSA** para firmar mensajes.
- Si el mensaje recibido no pasa la validación con HMAC, se muestra `"Error en la consulta"` y termina el proceso.

## 🔁 Flujo de Comunicación

1. El cliente se conecta al servidor principal.
2. Ambos establecen una llave de sesión segura.
3. El servidor envía la lista de servicios disponibles.
4. El cliente elige un servicio y solicita sus datos.
5. El servidor responde con la IP y puerto del servidor delegado.
6. El cliente finaliza la comunicación (la conexión con el servidor delegado no es parte de este proyecto).

## ⚙️ Implementación

El sistema está hecho en **Java**, utilizando únicamente librerías estándar (`java.security`, `javax.crypto`, `BigInteger`, etc.). Las llaves pública y privada del servidor se generan previamente y se almacenan en archivos separados.

---

Este prototipo es parte de un proyecto académico del curso **ISIS2203 – Infraestructura Computacional**, Universidad de los Andes, 2025-10.
