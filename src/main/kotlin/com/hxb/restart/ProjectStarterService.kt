package com.hxb.restart

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionManager
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManager
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowId
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
        val runManager = RunManager.getInstance(openProjects)
        val configuration = runManager.selectedConfiguration
        if (configuration != null) {
            logger.info("ProjectStarterService Plugin is running cf name.," + configuration!!.name)
            val runConfiguration = configuration.configuration
            val executor = ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.RUN)
            val runner = ProgramRunner.getRunner(ToolWindowId.DEBUG, runConfiguration)
            if (runner != null) {
                // Stop the current running configuration
                // Stop the current running configuration
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