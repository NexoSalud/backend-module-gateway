**NEXOSALUD HIS**

# **Especificación del Contrato B**

Todo lo que Recaudo entrega a Facturación

Estado: Listo para implementación

# **1\. Qué entrega Recaudo, en una frase**

Recaudo entrega a Facturación el Contrato B: un evento financiero por episodio que indica cuánto pagó el paciente, bajo qué concepto, y con qué comprobante. No incluye ningún dato clínico y no calcula el valor a cargo de la EPS.

| Regla de oro Recaudo opera con dinero y afiliación. Los datos clínicos (CUPS, CIE-10) y el valor\_neto\_eps NO salen de Recaudo. Si alguno de esos aparece en el Contrato B, es un error de diseño que debe corregirse antes de mezclar el dato. |
| :---- |

# **2\. El Contrato B — estructura JSON**

Esta es la estructura completa que Recaudo publica hacia Facturación. Todos los campos se detallan campo por campo en la sección 4\.

| {   "contrato\_version": "2.0",   "evento\_id": "uuid-v4",   "episodio\_id": "EPI-2026-000123",   "paciente\_id": "...",   "eps\_id": "...",   "fecha\_atencion": "YYYY-MM-DD",   "tipo\_servicio": "consulta\_general | urgencia | hospitalizacion | procedimiento",   "numero\_autorizacion": "... | null",   "valor\_recaudado\_paciente": 12300,   "tipo\_cobro": "cuota\_moderadora | copago | particular | exento",   "comprobante\_recaudo\_id": "...",   "comprobante\_inmutable": true,   "corrige\_comprobante\_id": null,   "fecha\_emision\_evento": "YYYY-MM-DDThh:mm:ssZ" } |
| :---- |

# **3\. Ejemplos por tipo de cobro**

El mismo episodio cambia según cómo se cobró. Estos ejemplos son la referencia para los casos de prueba.

## **3.1 Copago (consulta externa contributivo)**

| {   "contrato\_version": "2.0",   "evento\_id": "8f3c...a1",   "episodio\_id": "EPI-2026-000123",   "paciente\_id": "PAC-55012",   "eps\_id": "EPS037",   "fecha\_atencion": "2026-05-20",   "tipo\_servicio": "consulta\_general",   "numero\_autorizacion": null,   "valor\_recaudado\_paciente": 12300,   "tipo\_cobro": "copago",   "comprobante\_recaudo\_id": "REC-2026-44871",   "comprobante\_inmutable": true,   "corrige\_comprobante\_id": null,   "fecha\_emision\_evento": "2026-05-20T09:14:02Z" } |
| :---- |

## **3.2 Exento — urgencia vital sin cobro (B cero explícito)**

| {   "contrato\_version": "2.0",   "evento\_id": "1b9d...c7",   "episodio\_id": "EPI-2026-000311",   "paciente\_id": "PAC-90233",   "eps\_id": "EPS012",   "fecha\_atencion": "2026-05-22",   "tipo\_servicio": "urgencia",   "numero\_autorizacion": null,   "valor\_recaudado\_paciente": 0,   "tipo\_cobro": "exento",   "comprobante\_recaudo\_id": "REC-2026-44990",   "comprobante\_inmutable": true,   "corrige\_comprobante\_id": null,   "fecha\_emision\_evento": "2026-05-22T18:40:11Z" } |
| :---- |

## **3.3 Particular (sin EPS)**

| {   "contrato\_version": "2.0",   "evento\_id": "4a2e...f0",   "episodio\_id": "EPI-2026-000402",   "paciente\_id": "PAC-77810",   "eps\_id": null,   "fecha\_atencion": "2026-05-25",   "tipo\_servicio": "procedimiento",   "numero\_autorizacion": null,   "valor\_recaudado\_paciente": 185000,   "tipo\_cobro": "particular",   "comprobante\_recaudo\_id": "REC-2026-45102",   "comprobante\_inmutable": true,   "corrige\_comprobante\_id": null,   "fecha\_emision\_evento": "2026-05-25T11:02:55Z" } |
| :---- |

## **3.4 Corrección de un recaudo previo**

