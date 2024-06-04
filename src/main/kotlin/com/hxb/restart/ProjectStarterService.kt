package com.hxb.restart

import com.fasterxml.jackson.jr.ob.JSON
import com.intellij.execution.*
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import net.minidev.json.JSONObject
import java.io.File
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.ServerSocket
import java.util.function.Consumer

class ProjectStarterService : ApplicationComponent {
    private var serverSocket: ServerSocket? = null
    val logger = com.intellij.openapi.diagnostic.Logger.getInstance(ProjectStarterService::class.java)

    override fun initComponent() {
        logger.info("ProjectStarterService Plugin is running.")

        Thread {

            logger.info("ProjectStarterService Plugin is running 1.")
            val multicastGroupAddress = "230.0.0.0"
            val multicastPort = 43211
            val group = InetAddress.getByName(multicastGroupAddress)
            val multicastSocket = MulticastSocket(multicastPort)
            multicastSocket.joinGroup(group)
            while (true) {
                try {
                    logger.info("ProjectStarterService Plugin is running 2.")
                    val buffer = ByteArray(1000)
                    val packet = DatagramPacket(buffer, buffer.size)
                    multicastSocket.receive(packet)
                    logger.info("ProjectStarterService Plugin is running 3.")
                    val message = String(packet.data, 0, packet.length).trim()
                    println("Received message: $message")

                    ApplicationManager.getApplication().invokeLater {
                        restartSpringBootProject(message)
                    }


                } catch (e: Exception) {
                    logger.error("ProjectStarterService Plugin is error 4.", e)
                }
            }

        }.start()
    }

    override fun disposeComponent() {
        if (serverSocket != null) {
            try {
                serverSocket!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun getMainClass(project: Project): @NlsSafe String? {

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
                    if(mainClass != null){
                        return mainClass
                    }
                }
            }
        }
        return null

    }

    fun getRunConfigurationWithMainClass(project: Project,): RunConfiguration? {
        val mainClass = getMainClass(project) ?: return null
        val runManager = RunManager.getInstance(project)
        val allConfigurations = runManager.allConfigurationsList
        logger.info(mainClass)
        allConfigurations.forEach { configuration ->
            logger.info( JSON.std.asString(configuration))

        }
        return null
    }

    fun findConfig(openProjects:Project): RunnerAndConfigurationSettings? {
        val runManager = RunManager.getInstance(openProjects)
        val configurations = runManager.allSettings

       return configurations.filter { it ->
           logger.warn("it.configuration.javaClass.name:"+it.configuration.javaClass.name)
           it.configuration.javaClass.name == "com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration"

       }.firstOrNull()
    }

    @Throws(ExecutionException::class)
    private fun restartSpringBootProject(message: String) {
        val projectName = message.split(" ")[1].trim()
        val command = message.split(" ")[0].trim()
        val openProjects = ProjectManager.getInstance().openProjects.filter { it.name == projectName }.firstOrNull()
        if (openProjects == null) {
            logger.warn("ProjectStarterService Plugin not find project return" + projectName)
            return;
        }
        var appName = openProjects.name
        if ("restart" != command.trim()) {
            return
        }

        println(getMainClass(openProjects))
        val runManager = RunManager.getInstance(openProjects)

        val configuration = /* runManager.selectedConfiguration*/ findConfig(openProjects)
        if (configuration != null) {
            logger.info("ProjectStarterService Plugin is running cf name.," + configuration!!.name)
            val runConfiguration = /*configuration.configuration*/ configuration.configuration
            val executor = ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.RUN)
            val runner = ProgramRunner.getRunner(ToolWindowId.DEBUG, runConfiguration)
            if (runner != null) {
                ExecutionManager.getInstance(openProjects)
                    .getContentManager().allDescriptors.forEach(Consumer<RunContentDescriptor> { descriptor: RunContentDescriptor ->
                        if (descriptor.processHandler!!.isProcessTerminating || descriptor.processHandler!!
                                .isProcessTerminated
                        ) {
                            return@Consumer
                        }
                        descriptor.processHandler!!.destroyProcess()
                    })
                logger.info("ProjectStarterService Plugin is running runner name.," + runner!!.runnerId)
                val environment = executor?.let { ExecutionEnvironment(it, runner, configuration, openProjects) }
                if (environment != null) {
                    logger.info("ProjectStarterService Plugin is running execute.,")
                    runner.execute(environment).run { }
                }
            }
        }
    }
}