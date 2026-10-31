# Arranque local con un solo comando

En esta computadora ya está preparada la configuración. Desde la raíz del
proyecto, en la terminal PowerShell de VS Code, ejecutar únicamente:

```powershell
.\mvnw.cmd spring-boot:run
```

Abrir `http://localhost:8081/login`. Detener la aplicación con Ctrl+C.
El comando Maven válido es `spring-boot:run`: `spring-boot` solo no es una meta
y `.\mvnw\.cmd` no es la ruta del wrapper.

## Cómo funciona

- `.mvn/local-java.cmd` selecciona Java 21 para el proceso del wrapper, sin
  cambiar JAVA_HOME ni PATH globales de Windows. Esto permite arrancar aunque
  el comando `java` del sistema todavía apunte a Java 8.
- `.mvn/maven.config` activa voluntariamente el perfil Maven `desarrollo-local`
  y configura la ruta del DLL nativo de Microsoft JDBC.
- El perfil de pom.xml configura spring-boot:run con perfiles Spring
  `sqlserver,local`, dirección web 127.0.0.1, puerto 8081, SQL Server local
  127.0.0.1:1433 y base SistemaFacturacionWeb.
- El proceso Java de la aplicación recibe `java.library.path` y las propiedades
  locales necesarias. No requiere exportar variables en la terminal.
- Se mantiene autenticación integrada de Windows, encrypt=true y ddl-auto=none.
  La excepción trustServerCertificate=true sigue limitada al perfil local y a
  loopback por SqlServerTlsConfiguration. Spring Security y CSRF no cambian.
- La configuración del plugin Maven no cambia los valores de los JAR de
  producción. application.properties conserva el perfil nodb predeterminado y
  la validación de certificados sigue habilitada fuera de local.

Estos dos archivos reales se excluyen de Git. No contienen contraseñas: solo
la instalación de Java, el perfil Maven y la carpeta del DLL de esta máquina.
Los ejemplos sí se pueden publicar.

## Preparación única en otra computadora

1. Copiar `.mvn/local-java.cmd.example` como `.mvn/local-java.cmd` y editar
   JAVA_HOME para indicar la carpeta del JDK 21 instalado (sin `/bin`).
2. Copiar `.mvn/maven.config.example` como `.mvn/maven.config` y editar
   `local.sqlserver.auth-path` con la carpeta que contiene el DLL x64 de la
   misma versión del controlador JDBC. Mantener `-Pdesarrollo-local`.
3. Verificar que SQL Server local escucha en 1433 y que existe la base con su
   esquema. La cuenta Windows debe tener los permisos de aplicación necesarios.
4. Ejecutar el comando de arranque anterior. El puerto 8081 debe estar libre.

No se configura autenticación SQL con usuario y contraseña. Los secretos nunca
deben añadirse a estos ejemplos ni al repositorio.

## Pruebas

Con la misma preparación local:

```powershell
.\mvnw.cmd test
```

El perfil Maven configura también el DLL y la excepción TLS local para las
pruebas SQL. Las integraciones existentes son de lectura; las escrituras de
recuperación y administración permanecen simuladas. Este comando no crea ni
recupera administradores.

Para compilar sin el perfil Maven local, usar `-P!desarrollo-local`; requiere
Java 21 disponible y, si se ejecutan pruebas SQL, su configuración explícita.
No usar el perfil desarrollo-local para desplegar un servidor de producción.

## Diagnóstico

- `mvnw.cmd -v` debe indicar Java 21. Revisar el archivo local de Java si no es así.
- Un error de carga del DLL requiere revisar la carpeta x64 configurada.
- Las URL JDBC externas o perfiles adicionales heredados pueden entrar en
  conflicto con la configuración local; el validador TLS informa de los
  conflictos sin imprimir credenciales. Revisar la configuración externa.
- El wrapper corrige la detección de carpetas .m2 normales cuyo atributo Target
  es nulo, evitando indexar un valor inexistente.
- El arranque normal no modifica contraseñas. La recuperación administrativa
  sigue siendo un comando separado y requiere autorización explícita.

## Verificación realizada

Se abrió un PowerShell nuevo con `-NoProfile`, se retiraron las variables
heredadas de Java, Spring y SQL Server y se ejecutó el comando exacto de arranque.
El wrapper seleccionó Java 21.0.13, Spring activó sqlserver/local, confirmó
SistemaFacturacionWeb y GET http://localhost:8081/login respondió HTTP 200.
Después se cerró únicamente esa instancia de prueba.

Desde otra sesión limpia, `.\mvnw.cmd test` aprobó las 274 pruebas Java. También
pasaron las 25 pruebas JavaScript. No se cambiaron datos ni contraseñas reales.
Registros locales: `target/arranque-local.log` y `target/arranque-local-tests.log`.