| {   "contrato\_version": "2.0",   "evento\_id": "9c7f...b3",        // evento NUEVO   "episodio\_id": "EPI-2026-000123",   "paciente\_id": "PAC-55012",   "eps\_id": "EPS037",   "fecha\_atencion": "2026-05-20",   "tipo\_servicio": "consulta\_general",   "numero\_autorizacion": null,   "valor\_recaudado\_paciente": 10300,  // valor corregido   "tipo\_cobro": "copago",   "comprobante\_recaudo\_id": "REC-2026-44999",  // nuevo comprobante   "comprobante\_inmutable": true,   "corrige\_comprobante\_id": "REC-2026-44871",  // apunta al original   "fecha\_emision\_evento": "2026-05-20T15:31:00Z" } |
| :---- |

## **3.5 Servicios derivados en consulta externa (episodios independientes)**

**Caso operativo más frecuente en consulta externa.** El paciente asiste a la consulta inicial y paga su cuota moderadora o copago (episodio EPI-2026-000123). En esa consulta el médico ordena un servicio derivado: cita con especialista, examen de laboratorio, imagen diagnóstica o procedimiento.

Ese servicio derivado **NO** pertenece al episodio de la consulta inicial. Genera una **nueva admisión** (Contrato A) y por tanto un **episodio\_id propio**. Cuando el paciente se acerca a Recaudo a pagar la cuota moderadora o copago de esa orden, Recaudo emite un Contrato B independiente, con la misma estructura, contra el nuevo episodio.

**Regla de cardinalidad:** cada episodio\_id produce exactamente un evento B (salvo corrección). Recaudo nunca acumula varios pagos no-correctivos sobre el mismo episodio. Si hay un nuevo servicio que cobrar, hay un nuevo episodio.

*Ejemplo — pago de procedimiento derivado de la consulta EPI-2026-000123:*

| {   "contrato\_version": "2.0",   "evento\_id": "c2a1...d9",   "episodio\_id": "EPI-2026-000777",   // NUEVO episodio (no el de la consulta)   "paciente\_id": "PAC-55012",          // mismo paciente   "eps\_id": "EPS037",                  // misma afiliación   "fecha\_atencion": "2026-06-05",      // fecha del servicio derivado   "tipo\_servicio": "procedimiento",    // o consulta\_general si es interconsulta   "numero\_autorizacion": "AUT-2026-31204",  // puede venir poblado (lo validó Admisión)   "valor\_recaudado\_paciente": 8200,   "tipo\_cobro": "cuota\_moderadora",    // puede diferir del cobro de la consulta inicial   "comprobante\_recaudo\_id": "REC-2026-46550",   "comprobante\_inmutable": true,   "corrige\_comprobante\_id": null,   "fecha\_emision\_evento": "2026-06-05T10:12:00Z" } |
| :---- |

Diferencias clave de este evento frente al de la consulta inicial:

* episodio\_id: nuevo. Facturación lo trata como un episodio independiente; converge su B con su propio C. No tiene relación de convergencia con la consulta inicial.

* numero\_autorizacion: aquí sí puede venir poblado si la orden lo requería. Recaudo solo lo transporta; la validación ya ocurrió en Admisión.

* tipo\_servicio: cambia a procedimiento (o se mantiene consulta\_general en interconsultas con especialista).

* tipo\_cobro: puede diferir del de la consulta inicial. Un mismo afiliado paga cuota moderadora en la consulta general y copago en el procedimiento, o viceversa, según su plan.

| Por qué episodios independientes y no un episodio-paquete En consulta externa el cobro al paciente ocurre por cada atención que se admite. Tratar la consulta y sus derivados como un solo episodio obligaría a acumular varios pagos con comprobantes distintos sobre un mismo episodio\_id, lo cual choca con la validación de unicidad de comprobante y con la convergencia 1:1 B+C de Facturación. Episodios independientes mantienen la trazabilidad limpia: un episodio, un cobro, un comprobante, un par B+C. |
| :---- |

# **4\. Diccionario de datos — campo por campo**

Para cada campo: tipo, obligatoriedad, qué es y por qué lo necesita Facturación.

| Campo | Tipo / Oblig. | Qué es y por qué lo necesita Facturación |
| :---- | :---- | :---- |
| contrato\_version | string · Obl. | Versión del esquema del contrato (“2.0”). Permite evolucionar sin romper a Facturación en silencio: si se agrega un campo, Facturación sabe contra qué versión parsea. Regla permanente del proyecto. |
| evento\_id | UUID · Obl. | Identificador único DEL MENSAJE (no del episodio). Clave de idempotencia: un reintento por fallo de red llega con el mismo evento\_id y se procesa una sola vez. Sin esto, un reintento es doble descuento. |
| episodio\_id | string · Obl. | Clave de negocio que une el Contrato B con el Contrato C dentro de Facturación. Lo genera Admisión, no Recaudo; Recaudo lo recibe en el Contrato A y lo propaga. Facturación converge B+C por este campo. Único cross-sede. Cada servicio derivado tiene su propio episodio\_id. |
| paciente\_id | string · Obl. | Identifica al paciente. Facturación lo usa para traer afiliación del Módulo de Pacientes y para el RIPS. |
| eps\_id | string · Obl.\* | El pagador. Facturación lo usa para seleccionar el contrato tarifario (la tarifa es por EPS) y para saber a quién radicar. \*Puede ir null si tipo\_cobro \= particular. |
| fecha\_atencion | date · Obl. | Fecha del evento clínico. Determina: (1) la tarifa vigente aplicable, (2) la versión de catálogos CUPS/CIE-10 válida, (3) la fecha contra la que el MUV valida afiliación en BDUA. Todo lo fechado en Facturación depende de este campo. |
| tipo\_servicio | enum · Obl. | consulta\_general | urgencia | hospitalizacion | procedimiento. NO es dato clínico (no es CUPS/CIE-10); es la categoría que Recaudo ya usa para calcular cuota o copago. Facturación lo usa como contexto del RIPS y para ramificar el flujo (urgencias). |
| numero\_autorizacion | string | null | Si el servicio requiere autorización previa, este número viaja de Admisión a través de Recaudo. Recaudo NO lo valida ni bloquea (eso ya pasó en Admisión); solo lo transporta. Facturación lo incluye en el RIPS porque el MUV lo exige cuando aplica. Frecuente en servicios derivados. |
| valor\_recaudado\_paciente | entero · Obl. | Cuánto pagó efectivamente el paciente en ventanilla, sin decimales. Facturación lo reporta en el RIPS como pago moderador y, según tipo\_cobro, lo usa para el neto EPS. En exención va 0\. |
| tipo\_cobro | enum · Obl. | cuota\_moderadora | copago | particular | exento. Define el efecto sobre el valor a cargo de la EPS (ver sección 5). Sin este campo, Facturación aplicaría una resta universal y facturaría mal el neto. |
| comprobante\_recaudo\_id | string · Obl. | Referencia al comprobante emitido al paciente. Trazabilidad y base de la conciliación caja-vs-facturación. Único por evento (salvo corrección). |
| comprobante\_inmutable | boolean | Marca que el comprobante es soporte contable inmutable (Res. 1995/1999). No se edita: si hay error, se corrige con un nuevo evento. Señaliza a Facturación que el comprobante no va a mutar. |
| corrige\_comprobante\_id | string | null | Si Recaudo corrige un recaudo ya enviado, emite un evento NUEVO con este campo apuntando al comprobante original. Permite a Facturación distinguir una corrección legítima de un duplicado. En el caso normal va null. |
| fecha\_emision\_evento | timestamp | Cuándo Recaudo generó el evento. Auditoría, ordenamiento y detección de eventos rezagados en la cola. |

# **5\. tipo\_cobro y su efecto en el valor a cargo de la EPS**

Recaudo NO calcula el neto EPS, pero el tipo\_cobro que envía determina cómo lo calcula Facturación. Esta tabla es la regla financiera central.

| tipo\_cobro | ¿Descuenta del neto EPS? | Fórmula que aplica Facturación |
| :---- | :---- | :---- |
| copago | Sí. Es participación del afiliado en el valor del servicio. | neto\_eps \= valor\_total\_tarifario − valor\_recaudado\_paciente |
| cuota\_moderadora | Por defecto no. Es un cobro por evento, no una fracción del costo. Depende del contrato EPS. | neto\_eps \= valor\_total\_tarifario (la cuota se reporta como recaudo, no se resta) |
| exento | No hay cobro al paciente. | neto\_eps \= valor\_total\_tarifario |
| particular | No aplica (no hay EPS). | no se calcula neto EPS; FEV particular por el valor total |

| Decisión pendiente del equipo El tratamiento exacto de la cuota moderadora frente al neto varía por contrato IPS-EPS. Debe quedar parametrizado por EPS, no hardcodeado. Por defecto no se resta. Confirmar con cuentas médicas. |
| :---- |

# **6\. Lo que Recaudo NO entrega (y por qué)**

Esto es tan parte del contrato como los campos incluidos. Estos datos NO deben aparecer en el Contrato B.

| No entrega | Por qué |
| :---- | :---- |
| CUPS y CIE-10 | Son datos clínicos: los genera el médico y van por el Contrato C directo a Facturación. Si pasaran por Recaudo bloquearían el cobro anticipado (en consulta externa Recaudo cobra antes de existir la HC) y violarían la minimización de datos sensibles (Ley 1581/2012). |
| valor\_neto\_eps | Recaudo no conoce la tarifa por CUPS, así que no puede calcularlo. Lo calcula Facturación tras converger B+C. (Este era el error de la v1.3.) |
| valor\_total\_tarifario | Mismo motivo: depende de los CUPS, que Recaudo no tiene. |
| RIPS, FEV, CUV | Son productos de Facturación, no de Recaudo. |
| Aplicación contable del pago de la EPS | Eso es Cartera, no Recaudo ni Facturación. |

# **7\. Cómo se entrega (mecánica de integración)**

El qué sin el cómo deja agujeros operativos. Recaudo publica el Contrato B con estas garantías.

**Patrón outbox transaccional.** Recaudo escribe el evento B en la misma transacción de base de datos en que emite el comprobante. Un publicador relé lo envía al bus. Garantía: nunca hay “comprobante emitido pero evento perdido”.

**Asíncrono.** Recaudo publica y sigue; no espera a Facturación. Si Facturación está caída, el evento queda en el outbox y se reintenta con backoff exponencial. Tras N intentos va a dead-letter con alerta. Ningún episodio cobrado se pierde en silencio.

**Idempotente.** Facturación deduplica por evento\_id. Reenviar el mismo evento es seguro y no produce efecto adicional.

**Orden no garantizado.** El Contrato C puede llegar antes o después que el B. La convergencia en Facturación no asume orden; espera a tener ambos (o a un B exento) por episodio\_id.

# **8\. Validaciones que Facturación corre al recibir el Contrato B**

Complemento de la spec: qué valida Facturación al consumir el evento y cómo responde. Esto define el manejo de errores del contrato.

| Validación | Si falla | Acción |
| :---- | :---- | :---- |
| contrato\_version soportada | Versión desconocida o no soportada | Rechazar a dead-letter; alertar. No procesar a ciegas. |
| evento\_id presente y no procesado antes | evento\_id duplicado | Descartar silenciosamente (idempotencia). No es error: es un reintento. |
| episodio\_id existe (Contrato A previo) | episodio\_id sin admisión | Rechazar; alertar. Un recaudo sin episodio es inconsistente. |
| episodio\_id sin evento B no-correctivo previo (1:1) | Segundo B no-correctivo sobre el mismo episodio\_id | Rechazar; alertar. Un servicio nuevo debe traer un episodio nuevo, no reutilizar el del episodio ya cobrado. |
| valor\_recaudado\_paciente \>= 0 y coherente con tipo\_cobro | Valor negativo, o \> 0 con tipo\_cobro \= exento | Rechazar; alertar. Posible error de caja. |
| tipo\_cobro en el enum permitido | Valor fuera del enum | Rechazar; alertar. |
| eps\_id presente salvo particular | eps\_id null con tipo\_cobro \!= particular | Rechazar; alertar. |
| comprobante\_recaudo\_id presente y único (salvo corrección) | Comprobante repetido sin corrige\_comprobante\_id | Rechazar como duplicado; alertar. |
| corrige\_comprobante\_id apunta a un comprobante existente | Referencia de corrección inválida | Rechazar; alertar. |
| numero\_autorizacion presente si el servicio lo requiere | Falta y el servicio lo exige | No bloquear aquí (ya se controla en Admisión); marcar para revisión antes del MUV. |

| Principio de manejo de errores Duplicado por idempotencia \= descartar sin ruido. Cualquier otra falla \= a dead-letter con alerta, nunca descartar en silencio. Recaudo nunca queda bloqueado por un fallo de Facturación: el comprobante al paciente es una operación local que ya se completó. |
| :---- |

# **9\. Casos especiales que Recaudo debe manejar**

| Caso | Comportamiento de Recaudo |
| :---- | :---- |
| Servicio derivado (especialista, examen, procedimiento ordenado) | Es un episodio nuevo, no el de la consulta inicial. Recaudo cobra la cuota moderadora o copago contra el nuevo episodio\_id (generado por Admisión) y emite un Contrato B independiente. Misma estructura; difieren episodio\_id, fecha\_atencion, posiblemente tipo\_servicio, tipo\_cobro y numero\_autorizacion. Ver sección 3.5. |
| Urgencias | El recaudo ocurre al egreso, no al ingreso. El Contrato C puede llegar a Facturación antes que el B. Si la urgencia es vital y exenta, Recaudo igual emite un Contrato B con tipo\_cobro \= exento y valor 0 (“B cero explícito”) para que Facturación no quede esperando indefinidamente. |
| Particular | tipo\_cobro \= particular, eps\_id \= null. Recaudo cobra el valor total. Facturación emite FEV particular y nunca toca MUV ni BDUA. |
| Exento vs. aún-no-cobrado | Un valor 0 con tipo\_cobro \= exento es distinto de “todavía no se cobró”. El flag explícito evita que Facturación confunda un episodio exento con uno pendiente. |
| Corrección / reverso | Nunca se edita un comprobante emitido. Se emite un evento nuevo con corrige\_comprobante\_id apuntando al original. Facturación reliquida en consecuencia. |

# **10\. Checklist de implementación para el equipo**

Marcar a medida que se implementa. Esta lista es la definición de “listo” para el lado de Recaudo del contrato.

☐  Generar evento\_id UUID único por mensaje.

☐  Propagar episodio\_id recibido en el Contrato A (no generarlo en Recaudo).

☐  Un episodio\_id \= un evento B no-correctivo. Nunca acumular varios pagos sobre el mismo episodio; un servicio nuevo exige un episodio nuevo desde Admisión.

☐  Incluir contrato\_version en todo evento.

☐  Persistir el evento en tabla outbox dentro de la misma transacción del comprobante.

☐  Publicador relé que envía el outbox al bus de eventos.

☐  Reintentos con backoff exponencial y cola dead-letter con alerta.

☐  tipo\_cobro obligatorio y validado contra el enum antes de publicar.

☐  valor\_recaudado\_paciente \= 0 con tipo\_cobro \= exento en urgencias vitales.

☐  eps\_id \= null permitido solo con tipo\_cobro \= particular.

☐  Transportar numero\_autorizacion cuando venga de Admisión (frecuente en servicios derivados); no validarlo ni bloquearlo en Recaudo.

☐  Mecanismo de corrección vía corrige\_comprobante\_id (nunca editar comprobante).

☐  No incluir jamás CUPS, CIE-10 ni valor\_neto\_eps en el payload.

☐  Comprobante de recaudo inmutable (soft delete, registro con usuario y timestamp).

NexoSalud HIS · Especificación del Contrato B (Recaudo → Facturación) · v1.1 · Junio 2026

Alineado con: Relación Recaudo–Facturación v2.0 · Res. 2275/2023, 1884/2024 · Ley 1581/2012 · Res. 1995/1999


**NEXOSALUD HIS**

# **0\. Frontera de responsabilidad del módulo de Recaudo**

| Regla rectora. El módulo de Recaudo tiene una sola responsabilidad: calcular y cobrar al paciente lo que la norma le obliga a pagar de su bolsillo en el punto de atención (cuota moderadora, copago o tarifa particular), registrar ese cobro con validez fiscal y reportarlo. Todo lo demás está fuera de su frontera. |
| :---- |

| Sí es responsabilidad de Recaudo | NO es responsabilidad de Recaudo |
| :---- | :---- |
| Determinar el valor a cobrar al paciente | Calcular el neto a la EPS (Facturación) |
| Emitir y numerar el recibo de caja | Construir el RIPS y la FEV (Facturación) |
| Registrar método de pago y arqueo de caja | Aplicar pagos a la cartera del paciente (Cartera) |
| Marcar episodios como pendientes de cobro | Gestionar glosas y respuestas a la EPS (Cuentas Médicas) |
| Reportar valor\_recaudado\_paciente por episodio | Conciliación bancaria y asentamiento (Cartera / Tesorería) |

# **1\. Cuándo ocurre el cobro según el tipo de atención**

| Tipo de atención | Momento del cobro | ¿Servicio prestado al cobrar? |
| :---- | :---- | :---- |
| Consulta externa programada (médico general, especialista, odontología) | ANTES de la atención — al admitir / confirmar presencia | No. Se cobra con base en la cita agendada. |
| Urgencias | DESPUÉS de la atención o al egreso | Sí. Se cobra sobre servicios ya prestados. |
| Hospitalización / Cirugía | Al egreso, o anticipos parciales durante la estancia | Parcial o total según política de la IPS. |
| Procedimiento diagnóstico ambulatorio | Generalmente antes de ejecutar el procedimiento | No. Se cobra al confirmar la orden. |

| Implicación crítica (consulta externa). El trigger del recaudo no es 'servicio prestado' sino 'cita confirmada / paciente presente'. El módulo debe poder iniciar el cobro sin HC diligenciada ni CUPS registrado por el médico. El valor a cobrar (cuota moderadora) no depende del costo del servicio. |
| :---- |

| Prohibición legal transversal (Ley 1751 de 2015). En urgencias está prohibido condicionar la atención al pago. El sistema registra la atención y deja el recaudo en estado PENDIENTE. Esta regla prevalece sobre cualquier configuración de cobro. |
| :---- |

# **2\. Visión general del flujo de cálculo**

Antes de mostrar cualquier valor al cajero, el sistema resuelve cinco variables en secuencia. La omisión o el orden incorrecto de cualquiera produce cobro indebido.

| Paso | Variable | Pregunta que responde |
| :---- | :---- | :---- |
| 1 | Identidad y afiliación | ¿Quién es el paciente y qué afiliación, rol y categoría tiene vigentes a la fecha de atención? (según Módulo de Pacientes) |
| 2 | Tipo de servicio | ¿Qué servicio va a recibir o recibió? ¿Es un servicio PyD exento de cuota moderadora? |
| 3 | Aplicabilidad de exención | ¿El paciente o el servicio están exentos por norma? (la exención corta el flujo) |
| 4 | Tipo de cobro | Según 1–3: cuota moderadora, copago, tarifa particular o $0. |
| 5 | Valor exacto | Cálculo con UVB vigente, categoría, rol del afiliado y topes por evento / anuales. |

# **3\. Paso a paso del cálculo**

## **Paso 1 — Identificar al paciente**

El sistema consulta el Módulo de Pacientes con el número de documento y recupera:

* Tipo de afiliación: contributivo, subsidiado, régimen de excepción/especial, ARL, SOAT, póliza, particular, sin afiliación verificable.

* Estado de afiliación: activo, suspendido, novedad pendiente.

* Rol del afiliado: **cotizante** o **beneficiario**. Atributo obligatorio: determina si aplica copago.

* Si es contributivo: el IBC del cotizante, del cual el sistema deriva la categoría A / B / C.

* Si es subsidiado: la condición de afiliación y los marcadores de exención poblacional aplicables.

### **Resolución de categoría contributiva (Acuerdo 260 de 2004\)**

| Categoría | Condición de IBC | Referencia |
| :---- | :---- | :---- |
| A | Menor a 2 SMLMV | Acuerdo 260/2004 |
| B | Entre 2 y 5 SMLMV | Acuerdo 260/2004 |
| C | Mayor a 5 SMLMV | Acuerdo 260/2004 |

Si en el núcleo familiar existe más de un cotizante, la categoría se calcula sobre el **menor ingreso declarado** del núcleo.

| Sin verificación de derechos en línea (decisión 2). Esta versión no consulta BDUA/ADRES. La afiliación, el régimen, el rol y la categoría se toman tal como están registrados en el Módulo de Pacientes. La exactitud del cobro depende de que ese registro esté actualizado. Cuando la afiliación no sea verificable internamente, el sistema aplica el cobro más conservador (particular) sin condicionar la atención, y habilita ajuste posterior por el rol auditor con trazabilidad. |
| :---- |

| Regla crítica de temporalidad. La afiliación, la categoría y la tarifa determinantes son las vigentes en la fecha de la atención, no en la fecha del pago. Aplica por igual a la afiliación y a los valores paramétricos (UVB, tablas, topes). |
| :---- |

## **Paso 2 — Identificar el tipo de servicio**

Para consulta externa programada, el sistema trae del Módulo de Consulta Externa:

* Tipo de cita: primera vez o control. Conservado solo para RIPS / epidemiología; sin efecto financiero.

* Especialidad o servicio: médico general, especialista, odontología, etc.

* Marcador PyD: si el servicio corresponde a **Protección Específica y Detección Temprana** (control prenatal, vacunación, citología, planificación, control de crónicos, etc.).

Para servicios ya prestados (urgencias, hospitalización, procedimientos), trae además:

* cups\_array: lista indexada de los CUPS registrados al cerrar la atención (uno o varios por egreso).

* El valor de cada CUPS resuelto por el motor de tarifas (servicio compartido, ver Paso 5b).

| Array\<CUPS\>. Para servicios prestados, la entrada es una lista de procedimientos, no un código único. El sistema liquida cada CUPS de forma independiente contra el manual tarifario aplicable y consolida la suma antes de presentar la pantalla de cobro. |
| :---- |

## **Paso 3 — Aplicabilidad de exención (corta el flujo)**

Antes de determinar el tipo de cobro, el sistema evalúa exenciones. **Una exención válida fija el cobro en $0 y detiene el cálculo posterior.**

| Categoría de exención | Fuente de verdad del marcador |
| :---- | :---- |
| Servicios PyD (Protección Específica y Detección Temprana) | Marcador es\_PyD desde Consulta Externa (por CUPS / finalidad) |
| Atención del parto y control prenatal | Marcador clínico desde Consulta Externa / HC |
| Enfermedades de alto costo / catastróficas / ruinosas | Marcador en perfil del paciente, validado por auditoría médica |
| Víctimas del conflicto armado | Marcador poblacional en perfil del paciente |
| Programas de promoción y prevención y control de crónicos | Marcador del programa en Consulta Externa |
| Menores, gestantes y demás exenciones del Decreto 1652/2022 | Edad / condición desde Módulo de Pacientes \+ tabla de exenciones |
| Régimen subsidiado exento por norma | Régimen registrado en Pacientes \+ tabla de exenciones |

| Atención. Si ningún marcador de exención aplica, el flujo continúa al Paso 4\. La ausencia de marcador nunca debe inferirse como 'no exento' cuando el servicio es PyD: el marcador es\_PyD es obligatorio en el contrato con Consulta Externa. |
| :---- |

## **Paso 4 — Determinar el tipo de cobro**

| Afiliación | Rol | Servicio | Tipo de cobro | Fuente del valor |
| :---- | :---- | :---- | :---- | :---- |
| Contributivo | Cotizante o beneficiario | Consulta ext. ambulatoria, medicamento, exámenes básicos | Cuota moderadora (fija) | Tabla UVB × categoría |
| Contributivo | Solo beneficiario | Hospitalización, cirugía, procedimiento | Copago (% del servicio) | % × valor servicio, con topes |
| Contributivo | Cotizante | Hospitalización, cirugía, procedimiento | $0 por copago | El cotizante no paga copago |
| Subsidiado | — | Servicios sujetos a copago según norma | Copago subsidiado | Tabla copago subsidiado |
| Subsidiado | — | Servicios exentos (PyD, poblaciones protegidas) | $0 (exento) | Norma SGSSS |
| Excepción / especial | — | Según régimen | Según régimen | Tabla del régimen |
| ARL / SOAT / Póliza | — | Cualquiera | $0 al paciente | Lo asume el tercero |
| Particular | — | Cualquiera | 100% del servicio | Tarifa propia IPS |
| Sin afiliación verificable | — | Cualquiera | Provisional como particular | Tarifa propia, ajustable |

| Corrección estructural. El copago es exclusivo de beneficiarios. Un cotizante contributivo nunca paga copago; en hospitalización/cirugía paga $0 por concepto de copago, sin perjuicio de las cuotas moderadoras de servicios ambulatorios asociados cuando apliquen. |
| :---- |

## **Paso 5 — Calcular el valor exacto**

Todos los cálculos usan la **UVB\_vigente** a la fecha de atención. Los valores se almacenan en UVB y se convierten a pesos con la equivalencia oficial.

### **5a. Cuota moderadora (consulta externa, servicios ambulatorios)**

Monto **fijo** que NO depende del costo del servicio. Se obtiene de la tabla vigente cruzando la categoría (A/B/C) con la UVB\_vigente. No requiere que el médico haya atendido al paciente.

* El campo tipo\_cita (primera vez / control) no tiene efecto en este cálculo.

* Los servicios marcados es\_PyD \= true no generan cuota moderadora (cobro $0).

valor\_cuota\_moderadora \= tabla\_cuota\_moderadora\[categoria, UVB\_vigente, fecha\_atencion\]

si servicio.es\_PyD \== true  →  valor\_cuota\_moderadora \= 0

### **5b. Copago (hospitalización, cirugía, procedimientos — solo beneficiarios / subsidiado según norma)**

Requiere el **valor del servicio** resuelto por el motor de tarifas (servicio compartido), que selecciona la tarifa correcta según:

(EPS contratante) × (manual: ISS / SOAT / propio) × (vigencia del contrato a la fecha) × (CUPS)

Con el valor de cada CUPS resuelto, el cálculo aplica **dos topes simultáneos**:

para cada cups\_i en cups\_array:  
    copago\_bruto\_i \= porcentaje\_categoria × valor\_servicio\_i  
   
copago\_evento \= Σ copago\_bruto\_i  
copago\_evento\_topado \= min(copago\_evento, tope\_por\_evento\[categoria, UVB\_vigente\])  
   
// Acumulado LOCAL de esta IPS en el año calendario (decisión 1\)  
acumulado\_anual \= consultar\_acumulado\_anual\_local(paciente, anio)  
disponible\_anual \= max(0, tope\_anual\[categoria, UVB\_vigente\] \- acumulado\_anual)  
   
copago\_final \= min(copago\_evento\_topado, disponible\_anual)

* Si disponible\_anual \== 0, el copago es $0 y se muestra alerta al cajero.

* El porcentaje por categoría y los topes (evento y anual) se obtienen de tablas parametrizadas en UVB.

| Dos topes, no uno. El tope por evento se aplica antes del tope anual. Omitirlo produce sobrecobro en eventos de alto costo, sujeto a devolución. |
| :---- |

| Limitación conocida del acumulado local (decisión 1, riesgo R-03). El tope anual se controla solo con los copagos cobrados en esta IPS. Si el paciente alcanzó el tope en otra IPS de la misma EPS, este sistema no lo sabrá y podría volver a cobrar. Es una limitación aceptada por alcance, no un defecto de cálculo. |
| :---- |

### **5c. Cobro total — paciente particular**

Aplica directamente la tarifa propia de la IPS para cada CUPS del arreglo. No hay porcentaje ni tope.

valor\_particular \= Σ tarifa\_propia\_IPS\[cups\_i, vigencia, fecha\_atencion\]

## **Paso 6 — Deudas anteriores (informativo, no condicionante)**

El sistema consulta recaudos pendientes de episodios anteriores y los muestra al cajero de forma informativa, claramente separados del valor del servicio actual.

| Corrección legal. Las deudas anteriores no se pre-seleccionan de forma obligatoria ni se penaliza su desmarque. No se puede condicionar la prestación del servicio actual al pago de deudas previas (en urgencias está expresamente prohibido). Si el paciente decide pagar la deuda, se registra como transacción separada con su propio consecutivo fiscal, nunca mezclada con el recibo del servicio actual. La gestión de cobro de cartera del paciente corresponde al módulo de Cartera. |
| :---- |

## **Paso 7 — Presentar el valor al cajero**

El cajero ve en pantalla:

* Datos del paciente \+ tipo de afiliación \+ rol (cotizante / beneficiario) \+ categoría.

* Tipo de atención.

* Tipo de cobro determinado por el sistema (cuota moderadora / copago / particular / $0).

* Base de cálculo visible: monto fijo (cuota moderadora) o desglose por CUPS con valor, porcentaje y topes aplicados (copago).

* Total a cobrar por el servicio actual.

* Deudas anteriores informativas (separadas, no sumadas por defecto).

* Método de pago.

| Control. El cajero no calcula nada manualmente. El sistema entrega el valor ya calculado. Solo un rol con permiso explícito puede modificarlo, y toda modificación queda auditada (quién, cuándo, valor original, valor nuevo, motivo). |
| :---- |

# **4\. Máquina de estados del recaudo**

El recaudo de un episodio es una entidad con ciclo de vida explícito. Sin esto, la caja no cuadra ni es auditable.

| Estado | Significado | Transiciones válidas |
| :---- | :---- | :---- |
| PENDIENTE | Cobro calculado, aún no pagado | → PARCIAL, SALDADO, ANULADO |
| PARCIAL | Pago parcial recibido | → SALDADO, ANULADO |
| SALDADO | Pagado en su totalidad | → ANULADO (solo con reverso autorizado) |
| NO\_APLICA | Cobro $0 por exención o tercero pagador | (terminal; genera transacción $0 trazable) |
| ANULADO | Recibo anulado / reversado con autorización | (terminal) |

| Nota sobre recaudos diferidos (decisión 4). Se elimina el estado DIFERIDO y el flujo de recuperación de esta versión. En urgencias, la atención se registra y el recaudo queda en PENDIENTE sin un proceso automático de recuperación posterior. La gestión de ese saldo, si se decide implementarla, se abordará como deuda técnica en una versión futura. |
| :---- |

**Reglas de caja:**

* Cada transacción de cobro tiene consecutivo fiscal único y secuencial.

* Las anulaciones y reversos requieren rol con permiso, motivo y quedan auditados; nunca se borra el registro original (soft delete \+ trazabilidad).

* El sistema soporta cierre de turno / arqueo de caja: cuadre entre lo cobrado registrado y lo recibido por método de pago.

* Los pacientes exentos / tercero pagador generan una transacción NO\_APLICA con valor $0 y motivo, dentro del flujo de Recaudo, preservando la trazabilidad.

| Corrección. Se elimina el 'bypass' donde Admisiones auto-generaba un comprobante EXENTO-ADM. El exento se modela como transacción $0 (NO\_APLICA) emitida por Recaudo, evitando comprobantes ficticios fuera de la numeración fiscal. |
| :---- |

# **5\. Casos especiales**

| Caso | Lógica del sistema |
| :---- | :---- |
| Paciente exento (menor, gestante, alto costo, víctima, PyD) | Se identifica en el Paso 3, fija el cobro en $0, genera transacción NO\_APLICA con motivo y reporta valor\_recaudado\_paciente: 0\. No pasa por pago, pero queda registrado en Recaudo. |
| Servicio PyD (prenatal, vacunación, citología, crónicos) | No genera cuota moderadora. Cobro $0 por norma, independiente del régimen contributivo. |
| Cotizante en hospitalización / cirugía | $0 por copago (el copago es solo de beneficiarios). |
| Tope por evento alcanzado | El copago se topa al máximo por evento. Alerta al cajero. |
| Tope anual local alcanzado | Consulta el acumulado anual local; si el nuevo copago lo supera, cobra solo hasta el tope o $0. Alerta al cajero. |
| Urgencia vital | Por ley no se condiciona la atención al pago. La atención se registra y el recaudo queda PENDIENTE (sin flujo de recuperación en esta versión). |
| Afiliación no verificable internamente | Cobro provisional como particular sin condicionar la atención. Ajuste posterior por rol auditor con trazabilidad. |
| Descuento / exoneración por política interna | Requiere rol con permiso especial. Registra quién autorizó, motivo, fecha, valor original vs. cobrado. |
| Paciente presente sin registro del médico | El cobro de cuota moderadora procede igual. No requiere HC ni CUPS. |
| Pago parcial | Estado PARCIAL. El saldo del paciente lo gestiona Cartera, no Recaudo. |

# **6\. Parámetros configurables**

Todas estas tablas cambian por norma y deben ser actualizables sin despliegue de código, con vigencia temporal (vigencia\_desde / vigencia\_hasta) para liquidar cada atención con la tabla correcta a su fecha.

| Parámetro | Descripción | Indexación |
| :---- | :---- | :---- |
| UVB\_vigente | Unidad de Valor Básico oficial por vigencia. Insumo base de todos los cálculos. | Resolución anual de Hacienda |
| Tabla de cuotas moderadoras | Monto fijo por categoría A/B/C, en UVB y su equivalente en pesos. No varía por tipo\_cita. | Circular externa anual |
| Tabla de porcentajes de copago | Porcentaje por categoría y régimen. | UVB / norma |
| Topes de copago por evento | Monto máximo por evento, por categoría, en UVB. | UVB / norma |
| Topes de copago anuales | Monto máximo acumulado por afiliado en el año calendario, por categoría. | UVB / norma |
| Tabla de copago subsidiado | Porcentajes / montos del régimen subsidiado según norma. | Norma SGSSS |
| Catálogo de exenciones | Condiciones que generan $0, con la fuente de verdad de cada marcador. | Decreto 1652/2022 y conexas |
| Motor de tarifas IPS-EPS (servicio compartido) | Valor del servicio por EPS × manual × vigencia × CUPS. Fuente única para Recaudo y Facturación. | Contratos tarifarios |
| Tarifa propia (particulares) | Manual de precios internos de la IPS. | Política IPS |
| Tabla maestra CIE-10 / CIE-11 | CIE-10 por defecto; CIE-11 se activa globalmente cuando el MUV actualice el validador. | Resolución 948/2026 \+ MUV |

| Indexación UVB obligatoria. Desde el 1 de enero de 2026 (Circular Externa 048 de 2025, Resolución 3488 de 2025), las cuotas moderadoras, copagos y topes se indexan a la UVB, no al SMLMV directo. Almacenar valores en UVB con su equivalencia en pesos es requisito de cumplimiento. |
| :---- |

# **7\. Reglas de negocio críticas**

| ID | Regla |
| :---- | :---- |
| RN-01 | La afiliación, categoría y tarifa determinantes son las vigentes en la fecha de atención, no en la del cobro. Aplica también a UVB\_vigente y a todas las tablas paramétricas. |
| RN-02 | En consulta externa, el recaudo ocurre antes de la atención. No requiere HC diligenciada ni CUPS. |
| RN-03 | La cuota moderadora la pagan cotizante y beneficiarios. El copago lo pagan únicamente beneficiarios; el cotizante nunca paga copago. |
| RN-04 | La cuota moderadora es un monto fijo por categoría A/B/C y UVB\_vigente, sin diferenciación por tipo\_cita. |
| RN-05 | Los servicios de Protección Específica y Detección Temprana (PyD) no generan cuota moderadora (cobro $0). |
| RN-06 | El copago aplica dos topes: por evento y anual por afiliado. Cálculo: min(% × valor\_servicio topado por evento, disponible del tope anual). |
| RN-07 | El tope anual de copago es individual por afiliado y se acumula por año calendario, con fuente de verdad LOCAL de esta IPS (decisión 1). |
| RN-08 | Una exención válida tiene prioridad sobre cualquier otra regla y fija el cobro en $0, deteniendo el cálculo. |
| RN-09 | El sistema no condiciona la atención al pago en urgencias (Ley 1751/2015), ni condiciona la prestación de un nuevo servicio al pago de deudas anteriores. |
| RN-10 | Ante afiliación no verificable internamente, se aplica el cobro más conservador (particular) sin bloquear la atención, con ajuste posterior trazable. |
| RN-11 | El cajero no puede modificar el valor calculado sin autorización de un rol con permiso explícito. Toda modificación se audita. |
| RN-12 | El módulo de Recaudo solo reporta valor\_recaudado\_paciente. El neto a la EPS es responsabilidad exclusiva de Facturación. |
| RN-13 | Recaudo no aplica pagos a cartera ni gestiona glosas. Esas funciones corresponden a Cartera y a Cuentas Médicas. |
| RN-14 | El acumulado anual de copago se consulta como dato local de esta IPS. No hay integración con terceros (decisión 1). |
| RN-15 | Toda transacción de cobro (incluido $0 por exención) genera un registro trazable dentro de Recaudo con consecutivo fiscal; los exentos no se inyectan desde Admisiones. |
| RN-16 | El sistema opera sobre CIE-10 por defecto; la adopción de CIE-11 se activa globalmente vía tabla maestra transicional, no por contrato individual. |

# **8\. Contratos de integración entre módulos**

## **8.1 Consulta Externa → Recaudo (Contrato A)**

Expone lo mínimo necesario para liquidar el recaudo de consulta externa antes de la atención.

| Campo | Descripción |
| :---- | :---- |
| episodio\_id | Identificador único de la cita / episodio. |
| paciente\_id | Para cruzar con Módulo de Pacientes y obtener afiliación, rol y categoría. |
| tipo\_cita | Primera vez / control. Solo RIPS / epidemiología. Sin efecto financiero. |
| tipo\_servicio | Médico general, especialista, odontología, etc. |
| es\_PyD | Booleano obligatorio. Indica si el servicio es PyD (exento de cuota moderadora). |
| fecha\_atencion | Determina la afiliación, categoría y tablas vigentes a usar. |
| estado\_cita | Confirmada / paciente presente. Trigger del recaudo. |

| Nota. Los CUPS y el valor del servicio no son necesarios en este contrato; se registran al finalizar la consulta (Contrato C). Se elimina el campo 'exento' de auto-generación; la exención se evalúa en el Paso 3 dentro de Recaudo. |
| :---- |

## **8.2 Recaudo → Facturación (Contrato B)**

Recaudo expone únicamente datos financieros de caja. Sin datos clínicos. Sin cálculo del neto a la EPS.

| Campo | Descripción |
| :---- | :---- |
| episodio\_id | Llave de convergencia con el Contrato C. |
| valor\_recaudado\_paciente | Dinero real cobrado en ventanilla. Único campo financiero que Recaudo reporta. $0 para exentos / terceros. |
| estado\_recaudo | PENDIENTE / PARCIAL / SALDADO / NO\_APLICA / ANULADO. |
| comprobante\_id | Referencia del recibo de caja con consecutivo fiscal. Para exentos, comprobante $0 trazable emitido por Recaudo. |
| fecha\_recaudo | Fecha del cobro en ventanilla. |
| metodo\_pago | Efectivo, tarjeta, transferencia, etc. |

| Nota. valor\_neto\_eps no existe en este contrato. El neto a la EPS lo calcula Facturación cruzando el contrato tarifario con los CUPS reales de la HC (Contrato C) y restando valor\_recaudado\_paciente. |
| :---- |

## **8.3 Historia Clínica → Facturación (Contrato C)**

Expone los datos clínicos para construir RIPS y FEV. Converge con el Contrato B por episodio\_id. Su especificación detallada vive en el módulo de HC / Facturación; se referencia aquí solo por convergencia.

| Campo | Descripción |
| :---- | :---- |
| episodio\_id | Llave de convergencia con el Contrato B. |
| cups\_array | Array de CUPS de todos los procedimientos del episodio. |
| cie10\_principal | Diagnóstico principal. CIE-10 por defecto hasta cambio del MUV. |
| cie10\_relacionados | Array de CIE-10 relacionados (según normativa RIPS). |
| fecha\_atencion | Fecha real de la prestación. |
| profesional\_id | Identificador del profesional que atendió. |

## **8.4 Motor de tarifas (servicio compartido)**

El motor de tarifas se expone como servicio interno compartido, fuente única de verdad para el valor de cada CUPS. Lo consumen tanto Recaudo (para el copago del paciente) como Facturación (para el neto a la EPS), evitando duplicar la lógica tarifaria.

| Aspecto | Definición |
| :---- | :---- |
| Entrada | cups, eps\_contratante, manual\_tarifario, fecha\_atencion. |
| Salida | valor\_servicio resuelto para esa combinación y vigencia. |
| Consumidores | Recaudo (copago) y Facturación (neto EPS). |
| Regla | Si no existe tarifa para la combinación, la liquidación de copago se bloquea y se escala a Facturación. No se asume valor por defecto. |

| Fronteras. La gestión de glosas (bifurcación de saldo, respuestas a la EPS, tiempos de respuesta) y el asentamiento bancario / conciliación se retiran de este documento: pertenecen a Cuentas Médicas / Facturación y a Cartera / Tesorería, respectivamente. |
| :---- |

# **9\. Marco normativo aplicable**

| Norma | Qué regula en este módulo |
| :---- | :---- |
| Acuerdo 260 de 2004 (CNSSS) | Categorías A/B/C; régimen de cuotas moderadoras y copagos; quién paga qué (cotizante vs. beneficiario). |
| Circular Externa 048 de 2025 (Minsalud) | Indexación de cuotas moderadoras y copagos 2026 con base en UVB. Vigente desde 01-ene-2026. |
| Resolución 3488 de 2025 (Hacienda) | Valor de la UVB para 2026\. |
| Decreto 1652 de 2022 | Catálogo de exenciones de cuotas moderadoras y copagos. |
| Ley 1751 de 2015 (Estatutaria de Salud) | Prohibición de condicionar la atención (urgencias) al pago. |
| Resolución 948 de 2026 (Minsalud) | Marco vigente RIPS-FEV. Deroga 2275/2023, 558/2024 y 1884/2024. MUV \+ CUV. Referenciada por Facturación. |
| Ley 1581 de 2012 (Habeas Data) | Minimización y separación de datos clínicos / financieros en los contratos de integración. |

# **10\. Criterios de aceptación del módulo**

* El sistema calcula cuota moderadora por categoría A/B/C y UVB\_vigente, sin usar tipo\_cita.

* El sistema no cobra copago a cotizantes; solo a beneficiarios (y subsidiado según norma).

* El copago aplica tope por evento y tope anual local correctamente y muestra alerta al alcanzarlos.

* Los servicios PyD se cobran en $0 por cuota moderadora.

* El catálogo de exenciones se evalúa antes del tipo de cobro y corta el flujo en $0.

* Toda tabla paramétrica respeta vigencia temporal y se liquida por la fecha de atención.

* La máquina de estados del recaudo funciona con consecutivo fiscal, anulaciones auditadas y arqueo de caja.

* Los exentos generan transacción NO\_APLICA trazable dentro de Recaudo (no inyectada por Admisiones).

* El Contrato B expone solo datos financieros (sin clínicos, sin neto a EPS).

* El motor de tarifas responde como servicio compartido y bloquea la liquidación si no hay tarifa para la combinación.

* Las modificaciones de valor por el cajero requieren rol con permiso y quedan auditadas.

# **11\. Riesgos y dependencias**

| ID | Riesgo | Prob. | Impacto | Mitigación |
| :---- | :---- | :---- | :---- | :---- |
| R-01 | Tablas paramétricas (UVB, cuotas, topes) desactualizadas a la vigencia. | Media | Alto | Versionado con vigencia temporal \+ alerta de tabla vencida \+ responsable de actualización normativa. |
| R-02 | El motor de tarifas no resuelve la tarifa para un CUPS/EPS/vigencia. | Media | Alto | Bloqueo de liquidación de copago si la tarifa no existe, con escalamiento a Facturación. |
| R-03 | Acumulado anual de copago solo local (decisión 1\) subestima el tope si el paciente se atendió en otra IPS. | Alta | Medio | Limitación aceptada por alcance. Documentar y alertar al cajero. Reevaluar integración en versión futura. |
| R-04 | Sin verificación BDUA/ADRES (decisión 2): cobro basado en afiliación interna posiblemente desactualizada → cobro indebido o subcobro. | Media | Alto | Mantener actualizado el Módulo de Pacientes. Cobro conservador ante duda \+ ajuste posterior por auditor. Riesgo legal residual aceptado por alcance. |
| R-05 | Marcador es\_PyD o de exención ausente o mal poblado desde Consulta Externa / Pacientes. | Media | Alto | es\_PyD obligatorio en Contrato A; auditoría periódica de marcadores; cobro indebido reversable con trazabilidad. |
| R-06 | Descuadre de caja por reversos / anulaciones mal gestionados. | Baja | Alto | Máquina de estados estricta \+ soft delete \+ arqueo por turno \+ permisos. |

*Lógica de Cobro v2.1 — NexoSalud HIS · Módulo de Recaudo*
