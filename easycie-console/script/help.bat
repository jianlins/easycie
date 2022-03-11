@ECHO OFF
if %1.==. (
 echo For detailed information, use help+following class names:
 echo Import
 echo Runpipe
 echo Viewer
 echo RunCPE
 echo XMISQLAgainstGoldComparater
 echo XMISQLSimpleComparator
 ) else (
java -cp ./preannotator.jar edu.utah.bmi.runner.%* help
)

