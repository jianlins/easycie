echo off
REM Author: Molinari Davis - www.davismol.net
REM Version: 0.1
REM Date: 29/08/2014

if "%1"=="/processFile" goto processFile
SET /P commandPath=Insert the jdk folder path: 
SET commandName=\jre\bin\unpack200.exe
FORFILES /p %commandPath% /s /m *.pack /c "cmd /c call "%~f0" /processFile @path"
goto :EOF
:processFile
SET outputName=%2
SET outputName=%outputName:pack=jar%
SET fullCommand=%commandPath%%commandName% %2 %outputName%
REM echo %fullCommand%
%fullCommand%
if %ERRORLEVEL% GEQ 1 (
    echo ERROR in extraction of file: %outputName%
) else (
    echo Extracted file: %outputName%
)