# Biblioteca Escolar EFT

Evaluación Final Transversal de Desarrollo Orientado a Objetos II.
Semana 9 — Duoc UC.

Aplicación de escritorio desarrollada con Java, Swing y MySQL para gestionar
el catálogo, los estudiantes, los préstamos y las devoluciones de una
biblioteca escolar.



## Funcionalidades

- Inicio de sesión mediante RUT y contraseña.
- Bibliotecario: gestión de categorías, libros, estudiantes y sus cuentas
  de acceso; registro de préstamos y devoluciones; consulta de reportes.
- Estudiante: consulta del catálogo, registro y devolución de sus propios
  préstamos, consulta de su historial y préstamos pendientes.
- Descuento de stock al prestar y reposición al devolver.
- Fecha de préstamo automática y vencimiento a los 7 días.
- Registro de la fecha real de devolución conservando el vencimiento original.
- Identificación de préstamos pendientes con atraso y devoluciones fuera de plazo.
- Reportes de libros más prestados, historial por estudiante y libros
  actualmente en préstamo.
- Validaciones y manejo centralizado de errores.
- Persistencia en MySQL para conservar la información al cerrar la aplicación.

## Organización

Los paquetes Java se encuentran en `src/cl/biblioteca`.

| Paquete | Responsabilidad |
| --- | --- |
| `main` | Inicio de la aplicación. |
| `modelo` | Entidades y sesión del usuario. |
| `vista` | Ventanas y formularios Swing. |
| `controlador` | Eventos de la interfaz y coordinación de tareas. |
| `servicio` | Reglas de negocio, permisos y operaciones relacionadas. |
| `dao` | Consultas y operaciones CRUD mediante JDBC. |
| `config` | Configuración y conexión con MySQL. |
| `interfaces` | Contratos para CRUD y operaciones de base de datos. |
| `util` | Validaciones, errores y tareas en segundo plano. |
| `pruebas` | Pruebas ejecutables de persistencia, servicios y concurrencia. |

La carpeta `sql` contiene los scripts de base de datos; `lib`, el conector
JDBC; y `config`, el ejemplo de configuración local.

## Diseño

- **POO:** `Usuario` es una clase abstracta. `Bibliotecario` y
  `UsuarioEstudiante` implementan el comportamiento de su rol mediante
  herencia y polimorfismo. `CrudDAO` y `OperacionBD` definen contratos comunes.
- **MVC:** las vistas presentan los datos y los controladores atienden sus eventos.
  Los servicios aplican las reglas de negocio y los DAO realizan la persistencia.
- **DAO:** separa las consultas SQL de la interfaz y utiliza parámetros JDBC.
- **Singleton:** `DatabaseConnection.getInstance()` centraliza el acceso a la conexión.
- **Hilos:** `TareaBD`, basada en `SwingWorker`, ejecuta operaciones de base de
  datos en segundo plano y entrega los resultados al hilo de Swing.
- **Sincronización y transacciones:** se coordina el acceso a la conexión y al
  stock. El préstamo y su descuento de stock se confirman juntos; la devolución
  y su reposición también. Las actualizaciones condicionadas evitan prestar
  sin stock o reponerlo dos veces por la misma devolución.

## Requisitos

- Java/JDK 26, utilizado para desarrollar y ejecutar el proyecto.
- IntelliJ IDEA para abrir, compilar y generar el ejecutable.
- MySQL Server en ejecución.
- MySQL Workbench o un cliente SQL para preparar la base de datos.
- Conector incluido: `lib/mysql-connector-j-26.7.0.jar`.

## Preparación de la base de datos

Para una instalación nueva, ejecutar en este orden:

1. `sql/PRY2203_EFT_S9_Script_crea_tablas_biblioteca.sql`
2. `sql/PRY2203_EFT_S9_Script_poblado_tablas_biblioteca.sql`
3. `sql/03_Agregar_fecha_devolucion_real.sql`

Los dos scripts originales se conservan. El tercer script agrega la columna
`fecha_devolucion_real` a la tabla `prestamos`.

Si la base de datos ya está preparada, no repetir los scripts: la creación
de tablas, el poblado y la incorporación de la columna no son idempotentes.

Los préstamos devueltos del poblado original no tienen una fecha real de
devolución conocida y se muestran como `Devuelto (sin fecha real)`.

## Configuración de MySQL

Copiar `config/basedatos.properties.example` como
`config/basedatos.properties` y completar los datos de la instalación local:

```properties
db.url=jdbc:mysql://localhost:3306/biblioteca?connectTimeout=5000&socketTimeout=15000
db.usuario=root
db.contrasena=tu_contrasena_mysql
```

La contraseña puede quedar vacía si el usuario de MySQL está configurado así.
Este archivo local está excluido de Git mediante `.gitignore`.

## Ejecución desde IntelliJ IDEA

1. Abrir la carpeta del proyecto y seleccionar el JDK 26.
2. Verificar en `File > Project Structure > Modules > Dependencies` que
   esté agregado el conector de la carpeta `lib`.
3. Usar la raíz del proyecto como `Working directory` en la configuración de ejecución.
4. Ejecutar `cl.biblioteca.main.Main`.

MySQL debe permanecer en ejecución durante el uso de la aplicación.

## Generación y ejecución del JAR

En IntelliJ IDEA:

1. Abrir `File > Project Structure > Artifacts`.
2. Seleccionar `+ > JAR > From modules with dependencies`.
3. Usar `cl.biblioteca.main.Main` como `Main Class` y elegir
   `Extract to the target JAR` para incluir el conector.
4. Usar la carpeta `src` como destino de `META-INF/MANIFEST.MF`.
5. Generar mediante `Build > Build Artifacts > Build`.
   Después de modificar el código, utilizar `Rebuild`.

El artefacto se genera dentro de `out/artifacts`. Para ejecutarlo, abrir
PowerShell en la raíz del proyecto y usar:

```powershell
$jarBiblioteca = Get-ChildItem '.\out\artifacts' -Filter 'BibliotecaEscolarEFT.jar' -Recurse -File | Select-Object -First 1
java -jar $jarBiblioteca.FullName
```

El comando `java` debe corresponder al JDK 26 y estar disponible en la terminal.
También puede ejecutarse usando la ruta completa a `java.exe`.

El JAR incluye el conector JDBC. Para funcionar requiere Java, MySQL y el
archivo externo `config/basedatos.properties`. Mantener la raíz del proyecto
como directorio de trabajo permite encontrar esa configuración.

## Usuarios del poblado inicial

| Perfil | Nombre | RUT | Contraseña |
| --- | --- | --- | --- |
| Bibliotecario | Antonia | `12345678-9` | `clave123` |
| Estudiante | Carlos | `98765432-1` | `clave123` |

Estas credenciales corresponden a los datos de ejemplo proporcionados para la actividad.

## Pruebas

El paquete `cl.biblioteca.pruebas` contiene clases con método `main` para
comprobar el CRUD, las validaciones, la autenticación, los permisos y los servicios.
También incluye `PruebaConcurrenciaPrestamos` y `PruebaReporteServicio`.

Las pruebas requieren MySQL configurado y los datos iniciales. Utilizan
registros temporales o transacciones con rollback para sus comprobaciones.

Se verificó manualmente la ejecución del JAR fuera de IntelliJ, el acceso
con ambos perfiles, el registro y la devolución de un préstamo, los cambios
de stock y la permanencia del historial después de cerrar y volver a abrir
la aplicación.