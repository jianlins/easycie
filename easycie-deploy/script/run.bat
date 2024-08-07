@echo off
cd /d %0\..
echo "Run pipeline id: " %1
shift

if "%4"=="" (
   set CLASSPATH=classes
   echo "Use default compiled class path: 'classes'"
   "C:\Program Files\Java\jdk-1.8\bin\java" -cp "%CLASSPATH%;easycie-deploy-2.0.1.1-jdk8.jar" edu.utah.bmi.nlp.runner.RunPipelineCommand -l edu.utah.bmi.nlp.uima.loggers.NLPDBEDWLogger %*
) else (
   set CLASSPATH=%4
   echo "Set compiled class path: %CLASSPATH%"
   "C:\Program Files\Java\jdk-1.8\bin\java" -cp "%CLASSPATH%;easycie-deploy-2.0.1.1-jdk8.jar" edu.utah.bmi.nlp.runner.RunPipelineCommand -l edu.utah.bmi.nlp.uima.loggers.NLPDBEDWLogger %1 %2 %3 %4
)
