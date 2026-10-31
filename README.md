# SistemaFacturacionWeb

Sistema de facturación con Java 21, Spring Boot 4, Spring Data JPA,
Spring Security, Thymeleaf, Bootstrap y SQL Server.

## Funcionalidades

- Gestión de clientes, proveedores, productos y empleados.
- Administración de usuarios y tipos de usuario con permisos por rol.
- Inicio y cierre de sesión; contraseñas Argon2id y protección CSRF.
- Facturas con múltiples productos, cantidades decimales e IVA histórico.
- Reportes de ventas, productos, empleados y clientes; PDF, Excel e impresión.
- Comandos locales protegidos para creación inicial y recuperación administrativa.

## Requisitos

- Java 21 y Maven o el wrapper incluido.
- SQL Server y una base existente llamada `SistemaFacturacionWeb` con el esquema
  del proyecto. No se publican datos reales ni respaldos de la base.
- Para autenticación integrada en Windows, el DLL nativo correspondiente al
  controlador Microsoft JDBC, configurado mediante `java.library.path`.

Hibernate mantiene `spring.jpa.hibernate.ddl-auto=none`: la aplicación no crea
ni modifica automáticamente las tablas.

## Desarrollo local

En la computadora configurada, el arranque cotidiano requiere únicamente:

```powershell
.\mvnw.cmd spring-boot:run
```

Abre `http://localhost:8081/login`. La preparación única de Java 21 y el DLL
utiliza archivos locales excluidos de Git; ver [ARRANQUE-LOCAL.md](ARRANQUE-LOCAL.md).
Los comandos siguientes documentan la configuración manual alternativa.

Configurar las variables en la misma terminal PowerShell que inicia Java:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:DB_SERVER = '127.0.0.1'
$env:DB_PORT = '1433'
$env:DB_NAME = 'SistemaFacturacionWeb'
$env:DB_TRUST_SERVER_CERTIFICATE = 'true'
```

Adaptar las rutas de Java y del DLL al equipo. Las pruebas de integración usan
consultas de lectura a SQL Server; las escrituras de las pruebas son simuladas.

```powershell
.\mvnw.cmd package '-Dspring.profiles.active=sqlserver' '-Dspring.profiles.include=local' '-DargLine=-Djava.library.path=C:/mssql-jdbc-auth/13.4/x64'

& "$env:JAVA_HOME/bin/java.exe" '-Djava.library.path=C:/mssql-jdbc-auth/13.4/x64' -jar target/sistemaweb-0.0.1-SNAPSHOT.jar '--spring.profiles.active=sqlserver,local' '--server.port=8081'
```

Abrir `http://localhost:8081/login`. No existen credenciales predeterminadas ni
registro público de administradores. Consultar la guía de seguridad para crear
la primera cuenta mediante una terminal local con entrada oculta.

La excepción de certificado requiere el perfil `local` y un servidor loopback.
En producción conservar `encrypt=true`, la validación del certificado y una CA
de confianza; no activar el perfil local.

## Documentación

- [Rediseño completo y listado de todas las vistas](REDISENO-COMPLETO.md)

- [Seguridad y primera cuenta administradora](SEGURIDAD-USUARIOS.md)
- [Recuperación local del administrador](RECUPERACION-ADMIN.md)
- [SQL Server y certificados TLS](SQLSERVER-LOCAL.md)
- [Facturación](FACTURAS.md)
- [Reportes y exportaciones](REPORTES.md)

Los JAR, logs, archivos de entorno, respaldos SQL y configuraciones locales del
IDE se excluyen del repositorio. Las rutas a JAR separados mencionadas en las
guías corresponden a empaquetados locales y no se incluyen en Git.
