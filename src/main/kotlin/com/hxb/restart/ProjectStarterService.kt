package com.hxb.restart

import com.intellij.execution.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ModalTaskOwner.project
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.idea.maven.execution.MavenRunner
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.execution.MavenRunnerSettings
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.utils.MavenProgressIndicator
import org.jetbrains.idea.maven.utils.MavenTask
import org.jetbrains.idea.maven.utils.MavenUtil
import java.net.*
import java.util.*
import java.util.concurrent.ExecutorService


/**
 * this is a idea plugin that able to restart the spring boot project in idea
 */
@Service
class ProjectStarterService : StartService {
    private var serverSocket: ServerSocket? = null
    val logger = com.intellij.openapi.diagnostic.Logger.getInstance(ProjectStarterService::class.java)

    companion object {
        fun getInstance(): ProjectStarterService = service()
    }

    override fun start() {
        logger.info("project starter service Plugin is running.")

        Thread {

            logger.info("project starter service Plugin is running 1.")
            val udpPort = 43211
            val localAddress = InetAddress.getByName("127.0.0.1")
            val udpSocket = DatagramSocket(udpPort, localAddress)
            while (true) {
                try {
                    logger.info("project starter service Plugin is running 2.")
                    val buffer = ByteArray(1000)
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket.receive(packet)
                    logger.info("project starter service Plugin is running 3.")
                    val message = String(packet.data, 0, packet.length).trim()
                    println("Received message: $message")

                    ApplicationManager.getApplication().invokeLater {
                        restartSpringBootProject(message)
                    }

                } catch (e: Exception) {
                    logger.error("project starter service Plugin is error 4.", e)
                }
            }

        }.start()
    }

    override fun stop() {
        if (serverSocket != null) {
            try {
                serverSocket!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    /*fun getMainClass(project: Project): @NlsSafe String? {

        val psiManager = PsiManager.getInstance(project)
        val baseDir = project.baseDir
        val pomFile = baseDir.findChild("pom.xml")
        println("Received pomFile: $pomFile")
        if (pomFile != null) {
            val psiFile = psiManager.findFile(pomFile)
            if (psiFile is XmlFile) {
                println("Received pomFile: 1")
                val rootTag = psiFile.rootTag
                val buildTag = rootTag?.findFirstSubTag("build")
                val pluginsTag = buildTag?.findFirstSubTag("plugins")
                val pluginTags = pluginsTag?.findSubTags("plugin")
                println("Received pomFile size: ${pluginTags!!.size}")
                pluginTags?.forEach { pluginTag ->
                    println("Received pomFile: $pluginTag")
                    val configurationTag = pluginTag.findFirstSubTag("configuration")
                    val mainClassTag = configurationTag?.findFirstSubTag("mainClass")
                    val mainClass = mainClassTag?.value?.text
                    if (mainClass != null) {
                        return mainClass
                    }
                }
            }
        }
        return null

    }*/


    fun findConfig(openProjects: Project): RunnerAndConfigurationSettings? {
        val runManager = RunManager.getInstance(openProjects)
        val configurations = runManager.allSettings

        return configurations.filter { it ->
            //TODO 可以自行扩展自己更感兴趣的项目类型进行重启
            logger.warn("it.configuration.javaClass.name:" + it.configuration.javaClass.name)
            it.configuration.javaClass.name == "com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration"

        }.firstOrNull()
    }


    @Throws(ExecutionException::class)
    private fun restartSpringBootProject(message: String) {
        val projectName = message.split(" ")[1].trim()
        val command = message.split(" ")[0].trim()
        val openProjects = ProjectManager.getInstance().openProjects.filter { it.name == projectName }.firstOrNull()
        if (openProjects == null) {
            logger.warn("project starter service Plugin not find project return" + projectName)
            return
        }
        if ("restart" != command.trim()) {
            return
        }
        val configuration = findConfig(openProjects)
        if (configuration != null) {
            logger.info("project starter service Plugin is running cf name.," + configuration!!.name)
            val runConfiguration = configuration.configuration
            val executor = ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG)
            val prunner = ProgramRunner.getRunner(ToolWindowId.DEBUG, runConfiguration)
            if (prunner != null) {
                RunContentManager.getInstance(openProjects).allDescriptors.forEach { descriptor ->
                    if (descriptor.processHandler!!.isProcessTerminating || descriptor.processHandler!!.isProcessTerminated) {
                        return@forEach
                    }
                    descriptor.processHandler!!.destroyProcess()
                }
                logger.info("project starter service Plugin is running runner name.," + prunner!!.runnerId)

                // 定义需要执行的Maven阶段
                val goals = listOf("clean", "compile")

                // 创建并配置MavenTask

                // 创建并配置MavenTask
                val mavenTask = MavenTask { indicator ->
                    val mavenProjectsManager = MavenProjectsManager.getInstance(openProjects)
                    val runner = MavenRunner.getInstance(openProjects)

                    val parameters = MavenRunnerParameters(
                        true,
                        mavenProjectsManager.rootProjects.first().directoryFile.path,
                        null,
                        goals,
                        Collections.emptyList(),
                        Collections.emptyList()
                    )
                    val settings = MavenRunnerSettings()
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            // 执行Maven操作
                            runner.run(parameters, settings, {
                                val environment =
                                    executor?.let { ExecutionEnvironment(it, prunner, configuration, openProjects) }
                                if (environment != null) {
                                    logger.info("project starter service Plugin is running execute.,")
                                    ApplicationManager.getApplication().invokeLater {
                                        prunner.execute(environment).run { }
                                    }
                                }
                            })
                        } catch (e: Exception) {
                            logger.error("Error during Maven run", e)
                        }

                    }
                }
                val handler = MavenUtil.runInBackground(openProjects, "Executing clean compile", true, mavenTask)
                // 使用Kotlin协程进行非阻塞等待和回调
                GlobalScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.IO) {
                        handler.waitFor() // 等待任务完成，但不会阻塞主线程
                    }
                    withContext(Dispatchers.Main) {
                        println("The Maven task has completed successfully. Executing callback logic...")
                    }
                }

            }
        }
    }
}