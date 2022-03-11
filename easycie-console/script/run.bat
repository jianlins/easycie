@ECHO OFF
if %1.==. (
    echo use "run classname parameters" to execute classes.
) else (
    D:\u0876964\jdk-8u101-windows-x64\bin\java -cp ./classes;./preannotator.jar edu.utah.bmi.runner.%*
)

