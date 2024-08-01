package edu.utah.bmi.nlp.easycie

import edu.utah.bmi.nlp.easycie.core.ConfigKeys
import edu.utah.bmi.nlp.easycie.core.SettingOper
import edu.utah.bmi.nlp.core.TasksInf
import edu.utah.bmi.nlp.easycie.tasks.Import
import edu.utah.bmi.nlp.easycie.tasks.RunCPEDescriptorTask
import edu.utah.bmi.nlp.sql.EDAO
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.*
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.test.assertFalse

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TestPipeline {

    @Test
    @Order(1)
    fun testImport() {
        val settingOper = SettingOper(easycieXML.path)
        tasks = settingOper.readSettings()
        val importer = Import(tasks, "txt")
        importer.run()

        val settingConfig = tasks.getTask("settings")
        val dbConfigFile = settingConfig.getValue(ConfigKeys.readDBConfigFileName)
        var appDir = File("./")
        if (tasks.getTask("projectDir") != null)
            appDir = File(tasks.getTask("projectDir").getValue("projectDir"))
        val dbconfig = File(appDir, dbConfigFile)
        dao = EDAO.getInstance(dbconfig, true, false)
        val numDocs = dao.queryRecord("SELECT COUNT(*) FROM SAMPLES").getValueByColumnId(1)
        assert(numDocs.equals(2))
    }

    @Test
    @Order(2)
    fun processPipeline() {
        tasks.getTask(ConfigKeys.maintask).setValue(ConfigKeys.annotator, "test_run")
        val runTask = RunCPEDescriptorTask(tasks)
        runTask.guiCall()
        val listener = runTask.runner.statCbL
        var timeout = 0
        while (listener.isProcessing && timeout < 3000) {
            Thread.sleep(5)
            timeout += 5
        }
        if (timeout >= 3000)
            assertFalse(false, "pipeline execute timeout.")
        dao.reConnect()
        val bunchConclusions = dao.queryRecord("SELECT COUNT(*) FROM RESULT_BUNCH").getValueByColumnId(1)
        System.out.println("Check number of bunch records inserted...")
        assert(bunchConclusions.equals(1))
        val docConclusions = dao.queryRecord("SELECT COUNT(*) FROM RESULT_DOC").getValueByColumnId(1)
        System.out.println("Check number of document records inserted...")
        assert(docConclusions.equals(2))
        val snippetConclusions = dao.queryRecord("SELECT COUNT(*) FROM RESULT_SNIPPET").getValueByColumnId(1)
        System.out.println("Check number of snippet records inserted...")
        assert(snippetConclusions.equals(5))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TestPipeline::class.java)
        private val projectDri = File("target/generated-test-sources/test_app")
        private val projectName = "test_project"
        private var easycieXML = File(projectDri, "conf/" + projectName + "/test_project_config.xml")
        lateinit var tasks: TasksInf
        lateinit var dao: EDAO

        @BeforeAll
        @JvmStatic
        internal fun createTempDB() {
            LOGGER.info("beforeAll called")
            if (!projectDri.exists())
                projectDri.mkdirs()
            AddNewPipeline(arrayOf("1", projectName, projectDri.path)).gen()
            FileUtils.copyDirectory(File("src/test/resources/demo_data/"), File(projectDri, "data"))
        }

        @AfterAll
        @JvmStatic
        internal fun cleanTempDB() {
            projectDri.deleteRecursively()
            LOGGER.info("afterAll called")
        }
    }
}