## Descripción del proyecto:
### Esta es una aplicación que se realizó en Android Studio con videcoding lo cual trata sobre “Reporte de Presión Arterial”, lo cual permitirá a los usuarios poder registrar, almacenar, clasificar y visualizar datos de presión arterial para un seguimiento efectivo, haciendo enfoque en la toma de captura de presión sistólica, diastólica, frecuencia cardiaca, fecha, hora y condiciones de medición.
## Instrucciones de instalación:
### 1. Clonar repositorio
### 2. Abrir el proyecto en Android Studio
### 3. Configurar dependencias
### 4. Configurar credenciales o URL de n8n
### 5. Verificar la API Para uso de recomendaciones
### 6. Ejecutar la aplicación.
## Explicación de la integración con n8n:
### La aplicación Android envía cada nueva medición de presión arterial al flujo de n8n mediante un Webhook, en n8n, el flujo evalúa si los valores están dentro del rango normal o si son una emergencia hipertensiva, si se detecta una emergencia, n8n automáticamente envía una alerta por Telegram a un contacto de emergencia con los datos del paciente, la hora y una recomendación, además, registra todos los datos tanto normales como críticos en Google Sheets, para tener un respaldo clínico del historial.
## Registros y dependencias:
### Incluir su API Key y URL para que se puedan obtener las respectivas recomendaciones.