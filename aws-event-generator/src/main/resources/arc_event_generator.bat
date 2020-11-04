:: Windows Batch File.
:: arc_device_simulator.bat - Read device event sample logs from files and
:: send them to designated syslog server.
::
:: author: Sang Hyoun Kim
:: 

@echo off
setlocal

:: Get working directory.
set CWD=%~dp0

java %JAVA_OPTS% -jar arc_event_generator.jar %*
